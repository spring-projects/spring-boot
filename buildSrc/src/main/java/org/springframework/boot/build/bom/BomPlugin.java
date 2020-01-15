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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import groovy.util.Node;
import groovy.xml.QName;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlatformExtension;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.tasks.GenerateMavenPom;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskExecutionException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.springframework.boot.build.DeployedPlugin;
import org.springframework.boot.build.MavenRepositoryPlugin;
import org.springframework.boot.build.bom.Library.Group;
import org.springframework.boot.build.bom.bomr.UpgradeBom;
import org.springframework.boot.build.mavenplugin.MavenExec;
import org.springframework.util.FileCopyUtils;

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
		BomExtension bom = project.getExtensions().create("bom", BomExtension.class, project.getDependencies());
		project.getTasks().create("bomrCheck", CheckBom.class, bom);
		project.getTasks().create("bomrUpgrade", UpgradeBom.class, bom);
		new PublishingCustomizer(project, bom).customize();
		Configuration effectiveBomConfiguration = project.getConfigurations().create("effectiveBom");
		project.getTasks().matching((task) -> task.getName().equals(DeployedPlugin.GENERATE_POM_TASK_NAME))
				.all((task) -> {
					Sync syncBom = project.getTasks().create("syncBom", Sync.class);
					syncBom.dependsOn(task);
					File generatedBomDir = new File(project.getBuildDir(), "generated/bom");
					syncBom.setDestinationDir(generatedBomDir);
					syncBom.from(((GenerateMavenPom) task).getDestination(), (pom) -> pom.rename((name) -> "pom.xml"));
					try {
						String settingsXmlContent = FileCopyUtils
								.copyToString(new InputStreamReader(
										getClass().getClassLoader().getResourceAsStream("effective-bom-settings.xml"),
										StandardCharsets.UTF_8))
								.replace("localRepositoryPath",
										new File(project.getBuildDir(), "local-m2-repository").getAbsolutePath());
						syncBom.from(project.getResources().getText().fromString(settingsXmlContent),
								(settingsXml) -> settingsXml.rename((name) -> "settings.xml"));
					}
					catch (IOException ex) {
						throw new GradleException("Failed to prepare settings.xml", ex);
					}
					MavenExec generateEffectiveBom = project.getTasks().create("generateEffectiveBom", MavenExec.class);
					generateEffectiveBom.setProjectDir(generatedBomDir);
					File effectiveBom = new File(project.getBuildDir(),
							"generated/effective-bom/" + project.getName() + "-effective-bom.xml");
					generateEffectiveBom.args("--settings", "settings.xml", "help:effective-pom",
							"-Doutput=" + effectiveBom);
					generateEffectiveBom.dependsOn(syncBom);
					generateEffectiveBom.getOutputs().file(effectiveBom);
					generateEffectiveBom.doLast(new StripUnrepeatableOutputAction(effectiveBom));
					project.getArtifacts().add(effectiveBomConfiguration.getName(), effectiveBom,
							(artifact) -> artifact.builtBy(generateEffectiveBom));
				});
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
					findChild(dependency, "version")
							.setValue("${" + this.bom.getArtifactVersionProperty(groupId, artifactId) + "}");
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
						plugin.appendNode("version", "${" + library.getVersionProperty() + "}");
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
