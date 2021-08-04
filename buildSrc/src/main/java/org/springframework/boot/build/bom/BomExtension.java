/*
 * Copyright 2012-2021 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserCodeException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.publish.maven.tasks.GenerateMavenPom;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskExecutionException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.springframework.boot.build.DeployedPlugin;
import org.springframework.boot.build.bom.Library.DependencyConstraintsDependencyVersions;
import org.springframework.boot.build.bom.Library.DependencyLockDependencyVersions;
import org.springframework.boot.build.bom.Library.DependencyVersions;
import org.springframework.boot.build.bom.Library.Exclusion;
import org.springframework.boot.build.bom.Library.Group;
import org.springframework.boot.build.bom.Library.LibraryVersion;
import org.springframework.boot.build.bom.Library.Module;
import org.springframework.boot.build.bom.Library.ProhibitedVersion;
import org.springframework.boot.build.bom.Library.VersionAlignment;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;
import org.springframework.boot.build.mavenplugin.MavenExec;
import org.springframework.util.FileCopyUtils;

/**
 * DSL extensions for {@link BomPlugin}.
 *
 * @author Andy Wilkinson
 */
public class BomExtension {

	private final Map<String, DependencyVersion> properties = new LinkedHashMap<>();

	private final Map<String, String> artifactVersionProperties = new HashMap<>();

	private final List<Library> libraries = new ArrayList<>();

	private final UpgradeHandler upgradeHandler;

	private final DependencyHandler dependencyHandler;

	private final Project project;

	public BomExtension(DependencyHandler dependencyHandler, Project project) {
		this.dependencyHandler = dependencyHandler;
		this.upgradeHandler = project.getObjects().newInstance(UpgradeHandler.class);
		this.project = project;
	}

	public List<Library> getLibraries() {
		return this.libraries;
	}

	public void upgrade(Action<UpgradeHandler> action) {
		action.execute(this.upgradeHandler);
	}

	public Upgrade getUpgrade() {
		return new Upgrade(this.upgradeHandler.upgradePolicy, new GitHub(this.upgradeHandler.gitHub.organization,
				this.upgradeHandler.gitHub.repository, this.upgradeHandler.gitHub.issueLabels));
	}

	public void library(String name, Action<LibraryHandler> action) {
		this.library(name, null, action);
	}

	public void library(String name, String version, Action<LibraryHandler> action) {
		ObjectFactory objects = this.project.getObjects();
		LibraryHandler libraryHandler = objects.newInstance(LibraryHandler.class, (version != null) ? version : "",
				objects);
		action.execute(libraryHandler);
		LibraryVersion libraryVersion = new LibraryVersion(DependencyVersion.parse(libraryHandler.version),
				libraryHandler.versionAlignment);
		addLibrary(new Library(name, libraryVersion, libraryHandler.groups, libraryHandler.prohibitedVersions,
				libraryHandler.dependencyVersions));
	}

	public void effectiveBomArtifact() {
		Configuration effectiveBomConfiguration = this.project.getConfigurations().create("effectiveBom");
		this.project.getTasks().matching((task) -> task.getName().equals(DeployedPlugin.GENERATE_POM_TASK_NAME))
				.all((task) -> {
					Sync syncBom = this.project.getTasks().create("syncBom", Sync.class);
					syncBom.dependsOn(task);
					File generatedBomDir = new File(this.project.getBuildDir(), "generated/bom");
					syncBom.setDestinationDir(generatedBomDir);
					syncBom.from(((GenerateMavenPom) task).getDestination(), (pom) -> pom.rename((name) -> "pom.xml"));
					try {
						String settingsXmlContent = FileCopyUtils
								.copyToString(new InputStreamReader(
										getClass().getClassLoader().getResourceAsStream("effective-bom-settings.xml"),
										StandardCharsets.UTF_8))
								.replace("localRepositoryPath",
										new File(this.project.getBuildDir(), "local-m2-repository").getAbsolutePath());
						syncBom.from(this.project.getResources().getText().fromString(settingsXmlContent),
								(settingsXml) -> settingsXml.rename((name) -> "settings.xml"));
					}
					catch (IOException ex) {
						throw new GradleException("Failed to prepare settings.xml", ex);
					}
					MavenExec generateEffectiveBom = this.project.getTasks().create("generateEffectiveBom",
							MavenExec.class);
					generateEffectiveBom.setProjectDir(generatedBomDir);
					File effectiveBom = new File(this.project.getBuildDir(),
							"generated/effective-bom/" + this.project.getName() + "-effective-bom.xml");
					generateEffectiveBom.args("--settings", "settings.xml", "help:effective-pom",
							"-Doutput=" + effectiveBom);
					generateEffectiveBom.dependsOn(syncBom);
					generateEffectiveBom.getOutputs().file(effectiveBom);
					generateEffectiveBom.doLast(new StripUnrepeatableOutputAction(effectiveBom));
					this.project.getArtifacts().add(effectiveBomConfiguration.getName(), effectiveBom,
							(artifact) -> artifact.builtBy(generateEffectiveBom));
				});
	}

	private String createDependencyNotation(String groupId, String artifactId, DependencyVersion version) {
		return groupId + ":" + artifactId + ":" + version;
	}

	Map<String, DependencyVersion> getProperties() {
		return this.properties;
	}

	String getArtifactVersionProperty(String groupId, String artifactId) {
		String coordinates = groupId + ":" + artifactId;
		return this.artifactVersionProperties.get(coordinates);
	}

	private void putArtifactVersionProperty(String groupId, String artifactId, String versionProperty) {
		String coordinates = groupId + ":" + artifactId;
		String existing = this.artifactVersionProperties.putIfAbsent(coordinates, versionProperty);
		if (existing != null) {
			throw new InvalidUserDataException("Cannot put version property for '" + coordinates
					+ "'. Version property '" + existing + "' has already been stored.");
		}
	}

	private void addLibrary(Library library) {
		this.libraries.add(library);
		String versionProperty = library.getVersionProperty();
		if (versionProperty != null) {
			this.properties.put(versionProperty, library.getVersion().getVersion());
		}
		for (Group group : library.getGroups()) {
			for (Module module : group.getModules()) {
				putArtifactVersionProperty(group.getId(), module.getName(), versionProperty);
				this.dependencyHandler.getConstraints().add(JavaPlatformPlugin.API_CONFIGURATION_NAME,
						createDependencyNotation(group.getId(), module.getName(), library.getVersion().getVersion()));
			}
			for (String bomImport : group.getBoms()) {
				putArtifactVersionProperty(group.getId(), bomImport, versionProperty);
				String bomDependency = createDependencyNotation(group.getId(), bomImport,
						library.getVersion().getVersion());
				this.dependencyHandler.add(JavaPlatformPlugin.API_CONFIGURATION_NAME,
						this.dependencyHandler.platform(bomDependency));
				this.dependencyHandler.add(BomPlugin.API_ENFORCED_CONFIGURATION_NAME,
						this.dependencyHandler.enforcedPlatform(bomDependency));
			}
		}
	}

	public static class LibraryHandler {

		private final List<Group> groups = new ArrayList<>();

		private final List<ProhibitedVersion> prohibitedVersions = new ArrayList<>();

		private final ObjectFactory objectFactory;

		private String version;

		private VersionAlignment versionAlignment;

		private DependencyVersions dependencyVersions;

		@Inject
		public LibraryHandler(String version, ObjectFactory objectFactory) {
			this.version = version;
			this.objectFactory = objectFactory;
		}

		public void version(String version, Action<VersionHandler> action) {
			this.version = version;
			VersionHandler versionHandler = new VersionHandler();
			action.execute(versionHandler);
			this.versionAlignment = new VersionAlignment(versionHandler.libraryName);
		}

		public void group(String id, Action<GroupHandler> action) {
			GroupHandler groupHandler = new GroupHandler(id);
			action.execute(groupHandler);
			this.groups
					.add(new Group(groupHandler.id, groupHandler.modules, groupHandler.plugins, groupHandler.imports));
		}

		public void prohibit(String range, Action<ProhibitedVersionHandler> action) {
			ProhibitedVersionHandler prohibitedVersionHandler = new ProhibitedVersionHandler();
			action.execute(prohibitedVersionHandler);
			try {
				this.prohibitedVersions.add(new ProhibitedVersion(VersionRange.createFromVersionSpec(range),
						prohibitedVersionHandler.reason));
			}
			catch (InvalidVersionSpecificationException ex) {
				throw new InvalidUserCodeException("Invalid version range", ex);
			}
		}

		public void dependencyVersions(Action<DependencyVersionsHandler> action) {
			DependencyVersionsHandler dependencyVersionsHandler = this.objectFactory
					.newInstance(DependencyVersionsHandler.class, this.version);
			action.execute(dependencyVersionsHandler);
			this.dependencyVersions = dependencyVersionsHandler.dependencyVersions;
		}

		public static class VersionHandler {

			private String libraryName;

			public void shouldAlignWithVersionFrom(String libraryName) {
				this.libraryName = libraryName;
			}

		}

		public static class ProhibitedVersionHandler {

			private String reason;

			public void because(String because) {
				this.reason = because;
			}

		}

		public class GroupHandler extends GroovyObjectSupport {

			private final String id;

			private List<Module> modules = new ArrayList<>();

			private List<String> imports = new ArrayList<>();

			private List<String> plugins = new ArrayList<>();

			public GroupHandler(String id) {
				this.id = id;
			}

			public void setModules(List<Object> modules) {
				this.modules = modules.stream()
						.map((input) -> (input instanceof Module) ? (Module) input : new Module((String) input))
						.collect(Collectors.toList());
			}

			public void setImports(List<String> imports) {
				this.imports = imports;
			}

			public void setPlugins(List<String> plugins) {
				this.plugins = plugins;
			}

			public Object methodMissing(String name, Object args) {
				if (args instanceof Object[] && ((Object[]) args).length == 1) {
					Object arg = ((Object[]) args)[0];
					if (arg instanceof Closure) {
						ExclusionHandler exclusionHandler = new ExclusionHandler();
						Closure<?> closure = (Closure<?>) arg;
						closure.setResolveStrategy(Closure.DELEGATE_FIRST);
						closure.setDelegate(exclusionHandler);
						closure.call(exclusionHandler);
						return new Module(name, exclusionHandler.exclusions);
					}
				}
				throw new InvalidUserDataException("Invalid exclusion configuration for module '" + name + "'");
			}

			public class ExclusionHandler {

				private final List<Exclusion> exclusions = new ArrayList<>();

				public void exclude(Map<String, String> exclusion) {
					this.exclusions.add(new Exclusion(exclusion.get("group"), exclusion.get("module")));
				}

			}

		}

		public static class DependencyVersionsHandler {

			private final String libraryVersion;

			private DependencyVersions dependencyVersions;

			@Inject
			public DependencyVersionsHandler(String libraryVersion) {
				this.libraryVersion = libraryVersion;
			}

			public void extractFrom(Action<ExtractFromHandler> action) {
				action.execute(new ExtractFromHandler());
			}

			public class ExtractFromHandler {

				public void dependencyLock(String location) {
					DependencyVersionsHandler.this.dependencyVersions = new DependencyLockDependencyVersions(location,
							DependencyVersionsHandler.this.libraryVersion);
				}

				public void dependencyConstraints(String location) {
					DependencyVersionsHandler.this.dependencyVersions = new DependencyConstraintsDependencyVersions(
							location, DependencyVersionsHandler.this.libraryVersion);
				}

			}

		}

	}

	public static class UpgradeHandler {

		private UpgradePolicy upgradePolicy;

		private final GitHubHandler gitHub = new GitHubHandler();

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

		private String organization = "spring-projects";

		private String repository = "spring-boot";

		private List<String> issueLabels;

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

		private String organization = "spring-projects";

		private String repository = "spring-boot";

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

	private static final class StripUnrepeatableOutputAction implements Action<Task> {

		private final File effectiveBom;

		private StripUnrepeatableOutputAction(File xmlFile) {
			this.effectiveBom = xmlFile;
		}

		@Override
		public void execute(Task task) {
			try {
				Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this.effectiveBom);
				XPath xpath = XPathFactory.newInstance().newXPath();
				NodeList comments = (NodeList) xpath.evaluate("//comment()", document, XPathConstants.NODESET);
				for (int i = 0; i < comments.getLength(); i++) {
					org.w3c.dom.Node comment = comments.item(i);
					comment.getParentNode().removeChild(comment);
				}
				org.w3c.dom.Node build = (org.w3c.dom.Node) xpath.evaluate("/project/build", document,
						XPathConstants.NODE);
				build.getParentNode().removeChild(build);
				org.w3c.dom.Node reporting = (org.w3c.dom.Node) xpath.evaluate("/project/reporting", document,
						XPathConstants.NODE);
				reporting.getParentNode().removeChild(reporting);
				TransformerFactory.newInstance().newTransformer().transform(new DOMSource(document),
						new StreamResult(this.effectiveBom));
			}
			catch (Exception ex) {
				throw new TaskExecutionException(task, ex);
			}
		}

	}

}
