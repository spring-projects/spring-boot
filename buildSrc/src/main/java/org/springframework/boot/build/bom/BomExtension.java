/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.build.bom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.inject.Inject;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.gradle.api.Action;
import org.gradle.api.InvalidUserCodeException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlatformPlugin;

import org.springframework.boot.build.bom.BomExtension.LibraryHandler.AlignWithHandler.PropertyHandler;
import org.springframework.boot.build.bom.BomExtension.LibraryHandler.AlignWithHandler.VersionHandler;
import org.springframework.boot.build.bom.Library.DependencyVersionAlignment;
import org.springframework.boot.build.bom.Library.Exclusion;
import org.springframework.boot.build.bom.Library.Group;
import org.springframework.boot.build.bom.Library.ImportedBom;
import org.springframework.boot.build.bom.Library.LibraryVersion;
import org.springframework.boot.build.bom.Library.Link;
import org.springframework.boot.build.bom.Library.Module;
import org.springframework.boot.build.bom.Library.PermittedDependency;
import org.springframework.boot.build.bom.Library.PomPropertyVersionAlignment;
import org.springframework.boot.build.bom.Library.ProhibitedVersion;
import org.springframework.boot.build.bom.Library.VersionAlignment;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;
import org.springframework.boot.build.properties.BuildProperties;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;

/**
 * DSL extensions for {@link BomPlugin}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public class BomExtension {

	private final String id;

	private final Project project;

	private final UpgradeHandler upgradeHandler;

	private final Map<String, DependencyVersion> properties = new LinkedHashMap<>();

	private final Map<String, String> artifactVersionProperties = new HashMap<>();

	private final List<Library> libraries = new ArrayList<>();

	public BomExtension(Project project) {
		this.project = project;
		this.upgradeHandler = project.getObjects().newInstance(UpgradeHandler.class, project);
		this.id = "%s:%s:%s".formatted(project.getGroup(), project.getName(), project.getVersion());
	}

	public String getId() {
		return this.id;
	}

	public List<Library> getLibraries() {
		return this.libraries;
	}

	public void upgrade(Action<UpgradeHandler> action) {
		action.execute(this.upgradeHandler);
	}

	public Upgrade getUpgrade() {
		GitHubHandler gitHub = this.upgradeHandler.gitHub;
		return new Upgrade(this.upgradeHandler.upgradePolicy,
				new GitHub(gitHub.organization, gitHub.repository, gitHub.issueLabels));
	}

	public void library(String name, Action<LibraryHandler> action) {
		library(name, null, action);
	}

	public void library(String name, String version, Action<LibraryHandler> action) {
		ObjectFactory objects = this.project.getObjects();
		LibraryHandler libraryHandler = objects.newInstance(LibraryHandler.class, this.project,
				(version != null) ? version : "");
		action.execute(libraryHandler);
		LibraryVersion libraryVersion = new LibraryVersion(DependencyVersion.parse(libraryHandler.version));
		addLibrary(new Library(name, libraryHandler.calendarName, libraryVersion, libraryHandler.groups,
				libraryHandler.prohibitedVersions, libraryHandler.considerSnapshots, versionAlignment(libraryHandler),
				libraryHandler.alignWith.dependencyManagementDeclaredIn, libraryHandler.linkRootName,
				libraryHandler.links));
	}

	private VersionAlignment versionAlignment(LibraryHandler libraryHandler) {
		VersionHandler version = libraryHandler.alignWith.version;
		if (version != null) {
			return new DependencyVersionAlignment(version.of, version.from, version.managedBy, this.project,
					this.libraries, libraryHandler.groups);
		}
		PropertyHandler property = libraryHandler.alignWith.property;
		if (property != null) {
			return new PomPropertyVersionAlignment(property.name, property.of, property.managedBy, this.project,
					this.libraries);
		}
		return null;
	}

	private String createDependencyNotation(String groupId, String artifactId, DependencyVersion version) {
		return groupId + ":" + artifactId + ":" + version;
	}

	Map<String, DependencyVersion> getProperties() {
		return this.properties;
	}

	String getArtifactVersionProperty(String groupId, String artifactId, String classifier) {
		String coordinates = groupId + ":" + artifactId + ":" + classifier;
		return this.artifactVersionProperties.get(coordinates);
	}

	private void putArtifactVersionProperty(String groupId, String artifactId, String versionProperty) {
		putArtifactVersionProperty(groupId, artifactId, null, versionProperty);
	}

	private void putArtifactVersionProperty(String groupId, String artifactId, String classifier,
			String versionProperty) {
		String coordinates = groupId + ":" + artifactId + ":" + ((classifier != null) ? classifier : "");
		String existing = this.artifactVersionProperties.putIfAbsent(coordinates, versionProperty);
		if (existing != null) {
			throw new InvalidUserDataException("Cannot put version property for '" + coordinates
					+ "'. Version property '" + existing + "' has already been stored.");
		}
	}

	private void addLibrary(Library library) {
		DependencyHandler dependencies = this.project.getDependencies();
		this.libraries.add(library);
		String versionProperty = library.getVersionProperty();
		if (versionProperty != null) {
			this.properties.put(versionProperty, library.getVersion().getVersion());
		}
		for (Group group : library.getGroups()) {
			for (Module module : group.getModules()) {
				addModule(library, dependencies, versionProperty, group, module);
			}
			for (ImportedBom bomImport : group.getBoms()) {
				addBomImport(library, dependencies, versionProperty, group, bomImport.name());
			}
		}
	}

	private void addModule(Library library, DependencyHandler dependencies, String versionProperty, Group group,
			Module module) {
		putArtifactVersionProperty(group.getId(), module.getName(), module.getClassifier(), versionProperty);
		String constraint = createDependencyNotation(group.getId(), module.getName(),
				library.getVersion().getVersion());
		dependencies.getConstraints().add(JavaPlatformPlugin.API_CONFIGURATION_NAME, constraint);
	}

	private void addBomImport(Library library, DependencyHandler dependencies, String versionProperty, Group group,
			String bomImport) {
		putArtifactVersionProperty(group.getId(), bomImport, versionProperty);
		String bomDependency = createDependencyNotation(group.getId(), bomImport, library.getVersion().getVersion());
		dependencies.add(JavaPlatformPlugin.API_CONFIGURATION_NAME, dependencies.platform(bomDependency));
		dependencies.add(BomPlugin.API_ENFORCED_CONFIGURATION_NAME, dependencies.enforcedPlatform(bomDependency));
	}

	public static class LibraryHandler {

		private final Project project;

		private final List<Group> groups = new ArrayList<>();

		private final List<ProhibitedVersion> prohibitedVersions = new ArrayList<>();

		private final AlignWithHandler alignWith;

		private boolean considerSnapshots;

		private String version;

		private String calendarName;

		private String linkRootName;

		private final Map<String, List<Link>> links = new HashMap<>();

		@Inject
		public LibraryHandler(Project project, String version) {
			this.project = project;
			this.version = version;
			this.alignWith = project.getObjects().newInstance(AlignWithHandler.class);
		}

		public void version(String version) {
			this.version = version;
		}

		public void considerSnapshots() {
			this.considerSnapshots = true;
		}

		public void setCalendarName(String calendarName) {
			this.calendarName = calendarName;
		}

		public void group(String id, Action<GroupHandler> action) {
			GroupHandler groupHandler = this.project.getObjects().newInstance(GroupHandler.class, id);
			action.execute(groupHandler);
			this.groups
				.add(new Group(groupHandler.id, groupHandler.modules, groupHandler.plugins, groupHandler.imports));
		}

		public void prohibit(Action<ProhibitedHandler> action) {
			ProhibitedHandler handler = new ProhibitedHandler();
			action.execute(handler);
			this.prohibitedVersions.add(new ProhibitedVersion(handler.versionRange, handler.startsWith,
					handler.endsWith, handler.contains, handler.reason));
		}

		public void alignWith(Action<AlignWithHandler> action) {
			action.execute(this.alignWith);
		}

		public void links(Action<LinksHandler> action) {
			links(null, action);
		}

		public void links(String linkRootName, Action<LinksHandler> action) {
			LinksHandler handler = new LinksHandler();
			action.execute(handler);
			this.linkRootName = linkRootName;
			this.links.putAll(handler.links);
		}

		public static class ProhibitedHandler {

			private String reason;

			private final List<String> startsWith = new ArrayList<>();

			private final List<String> endsWith = new ArrayList<>();

			private final List<String> contains = new ArrayList<>();

			private VersionRange versionRange;

			public void versionRange(String versionRange) {
				try {
					this.versionRange = VersionRange.createFromVersionSpec(versionRange);
				}
				catch (InvalidVersionSpecificationException ex) {
					throw new InvalidUserCodeException("Invalid version range", ex);
				}
			}

			public void startsWith(String startsWith) {
				this.startsWith.add(startsWith);
			}

			public void startsWith(Collection<String> startsWith) {
				this.startsWith.addAll(startsWith);
			}

			public void endsWith(String endsWith) {
				this.endsWith.add(endsWith);
			}

			public void endsWith(Collection<String> endsWith) {
				this.endsWith.addAll(endsWith);
			}

			public void contains(String contains) {
				this.contains.add(contains);
			}

			public void contains(List<String> contains) {
				this.contains.addAll(contains);
			}

			public void because(String because) {
				this.reason = because;
			}

		}

		public static class GroupHandler extends GroovyObjectSupport {

			private final String id;

			private List<Module> modules = new ArrayList<>();

			private List<ImportedBom> imports = new ArrayList<>();

			private List<String> plugins = new ArrayList<>();

			@Inject
			public GroupHandler(String id) {
				this.id = id;
			}

			public void setModules(List<Object> modules) {
				this.modules = modules.stream()
					.map((input) -> (input instanceof Module module) ? module : new Module((String) input))
					.toList();
			}

			public void bom(String bom) {
				this.imports.add(new ImportedBom(bom));
			}

			public void bom(String bom, Action<ImportBomHandler> action) {
				ImportBomHandler handler = new ImportBomHandler();
				action.execute(handler);
				this.imports.add(new ImportedBom(bom, handler.permittedDependencies));
			}

			public void setPlugins(List<String> plugins) {
				this.plugins = plugins;
			}

			public Object methodMissing(String name, Object args) {
				if (args instanceof Object[] argsArray && argsArray.length == 1) {
					if (argsArray[0] instanceof Closure<?> closure) {
						ModuleHandler moduleHandler = new ModuleHandler();
						closure.setResolveStrategy(Closure.DELEGATE_FIRST);
						closure.setDelegate(moduleHandler);
						closure.call(moduleHandler);
						return new Module(name, moduleHandler.type, moduleHandler.classifier, moduleHandler.exclusions);
					}
				}
				throw new InvalidUserDataException("Invalid configuration for module '" + name + "'");
			}

			public class ModuleHandler {

				private final List<Exclusion> exclusions = new ArrayList<>();

				private String type;

				private String classifier;

				public void exclude(Map<String, String> exclusion) {
					this.exclusions.add(new Exclusion(exclusion.get("group"), exclusion.get("module")));
				}

				public void setType(String type) {
					this.type = type;
				}

				public void setClassifier(String classifier) {
					this.classifier = classifier;
				}

			}

			public class ImportBomHandler {

				private final List<PermittedDependency> permittedDependencies = new ArrayList<>();

				public void permit(String allowed) {
					String[] components = allowed.split(":");
					this.permittedDependencies.add(new PermittedDependency(components[0], components[1]));
				}

			}

		}

		public static class AlignWithHandler {

			private VersionHandler version;

			private PropertyHandler property;

			private String dependencyManagementDeclaredIn;

			public void version(Action<VersionHandler> action) {
				this.version = new VersionHandler();
				action.execute(this.version);
			}

			public void property(Action<PropertyHandler> action) {
				this.property = new PropertyHandler();
				action.execute(this.property);
			}

			public void dependencyManagementDeclaredIn(String bomCoordinates) {
				this.dependencyManagementDeclaredIn = bomCoordinates;
			}

			public static class VersionHandler {

				private String of;

				private String from;

				private String managedBy;

				public void of(String of) {
					this.of = of;
				}

				public void from(String from) {
					this.from = from;
				}

				public void managedBy(String managedBy) {
					this.managedBy = managedBy;
				}

			}

			public static class PropertyHandler {

				private String name;

				private String of;

				private String managedBy;

				public void name(String name) {
					this.name = name;
				}

				public void of(String dependency) {
					this.of = dependency;
				}

				public void managedBy(String managedBy) {
					this.managedBy = managedBy;
				}

			}

		}

	}

	public static class LinksHandler {

		private final Map<String, List<Link>> links = new HashMap<>();

		public void site(String linkTemplate) {
			site(asFactory(linkTemplate));
		}

		public void site(Function<LibraryVersion, String> linkFactory) {
			add("site", linkFactory);
		}

		public void github(String linkTemplate) {
			github(asFactory(linkTemplate));
		}

		public void github(Function<LibraryVersion, String> linkFactory) {
			add("github", linkFactory);
		}

		public void docs(String linkTemplate) {
			docs(asFactory(linkTemplate));
		}

		public void docs(Function<LibraryVersion, String> linkFactory) {
			add("docs", linkFactory);
		}

		public void javadoc(String linkTemplate) {
			javadoc(asFactory(linkTemplate));
		}

		public void javadoc(String linkTemplate, String... packages) {
			javadoc(asFactory(linkTemplate), packages);
		}

		public void javadoc(Function<LibraryVersion, String> linkFactory) {
			add("javadoc", linkFactory);
		}

		public void javadoc(Function<LibraryVersion, String> linkFactory, String... packages) {
			add("javadoc", linkFactory, packages);
		}

		public void javadoc(String rootName, Function<LibraryVersion, String> linkFactory, String... packages) {
			add(rootName, "javadoc", linkFactory, packages);
		}

		public void releaseNotes(String linkTemplate) {
			releaseNotes(asFactory(linkTemplate));
		}

		public void releaseNotes(Function<LibraryVersion, String> linkFactory) {
			add("releaseNotes", linkFactory);
		}

		public void add(String name, String linkTemplate) {
			add(name, asFactory(linkTemplate));
		}

		public void add(String name, Function<LibraryVersion, String> linkFactory) {
			add(name, linkFactory, null);
		}

		public void add(String name, Function<LibraryVersion, String> linkFactory, String[] packages) {
			add(null, name, linkFactory, packages);
		}

		private void add(String rootName, String name, Function<LibraryVersion, String> linkFactory,
				String[] packages) {
			Link link = new Link(rootName, linkFactory, (packages != null) ? List.of(packages) : null);
			this.links.computeIfAbsent(name, (key) -> new ArrayList<>()).add(link);
		}

		private Function<LibraryVersion, String> asFactory(String linkTemplate) {
			return (version) -> {
				PlaceholderResolver resolver = (name) -> "version".equals(name) ? version.toString() : null;
				return new PropertyPlaceholderHelper("{", "}").replacePlaceholders(linkTemplate, resolver);
			};
		}

	}

	public static class UpgradeHandler {

		private UpgradePolicy upgradePolicy;

		private final GitHubHandler gitHub;

		@Inject
		public UpgradeHandler(Project project) {
			this.gitHub = new GitHubHandler(project);
		}

		public void setPolicy(UpgradePolicy upgradePolicy) {
			this.upgradePolicy = upgradePolicy;
		}

		public void gitHub(Action<GitHubHandler> action) {
			action.execute(this.gitHub);
		}

	}

	public static final class Upgrade {

		private final UpgradePolicy upgradePolicy;

		private final GitHub gitHub;

		private Upgrade(UpgradePolicy upgradePolicy, GitHub gitHub) {
			this.upgradePolicy = upgradePolicy;
			this.gitHub = gitHub;
		}

		public UpgradePolicy getPolicy() {
			return this.upgradePolicy;
		}

		public GitHub getGitHub() {
			return this.gitHub;
		}

	}

	public static class GitHubHandler {

		private String organization;

		private String repository;

		private List<String> issueLabels;

		public GitHubHandler(Project project) {
			BuildProperties buildProperties = BuildProperties.get(project);
			this.organization = buildProperties.gitHub().organization();
			this.repository = buildProperties.gitHub().repository();
		}

		public void setOrganization(String organization) {
			this.organization = organization;
		}

		public void setRepository(String repository) {
			this.repository = repository;
		}

		public void setIssueLabels(List<String> issueLabels) {
			this.issueLabels = issueLabels;
		}

	}

	public static final class GitHub {

		private final String organization;

		private final String repository;

		private final List<String> issueLabels;

		private GitHub(String organization, String repository, List<String> issueLabels) {
			this.organization = organization;
			this.repository = repository;
			this.issueLabels = issueLabels;
		}

		public String getOrganization() {
			return this.organization;
		}

		public String getRepository() {
			return this.repository;
		}

		public List<String> getIssueLabels() {
			return this.issueLabels;
		}

	}

}
