/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

	/**
	 * Constructs a new instance of BomExtension.
	 * @param dependencyHandler The dependency handler to be used.
	 * @param project The project associated with the BomExtension.
	 */
	public BomExtension(DependencyHandler dependencyHandler, Project project) {
		this.dependencyHandler = dependencyHandler;
		this.upgradeHandler = project.getObjects().newInstance(UpgradeHandler.class);
		this.project = project;
	}

	/**
	 * Returns the list of libraries.
	 * @return the list of libraries
	 */
	public List<Library> getLibraries() {
		return this.libraries;
	}

	/**
	 * Upgrades the BomExtension by executing the specified action on the upgrade handler.
	 * @param action the action to be executed on the upgrade handler
	 */
	public void upgrade(Action<UpgradeHandler> action) {
		action.execute(this.upgradeHandler);
	}

	/**
	 * Returns the Upgrade object with the specified upgrade policy and GitHub details.
	 * @return the Upgrade object
	 */
	public Upgrade getUpgrade() {
		return new Upgrade(this.upgradeHandler.upgradePolicy, new GitHub(this.upgradeHandler.gitHub.organization,
				this.upgradeHandler.gitHub.repository, this.upgradeHandler.gitHub.issueLabels));
	}

	/**
	 * Executes the specified action on the library with the given name.
	 * @param name The name of the library.
	 * @param action The action to be performed on the library.
	 */
	public void library(String name, Action<LibraryHandler> action) {
		library(name, null, action);
	}

	/**
	 * Adds a library to the project's library collection.
	 * @param name The name of the library.
	 * @param version The version of the library.
	 * @param action The action to be performed on the library handler.
	 */
	public void library(String name, String version, Action<LibraryHandler> action) {
		ObjectFactory objects = this.project.getObjects();
		LibraryHandler libraryHandler = objects.newInstance(LibraryHandler.class, (version != null) ? version : "");
		action.execute(libraryHandler);
		LibraryVersion libraryVersion = new LibraryVersion(DependencyVersion.parse(libraryHandler.version));
		VersionAlignment versionAlignment = (libraryHandler.alignWithVersion != null)
				? new VersionAlignment(libraryHandler.alignWithVersion.from, libraryHandler.alignWithVersion.managedBy,
						this.project, this.libraries, libraryHandler.groups)
				: null;
		addLibrary(new Library(name, libraryHandler.calendarName, libraryVersion, libraryHandler.groups,
				libraryHandler.prohibitedVersions, libraryHandler.considerSnapshots, versionAlignment));
	}

	/**
	 * This method creates an effective BOM artifact. It creates a configuration called
	 * "effectiveBom" and matches the task with the name "GENERATE_POM_TASK_NAME". It then
	 * creates a Sync task called "syncBom" and sets its destination directory to
	 * "generated/bom" in the build directory. It copies the generated POM file and
	 * settings.xml file to the syncBom task. It creates a MavenExec task called
	 * "generateEffectiveBom" and sets its project directory to "generated/bom". It
	 * generates the effective BOM XML file using the Maven command "help:effective-pom"
	 * and saves it to "generated/effective-bom/{projectName}-effective-bom.xml". It adds
	 * the effective BOM artifact to the project's artifacts.
	 */
	public void effectiveBomArtifact() {
		Configuration effectiveBomConfiguration = this.project.getConfigurations().create("effectiveBom");
		this.project.getTasks()
			.matching((task) -> task.getName().equals(DeployedPlugin.GENERATE_POM_TASK_NAME))
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
				MavenExec generateEffectiveBom = this.project.getTasks()
					.create("generateEffectiveBom", MavenExec.class);
				generateEffectiveBom.setProjectDir(generatedBomDir);
				File effectiveBom = new File(this.project.getBuildDir(),
						"generated/effective-bom/" + this.project.getName() + "-effective-bom.xml");
				generateEffectiveBom.args("--settings", "settings.xml", "help:effective-pom",
						"-Doutput=" + effectiveBom);
				generateEffectiveBom.dependsOn(syncBom);
				generateEffectiveBom.getOutputs().file(effectiveBom);
				generateEffectiveBom.doLast(new StripUnrepeatableOutputAction(effectiveBom));
				this.project.getArtifacts()
					.add(effectiveBomConfiguration.getName(), effectiveBom,
							(artifact) -> artifact.builtBy(generateEffectiveBom));
			});
	}

	/**
	 * Creates a dependency notation string using the provided groupId, artifactId, and
	 * version.
	 * @param groupId the group ID of the dependency
	 * @param artifactId the artifact ID of the dependency
	 * @param version the version of the dependency
	 * @return the dependency notation string in the format "groupId:artifactId:version"
	 */
	private String createDependencyNotation(String groupId, String artifactId, DependencyVersion version) {
		return groupId + ":" + artifactId + ":" + version;
	}

	/**
	 * Returns the properties of the BomExtension.
	 * @return a map containing the properties of the BomExtension
	 */
	Map<String, DependencyVersion> getProperties() {
		return this.properties;
	}

	/**
	 * Retrieves the version property of an artifact based on its coordinates.
	 * @param groupId the group ID of the artifact
	 * @param artifactId the artifact ID
	 * @param classifier the classifier of the artifact
	 * @return the version property of the artifact, or null if not found
	 */
	String getArtifactVersionProperty(String groupId, String artifactId, String classifier) {
		String coordinates = groupId + ":" + artifactId + ":" + classifier;
		return this.artifactVersionProperties.get(coordinates);
	}

	/**
	 * Sets the version property for the specified artifact in the BOM extension.
	 * @param groupId the group ID of the artifact
	 * @param artifactId the artifact ID of the artifact
	 * @param versionProperty the version property to be set
	 */
	private void putArtifactVersionProperty(String groupId, String artifactId, String versionProperty) {
		putArtifactVersionProperty(groupId, artifactId, null, versionProperty);
	}

	/**
	 * Puts the version property for the specified artifact coordinates into the
	 * artifactVersionProperties map. The artifact coordinates consist of the groupId,
	 * artifactId, and optional classifier. If a version property already exists for the
	 * specified coordinates, an InvalidUserDataException is thrown.
	 * @param groupId The group ID of the artifact.
	 * @param artifactId The artifact ID.
	 * @param classifier The optional classifier.
	 * @param versionProperty The version property to be stored.
	 * @throws InvalidUserDataException If a version property already exists for the
	 * specified coordinates.
	 */
	private void putArtifactVersionProperty(String groupId, String artifactId, String classifier,
			String versionProperty) {
		String coordinates = groupId + ":" + artifactId + ":" + ((classifier != null) ? classifier : "");
		String existing = this.artifactVersionProperties.putIfAbsent(coordinates, versionProperty);
		if (existing != null) {
			throw new InvalidUserDataException("Cannot put version property for '" + coordinates
					+ "'. Version property '" + existing + "' has already been stored.");
		}
	}

	/**
	 * Adds a library to the list of libraries in the BomExtension.
	 * @param library the library to be added
	 */
	private void addLibrary(Library library) {
		this.libraries.add(library);
		String versionProperty = library.getVersionProperty();
		if (versionProperty != null) {
			this.properties.put(versionProperty, library.getVersion().getVersion());
		}
		for (Group group : library.getGroups()) {
			for (Module module : group.getModules()) {
				putArtifactVersionProperty(group.getId(), module.getName(), module.getClassifier(), versionProperty);
				this.dependencyHandler.getConstraints()
					.add(JavaPlatformPlugin.API_CONFIGURATION_NAME, createDependencyNotation(group.getId(),
							module.getName(), library.getVersion().getVersion()));
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

	/**
	 * LibraryHandler class.
	 */
	public static class LibraryHandler {

		private final List<Group> groups = new ArrayList<>();

		private final List<ProhibitedVersion> prohibitedVersions = new ArrayList<>();

		private boolean considerSnapshots = false;

		private String version;

		private String calendarName;

		private AlignWithVersionHandler alignWithVersion;

		/**
		 * Constructs a new LibraryHandler object with the specified version.
		 * @param version the version of the library
		 */
		@Inject
		public LibraryHandler(String version) {
			this.version = version;
		}

		/**
		 * Sets the version of the library.
		 * @param version the version to set
		 */
		public void version(String version) {
			this.version = version;
		}

		/**
		 * Enables the consideration of snapshots in the LibraryHandler.
		 */
		public void considerSnapshots() {
			this.considerSnapshots = true;
		}

		/**
		 * Sets the name of the calendar.
		 * @param calendarName the name of the calendar
		 */
		public void setCalendarName(String calendarName) {
			this.calendarName = calendarName;
		}

		/**
		 * Creates a new group with the specified ID and executes the provided action on
		 * it.
		 * @param id the ID of the group
		 * @param action the action to be executed on the group
		 */
		public void group(String id, Action<GroupHandler> action) {
			GroupHandler groupHandler = new GroupHandler(id);
			action.execute(groupHandler);
			this.groups
				.add(new Group(groupHandler.id, groupHandler.modules, groupHandler.plugins, groupHandler.imports));
		}

		/**
		 * Prohibits a specific action by adding a new prohibited version to the list of
		 * prohibited versions.
		 * @param action the action to be prohibited
		 * @throws IllegalArgumentException if the action is null
		 */
		public void prohibit(Action<ProhibitedHandler> action) {
			ProhibitedHandler handler = new ProhibitedHandler();
			action.execute(handler);
			this.prohibitedVersions.add(new ProhibitedVersion(handler.versionRange, handler.startsWith,
					handler.endsWith, handler.contains, handler.reason));
		}

		/**
		 * Aligns with a specific version by executing the provided action.
		 * @param action the action to be executed for aligning with the version
		 */
		public void alignWithVersion(Action<AlignWithVersionHandler> action) {
			this.alignWithVersion = new AlignWithVersionHandler();
			action.execute(this.alignWithVersion);
		}

		/**
		 * ProhibitedHandler class.
		 */
		public static class ProhibitedHandler {

			private String reason;

			private final List<String> startsWith = new ArrayList<>();

			private final List<String> endsWith = new ArrayList<>();

			private final List<String> contains = new ArrayList<>();

			private VersionRange versionRange;

			/**
			 * Sets the version range for the ProhibitedHandler.
			 * @param versionRange the version range to be set
			 * @throws InvalidUserCodeException if the version range is invalid
			 */
			public void versionRange(String versionRange) {
				try {
					this.versionRange = VersionRange.createFromVersionSpec(versionRange);
				}
				catch (InvalidVersionSpecificationException ex) {
					throw new InvalidUserCodeException("Invalid version range", ex);
				}
			}

			/**
			 * Adds a string to the list of strings that the ProhibitedHandler starts
			 * with.
			 * @param startsWith the string to be added
			 */
			public void startsWith(String startsWith) {
				this.startsWith.add(startsWith);
			}

			/**
			 * Adds the specified collection of strings to the existing collection of
			 * strings that represent the starting characters to be checked.
			 * @param startsWith the collection of strings to be added
			 */
			public void startsWith(Collection<String> startsWith) {
				this.startsWith.addAll(startsWith);
			}

			/**
			 * Adds a string to the list of strings to check if it ends with.
			 * @param endsWith the string to add to the list
			 */
			public void endsWith(String endsWith) {
				this.endsWith.add(endsWith);
			}

			/**
			 * Adds the specified collection of strings to the list of endings to check
			 * for.
			 * @param endsWith the collection of strings to add
			 */
			public void endsWith(Collection<String> endsWith) {
				this.endsWith.addAll(endsWith);
			}

			/**
			 * Adds a string to the list of prohibited words.
			 * @param contains the string to be added
			 */
			public void contains(String contains) {
				this.contains.add(contains);
			}

			/**
			 * Adds the specified list of strings to the existing list of contains.
			 * @param contains the list of strings to be added
			 */
			public void contains(List<String> contains) {
				this.contains.addAll(contains);
			}

			/**
			 * Sets the reason for prohibition.
			 * @param because the reason for prohibition
			 */
			public void because(String because) {
				this.reason = because;
			}

		}

		/**
		 * GroupHandler class.
		 */
		public class GroupHandler extends GroovyObjectSupport {

			private final String id;

			private List<Module> modules = new ArrayList<>();

			private List<String> imports = new ArrayList<>();

			private List<String> plugins = new ArrayList<>();

			/**
			 * Constructs a new GroupHandler object with the specified id.
			 * @param id the id of the group
			 */
			public GroupHandler(String id) {
				this.id = id;
			}

			/**
			 * Sets the modules for the GroupHandler.
			 * @param modules the list of modules to be set
			 * @throws IllegalArgumentException if any of the modules are not of type
			 * Module or String
			 */
			public void setModules(List<Object> modules) {
				this.modules = modules.stream()
					.map((input) -> (input instanceof Module module) ? module : new Module((String) input))
					.toList();
			}

			/**
			 * Sets the list of imports for the GroupHandler class.
			 * @param imports the list of imports to be set
			 */
			public void setImports(List<String> imports) {
				this.imports = imports;
			}

			/**
			 * Sets the list of plugins for the GroupHandler.
			 * @param plugins the list of plugins to be set
			 */
			public void setPlugins(List<String> plugins) {
				this.plugins = plugins;
			}

			/**
			 * Handles the missing method for the GroupHandler class.
			 * @param name The name of the missing method.
			 * @param args The arguments passed to the missing method.
			 * @return The created Module object based on the provided arguments.
			 * @throws InvalidUserDataException If the configuration for the module is
			 * invalid.
			 */
			public Object methodMissing(String name, Object args) {
				if (args instanceof Object[] && ((Object[]) args).length == 1) {
					Object arg = ((Object[]) args)[0];
					if (arg instanceof Closure<?> closure) {
						ModuleHandler moduleHandler = new ModuleHandler();
						closure.setResolveStrategy(Closure.DELEGATE_FIRST);
						closure.setDelegate(moduleHandler);
						closure.call(moduleHandler);
						return new Module(name, moduleHandler.type, moduleHandler.classifier, moduleHandler.exclusions);
					}
				}
				throw new InvalidUserDataException("Invalid configuration for module '" + name + "'");
			}

			/**
			 * ModuleHandler class.
			 */
			public class ModuleHandler {

				private final List<Exclusion> exclusions = new ArrayList<>();

				private String type;

				private String classifier;

				/**
				 * Adds an exclusion to the list of exclusions.
				 * @param exclusion a Map containing the group and module to be excluded
				 */
				public void exclude(Map<String, String> exclusion) {
					this.exclusions.add(new Exclusion(exclusion.get("group"), exclusion.get("module")));
				}

				/**
				 * Sets the type of the module.
				 * @param type the type of the module
				 */
				public void setType(String type) {
					this.type = type;
				}

				/**
				 * Sets the classifier for the ModuleHandler.
				 * @param classifier the classifier to be set
				 */
				public void setClassifier(String classifier) {
					this.classifier = classifier;
				}

			}

		}

		/**
		 * AlignWithVersionHandler class.
		 */
		public static class AlignWithVersionHandler {

			private String from;

			private String managedBy;

			/**
			 * Sets the "from" value for the AlignWithVersionHandler.
			 * @param from the value to set as the "from" value
			 */
			public void from(String from) {
				this.from = from;
			}

			/**
			 * Sets the manager of the AlignWithVersionHandler.
			 * @param managedBy the name of the manager
			 */
			public void managedBy(String managedBy) {
				this.managedBy = managedBy;
			}

		}

	}

	/**
	 * UpgradeHandler class.
	 */
	public static class UpgradeHandler {

		private UpgradePolicy upgradePolicy;

		private final GitHubHandler gitHub = new GitHubHandler();

		/**
		 * Sets the upgrade policy for the UpgradeHandler.
		 * @param upgradePolicy the upgrade policy to be set
		 */
		public void setPolicy(UpgradePolicy upgradePolicy) {
			this.upgradePolicy = upgradePolicy;
		}

		/**
		 * Executes the specified action on the GitHubHandler instance.
		 * @param action the action to be executed on the GitHubHandler
		 */
		public void gitHub(Action<GitHubHandler> action) {
			action.execute(this.gitHub);
		}

	}

	/**
	 * Upgrade class.
	 */
	public static final class Upgrade {

		private final UpgradePolicy upgradePolicy;

		private final GitHub gitHub;

		/**
		 * Constructs a new Upgrade object with the specified upgrade policy and GitHub
		 * instance.
		 * @param upgradePolicy the upgrade policy to be used
		 * @param gitHub the GitHub instance to be used
		 */
		private Upgrade(UpgradePolicy upgradePolicy, GitHub gitHub) {
			this.upgradePolicy = upgradePolicy;
			this.gitHub = gitHub;
		}

		/**
		 * Returns the upgrade policy of the Upgrade object.
		 * @return the upgrade policy
		 */
		public UpgradePolicy getPolicy() {
			return this.upgradePolicy;
		}

		/**
		 * Returns the GitHub object associated with this Upgrade instance.
		 * @return the GitHub object
		 */
		public GitHub getGitHub() {
			return this.gitHub;
		}

	}

	/**
	 * GitHubHandler class.
	 */
	public static class GitHubHandler {

		private String organization = "spring-projects";

		private String repository = "spring-boot";

		private List<String> issueLabels;

		/**
		 * Sets the organization for the GitHubHandler.
		 * @param organization the organization to be set
		 */
		public void setOrganization(String organization) {
			this.organization = organization;
		}

		/**
		 * Sets the repository for the GitHubHandler.
		 * @param repository the repository to be set
		 */
		public void setRepository(String repository) {
			this.repository = repository;
		}

		/**
		 * Sets the issue labels for the GitHubHandler.
		 * @param issueLabels the list of labels to be set
		 */
		public void setIssueLabels(List<String> issueLabels) {
			this.issueLabels = issueLabels;
		}

	}

	/**
	 * GitHub class.
	 */
	public static final class GitHub {

		private String organization = "spring-projects";

		private String repository = "spring-boot";

		private final List<String> issueLabels;

		/**
		 * Constructs a new GitHub object with the specified organization, repository, and
		 * issue labels.
		 * @param organization the organization name associated with the GitHub repository
		 * @param repository the repository name on GitHub
		 * @param issueLabels a list of labels associated with the issues in the
		 * repository
		 */
		private GitHub(String organization, String repository, List<String> issueLabels) {
			this.organization = organization;
			this.repository = repository;
			this.issueLabels = issueLabels;
		}

		/**
		 * Returns the organization of the GitHub class.
		 * @return the organization of the GitHub class
		 */
		public String getOrganization() {
			return this.organization;
		}

		/**
		 * Returns the repository associated with this GitHub object.
		 * @return the repository associated with this GitHub object
		 */
		public String getRepository() {
			return this.repository;
		}

		/**
		 * Returns the list of labels associated with the issue.
		 * @return the list of labels associated with the issue
		 */
		public List<String> getIssueLabels() {
			return this.issueLabels;
		}

	}

	/**
	 * StripUnrepeatableOutputAction class.
	 */
	private static final class StripUnrepeatableOutputAction implements Action<Task> {

		private final File effectiveBom;

		/**
		 * Constructs a new StripUnrepeatableOutputAction object with the specified XML
		 * file.
		 * @param xmlFile the XML file to be used as the effective BOM
		 */
		private StripUnrepeatableOutputAction(File xmlFile) {
			this.effectiveBom = xmlFile;
		}

		/**
		 * Executes the given task by stripping unrepeatable output from the effective
		 * BOM.
		 * @param task the task to be executed
		 * @throws TaskExecutionException if an error occurs during task execution
		 */
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
				TransformerFactory.newInstance()
					.newTransformer()
					.transform(new DOMSource(document), new StreamResult(this.effectiveBom));
			}
			catch (Exception ex) {
				throw new TaskExecutionException(task, ex);
			}
		}

	}

}
