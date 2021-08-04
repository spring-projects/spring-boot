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

import java.util.List;
import java.util.stream.Collectors;

import groovy.util.Node;
import groovy.xml.QName;
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

	@Override
	public void apply(Project project) {
		PluginContainer plugins = project.getPlugins();
		plugins.apply(DeployedPlugin.class);
		plugins.apply(MavenRepositoryPlugin.class);
		plugins.apply(JavaPlatformPlugin.class);
		JavaPlatformExtension javaPlatform = project.getExtensions().getByType(JavaPlatformExtension.class);
		javaPlatform.allowDependencies();
		createApiEnforcedConfiguration(project);
		BomExtension bom = project.getExtensions().create("bom", BomExtension.class, project.getDependencies(),
				project);
		project.getTasks().create("bomrCheck", CheckBom.class, bom);
		project.getTasks().create("bomrUpgrade", UpgradeBom.class, bom);
		new PublishingCustomizer(project, bom).customize();

	}

	private void createApiEnforcedConfiguration(Project project) {
		Configuration apiEnforced = project.getConfigurations().create(API_ENFORCED_CONFIGURATION_NAME,
				(configuration) -> {
					configuration.setCanBeConsumed(false);
					configuration.setCanBeResolved(false);
					configuration.setVisible(false);
				});
		project.getConfigurations().getByName(JavaPlatformPlugin.ENFORCED_API_ELEMENTS_CONFIGURATION_NAME)
				.extendsFrom(apiEnforced);
		project.getConfigurations().getByName(JavaPlatformPlugin.ENFORCED_RUNTIME_ELEMENTS_CONFIGURATION_NAME)
				.extendsFrom(apiEnforced);
	}

	private static final class PublishingCustomizer {

		private final Project project;

		private final BomExtension bom;

		private PublishingCustomizer(Project project, BomExtension bom) {
			this.project = project;
			this.bom = bom;
		}

		private void customize() {
			PublishingExtension publishing = this.project.getExtensions().getByType(PublishingExtension.class);
			publishing.getPublications().withType(MavenPublication.class).all(this::configurePublication);
		}

		private void configurePublication(MavenPublication publication) {
			publication.pom(this::customizePom);
		}

		@SuppressWarnings("unchecked")
		private void customizePom(MavenPom pom) {
			pom.withXml((xml) -> {
				Node projectNode = xml.asNode();
				Node properties = new Node(null, "properties");
				this.bom.getProperties().forEach(properties::appendNode);
				Node dependencyManagement = findChild(projectNode, "dependencyManagement");
				if (dependencyManagement != null) {
					addPropertiesBeforeDependencyManagement(projectNode, properties);
					replaceVersionsWithVersionPropertyReferences(dependencyManagement);
					addExclusionsToManagedDependencies(dependencyManagement);
				}
				else {
					projectNode.children().add(properties);
				}
				addPluginManagement(projectNode);
			});
		}

		@SuppressWarnings("unchecked")
		private void addPropertiesBeforeDependencyManagement(Node projectNode, Node properties) {
			for (int i = 0; i < projectNode.children().size(); i++) {
				if (isNodeWithName(projectNode.children().get(i), "dependencyManagement")) {
					projectNode.children().add(i, properties);
					break;
				}
			}
		}

		private void replaceVersionsWithVersionPropertyReferences(Node dependencyManagement) {
			Node dependencies = findChild(dependencyManagement, "dependencies");
			if (dependencies != null) {
				for (Node dependency : findChildren(dependencies, "dependency")) {
					String groupId = findChild(dependency, "groupId").text();
					String artifactId = findChild(dependency, "artifactId").text();
					String versionProperty = this.bom.getArtifactVersionProperty(groupId, artifactId);
					if (versionProperty != null) {
						findChild(dependency, "version").setValue("${" + versionProperty + "}");
					}
				}
			}
		}

		private void addExclusionsToManagedDependencies(Node dependencyManagement) {
			Node dependencies = findChild(dependencyManagement, "dependencies");
			if (dependencies != null) {
				for (Node dependency : findChildren(dependencies, "dependency")) {
					String groupId = findChild(dependency, "groupId").text();
					String artifactId = findChild(dependency, "artifactId").text();
					this.bom.getLibraries().stream().flatMap((library) -> library.getGroups().stream())
							.filter((group) -> group.getId().equals(groupId))
							.flatMap((group) -> group.getModules().stream())
							.filter((module) -> module.getName().equals(artifactId))
							.flatMap((module) -> module.getExclusions().stream()).forEach((exclusion) -> {
								Node exclusions = findOrCreateNode(dependency, "exclusions");
								Node node = new Node(exclusions, "exclusion");
								node.appendNode("groupId", exclusion.getGroupId());
								node.appendNode("artifactId", exclusion.getArtifactId());
							});
				}
			}
		}

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

		private Node findChild(Node parent, String name) {
			for (Object child : parent.children()) {
				if (child instanceof Node) {
					Node node = (Node) child;
					if ((node.name() instanceof QName) && name.equals(((QName) node.name()).getLocalPart())) {
						return node;
					}
					if (name.equals(node.name())) {
						return node;
					}
				}
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		private List<Node> findChildren(Node parent, String name) {
			return (List<Node>) parent.children().stream().filter((child) -> isNodeWithName(child, name))
					.collect(Collectors.toList());

		}

		private boolean isNodeWithName(Object candidate, String name) {
			if (candidate instanceof Node) {
				Node node = (Node) candidate;
				if ((node.name() instanceof QName) && name.equals(((QName) node.name()).getLocalPart())) {
					return true;
				}
				if (name.equals(node.name())) {
					return true;
				}
			}
			return false;
		}

	}

}
