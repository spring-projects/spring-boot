/*
 * Copyright 2012-2020 the original author or authors.
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.gradle.api.InvalidUserCodeException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.util.ConfigureUtil;

import org.springframework.boot.build.bom.Library.Exclusion;
import org.springframework.boot.build.bom.Library.Group;
import org.springframework.boot.build.bom.Library.Module;
import org.springframework.boot.build.bom.Library.ProhibitedVersion;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;

/**
 * DSL extensions for {@link BomPlugin}.
 *
 * @author Andy Wilkinson
 */
public class BomExtension {

	private final Map<String, DependencyVersion> properties = new LinkedHashMap<>();

	private final Map<String, String> artifactVersionProperties = new HashMap<>();

	private final List<Library> libraries = new ArrayList<>();

	private final UpgradeHandler upgradeHandler = new UpgradeHandler();

	private final DependencyHandler dependencyHandler;

	public BomExtension(DependencyHandler dependencyHandler) {
		this.dependencyHandler = dependencyHandler;
	}

	public List<Library> getLibraries() {
		return this.libraries;
	}

	public void upgrade(Closure<?> closure) {
		ConfigureUtil.configure(closure, this.upgradeHandler);
	}

	public Upgrade getUpgrade() {
		return new Upgrade(this.upgradeHandler.upgradePolicy, new GitHub(this.upgradeHandler.gitHub.organization,
				this.upgradeHandler.gitHub.repository, this.upgradeHandler.gitHub.issueLabels));
	}

	public void library(String name, String version, Closure<?> closure) {
		LibraryHandler libraryHandler = new LibraryHandler();
		ConfigureUtil.configure(closure, libraryHandler);
		addLibrary(new Library(name, DependencyVersion.parse(version), libraryHandler.groups,
				libraryHandler.prohibitedVersions));
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
		this.properties.put(library.getVersionProperty(), library.getVersion());
		for (Group group : library.getGroups()) {
			for (Module module : group.getModules()) {
				putArtifactVersionProperty(group.getId(), module.getName(), library.getVersionProperty());
				this.dependencyHandler.getConstraints().add(JavaPlatformPlugin.API_CONFIGURATION_NAME,
						createDependencyNotation(group.getId(), module.getName(), library.getVersion()));
			}
			for (String bomImport : group.getBoms()) {
				putArtifactVersionProperty(group.getId(), bomImport, library.getVersionProperty());
				String bomDependency = createDependencyNotation(group.getId(), bomImport, library.getVersion());
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

		public void group(String id, Closure<?> closure) {
			GroupHandler groupHandler = new GroupHandler(id);
			ConfigureUtil.configure(closure, groupHandler);
			this.groups
					.add(new Group(groupHandler.id, groupHandler.modules, groupHandler.plugins, groupHandler.imports));
		}

		public void prohibit(String range, Closure<?> closure) {
			ProhibitedVersionHandler prohibitedVersionHandler = new ProhibitedVersionHandler();
			ConfigureUtil.configure(closure, prohibitedVersionHandler);
			try {
				this.prohibitedVersions.add(new ProhibitedVersion(VersionRange.createFromVersionSpec(range),
						prohibitedVersionHandler.reason));
			}
			catch (InvalidVersionSpecificationException ex) {
				throw new InvalidUserCodeException("Invalid version range", ex);
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
						ConfigureUtil.configure((Closure<?>) arg, exclusionHandler);
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

	}

	public static class UpgradeHandler {

		private UpgradePolicy upgradePolicy;

		private final GitHubHandler gitHub = new GitHubHandler();

		public void setPolicy(UpgradePolicy upgradePolicy) {
			this.upgradePolicy = upgradePolicy;
		}

		public void gitHub(Closure<?> closure) {
			ConfigureUtil.configure(closure, this.gitHub);
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

		private List<String> issueLabels;

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
