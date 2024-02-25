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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import groovy.namespace.QName;
import groovy.util.Node;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlatformExtension;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPublication;

import org.springframework.boot.build.DeployedPlugin;
import org.springframework.boot.build.MavenRepositoryPlugin;
import org.springframework.boot.build.bom.Library.Group;
import org.springframework.boot.build.bom.Library.Module;
import org.springframework.boot.build.bom.bomr.MoveToSnapshots;
import org.springframework.boot.build.bom.bomr.UpgradeBom;

/**
 * {@link Plugin} for defining a bom. Dependencies are added as constraints in the
 * {@code api} configuration. Imported boms are added as enforced platforms in the
 * {@code api} configuration.
 *
 * @author Andy Wilkinson
 */
public class BomPlugin implements Plugin<Project> {

	static final String API_ENFORCED_CONFIGURATION_NAME = "apiEnforced";

	/**
     * Applies the necessary plugins and configurations to the given project for the BomPlugin.
     * 
     * @param project The project to apply the plugins and configurations to.
     */
    @Override
	public void apply(Project project) {
		PluginContainer plugins = project.getPlugins();
		plugins.apply(DeployedPlugin.class);
		plugins.apply(MavenRepositoryPlugin.class);
		plugins.apply(JavaPlatformPlugin.class);
		JavaPlatformExtension javaPlatform = project.getExtensions().getByType(JavaPlatformExtension.class);
		javaPlatform.allowDependencies();
		createApiEnforcedConfiguration(project);
		BomExtension bom = project.getExtensions()
			.create("bom", BomExtension.class, project.getDependencies(), project);
		CheckBom checkBom = project.getTasks().create("bomrCheck", CheckBom.class, bom);
		project.getTasks().named("check").configure((check) -> check.dependsOn(checkBom));
		project.getTasks().create("bomrUpgrade", UpgradeBom.class, bom);
		project.getTasks().create("moveToSnapshots", MoveToSnapshots.class, bom);
		new PublishingCustomizer(project, bom).customize();

	}

	/**
     * Creates an API enforced configuration for the given project.
     * 
     * @param project the project for which the API enforced configuration is created
     */
    private void createApiEnforcedConfiguration(Project project) {
		Configuration apiEnforced = project.getConfigurations()
			.create(API_ENFORCED_CONFIGURATION_NAME, (configuration) -> {
				configuration.setCanBeConsumed(false);
				configuration.setCanBeResolved(false);
				configuration.setVisible(false);
			});
		project.getConfigurations()
			.getByName(JavaPlatformPlugin.ENFORCED_API_ELEMENTS_CONFIGURATION_NAME)
			.extendsFrom(apiEnforced);
		project.getConfigurations()
			.getByName(JavaPlatformPlugin.ENFORCED_RUNTIME_ELEMENTS_CONFIGURATION_NAME)
			.extendsFrom(apiEnforced);
	}

	/**
     * PublishingCustomizer class.
     */
    private static final class PublishingCustomizer {

		private final Project project;

		private final BomExtension bom;

		/**
         * Constructs a new instance of the PublishingCustomizer class with the specified project and bom.
         * 
         * @param project the project to be associated with the PublishingCustomizer
         * @param bom the BomExtension to be associated with the PublishingCustomizer
         */
        private PublishingCustomizer(Project project, BomExtension bom) {
			this.project = project;
			this.bom = bom;
		}

		/**
         * Customizes the publishing configuration for the project.
         * This method retrieves the PublishingExtension from the project's extensions and configures all MavenPublication publications.
         * The configuration is done by invoking the configurePublication method for each MavenPublication.
         */
        private void customize() {
			PublishingExtension publishing = this.project.getExtensions().getByType(PublishingExtension.class);
			publishing.getPublications().withType(MavenPublication.class).all(this::configurePublication);
		}

		/**
         * Configures the Maven publication by customizing the POM.
         * 
         * @param publication the Maven publication to be configured
         */
        private void configurePublication(MavenPublication publication) {
			publication.pom(this::customizePom);
		}

		/**
         * Customizes the POM file by adding properties, managing dependencies, replacing versions with version property references,
         * adding exclusions and types to managed dependencies, and adding plugin management.
         * 
         * @param pom the MavenPom object representing the POM file to be customized
         */
        @SuppressWarnings("unchecked")
		private void customizePom(MavenPom pom) {
			pom.withXml((xml) -> {
				Node projectNode = xml.asNode();
				Node properties = new Node(null, "properties");
				this.bom.getProperties().forEach(properties::appendNode);
				Node dependencyManagement = findChild(projectNode, "dependencyManagement");
				if (dependencyManagement != null) {
					addPropertiesBeforeDependencyManagement(projectNode, properties);
					addClassifiedManagedDependencies(dependencyManagement);
					replaceVersionsWithVersionPropertyReferences(dependencyManagement);
					addExclusionsToManagedDependencies(dependencyManagement);
					addTypesToManagedDependencies(dependencyManagement);
				}
				else {
					projectNode.children().add(properties);
				}
				addPluginManagement(projectNode);
			});
		}

		/**
         * Adds the properties node before the dependencyManagement node in the projectNode.
         * 
         * @param projectNode the project node to add the properties node to
         * @param properties the properties node to be added
         */
        @SuppressWarnings("unchecked")
		private void addPropertiesBeforeDependencyManagement(Node projectNode, Node properties) {
			for (int i = 0; i < projectNode.children().size(); i++) {
				if (isNodeWithName(projectNode.children().get(i), "dependencyManagement")) {
					projectNode.children().add(i, properties);
					break;
				}
			}
		}

		/**
         * Replaces the versions of dependencies with version property references.
         * 
         * @param dependencyManagement the Node representing the dependency management section
         */
        private void replaceVersionsWithVersionPropertyReferences(Node dependencyManagement) {
			Node dependencies = findChild(dependencyManagement, "dependencies");
			if (dependencies != null) {
				for (Node dependency : findChildren(dependencies, "dependency")) {
					String groupId = findChild(dependency, "groupId").text();
					String artifactId = findChild(dependency, "artifactId").text();
					Node classifierNode = findChild(dependency, "classifier");
					String classifier = (classifierNode != null) ? classifierNode.text() : "";
					String versionProperty = this.bom.getArtifactVersionProperty(groupId, artifactId, classifier);
					if (versionProperty != null) {
						findChild(dependency, "version").setValue("${" + versionProperty + "}");
					}
				}
			}
		}

		/**
         * Adds exclusions to managed dependencies.
         * 
         * @param dependencyManagement The dependency management node.
         */
        private void addExclusionsToManagedDependencies(Node dependencyManagement) {
			Node dependencies = findChild(dependencyManagement, "dependencies");
			if (dependencies != null) {
				for (Node dependency : findChildren(dependencies, "dependency")) {
					String groupId = findChild(dependency, "groupId").text();
					String artifactId = findChild(dependency, "artifactId").text();
					this.bom.getLibraries()
						.stream()
						.flatMap((library) -> library.getGroups().stream())
						.filter((group) -> group.getId().equals(groupId))
						.flatMap((group) -> group.getModules().stream())
						.filter((module) -> module.getName().equals(artifactId))
						.flatMap((module) -> module.getExclusions().stream())
						.forEach((exclusion) -> {
							Node exclusions = findOrCreateNode(dependency, "exclusions");
							Node node = new Node(exclusions, "exclusion");
							node.appendNode("groupId", exclusion.getGroupId());
							node.appendNode("artifactId", exclusion.getArtifactId());
						});
				}
			}
		}

		/**
         * Adds types to managed dependencies.
         * 
         * @param dependencyManagement The dependency management node.
         * @throws IllegalStateException if multiple types are found for a dependency.
         */
        private void addTypesToManagedDependencies(Node dependencyManagement) {
			Node dependencies = findChild(dependencyManagement, "dependencies");
			if (dependencies != null) {
				for (Node dependency : findChildren(dependencies, "dependency")) {
					String groupId = findChild(dependency, "groupId").text();
					String artifactId = findChild(dependency, "artifactId").text();
					Set<String> types = this.bom.getLibraries()
						.stream()
						.flatMap((library) -> library.getGroups().stream())
						.filter((group) -> group.getId().equals(groupId))
						.flatMap((group) -> group.getModules().stream())
						.filter((module) -> module.getName().equals(artifactId))
						.map(Module::getType)
						.filter(Objects::nonNull)
						.collect(Collectors.toSet());
					if (types.size() > 1) {
						throw new IllegalStateException(
								"Multiple types for " + groupId + ":" + artifactId + ": " + types);
					}
					if (types.size() == 1) {
						String type = types.iterator().next();
						dependency.appendNode("type", type);
					}
				}
			}
		}

		/**
         * Adds classified managed dependencies to the given dependency management node.
         * 
         * @param dependencyManagement The dependency management node to add the classified dependencies to.
         */
        @SuppressWarnings("unchecked")
		private void addClassifiedManagedDependencies(Node dependencyManagement) {
			Node dependencies = findChild(dependencyManagement, "dependencies");
			if (dependencies != null) {
				for (Node dependency : findChildren(dependencies, "dependency")) {
					String groupId = findChild(dependency, "groupId").text();
					String artifactId = findChild(dependency, "artifactId").text();
					String version = findChild(dependency, "version").text();
					Set<String> classifiers = this.bom.getLibraries()
						.stream()
						.flatMap((library) -> library.getGroups().stream())
						.filter((group) -> group.getId().equals(groupId))
						.flatMap((group) -> group.getModules().stream())
						.filter((module) -> module.getName().equals(artifactId))
						.map(Module::getClassifier)
						.filter(Objects::nonNull)
						.collect(Collectors.toSet());
					Node target = dependency;
					for (String classifier : classifiers) {
						if (!classifier.isEmpty()) {
							if (target == null) {
								target = new Node(null, "dependency");
								target.appendNode("groupId", groupId);
								target.appendNode("artifactId", artifactId);
								target.appendNode("version", version);
								int index = dependency.parent().children().indexOf(dependency);
								dependency.parent().children().add(index + 1, target);
							}
							target.appendNode("classifier", classifier);
						}
						target = null;
					}
				}
			}
		}

		/**
         * Adds plugin management to the specified project node.
         * 
         * @param projectNode the project node to add plugin management to
         */
        private void addPluginManagement(Node projectNode) {
			for (Library library : this.bom.getLibraries()) {
				for (Group group : library.getGroups()) {
					Node plugins = findOrCreateNode(projectNode, "build", "pluginManagement", "plugins");
					for (String pluginName : group.getPlugins()) {
						Node plugin = new Node(plugins, "plugin");
						plugin.appendNode("groupId", group.getId());
						plugin.appendNode("artifactId", pluginName);
						String versionProperty = library.getVersionProperty();
						String value = (versionProperty != null) ? "${" + versionProperty + "}"
								: library.getVersion().getVersion().toString();
						plugin.appendNode("version", value);
					}
				}
			}
		}

		/**
         * Finds or creates a node in the tree structure based on the given path.
         * 
         * @param parent The parent node in the tree structure.
         * @param path   The path to the desired node, represented as an array of strings.
         * @return The found or created node.
         */
        private Node findOrCreateNode(Node parent, String... path) {
			Node current = parent;
			for (String nodeName : path) {
				Node child = findChild(current, nodeName);
				if (child == null) {
					child = new Node(current, nodeName);
				}
				current = child;
			}
			return current;
		}

		/**
         * Finds a child node with the given name in the parent node.
         * 
         * @param parent the parent node to search in
         * @param name   the name of the child node to find
         * @return the found child node, or null if not found
         */
        private Node findChild(Node parent, String name) {
			for (Object child : parent.children()) {
				if (child instanceof Node node) {
					if ((node.name() instanceof QName qname) && name.equals(qname.getLocalPart())) {
						return node;
					}
					if (name.equals(node.name())) {
						return node;
					}
				}
			}
			return null;
		}

		/**
         * Finds the children of a given parent node with a specific name.
         * 
         * @param parent the parent node to search for children
         * @param name the name of the children nodes to find
         * @return a list of child nodes with the specified name
         */
        @SuppressWarnings("unchecked")
		private List<Node> findChildren(Node parent, String name) {
			return parent.children().stream().filter((child) -> isNodeWithName(child, name)).toList();
		}

		/**
         * Checks if the given candidate object is a Node with the specified name.
         * 
         * @param candidate the object to be checked
         * @param name the name to be compared with the Node's name
         * @return true if the candidate is a Node with the specified name, false otherwise
         */
        private boolean isNodeWithName(Object candidate, String name) {
			if (candidate instanceof Node node) {
				if ((node.name() instanceof QName qname) && name.equals(qname.getLocalPart())) {
					return true;
				}
				return name.equals(node.name());
			}
			return false;
		}

	}

}
