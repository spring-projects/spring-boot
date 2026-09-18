/*
 * Copyright 2012-present the original author or authors.
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
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.springframework.boot.build.bom.Library.Group;
import org.springframework.boot.build.bom.Library.ImportedBom;
import org.springframework.boot.build.bom.Library.Link;
import org.springframework.boot.build.bom.Library.LinkType;
import org.springframework.boot.build.bom.Library.LinkedVersion;
import org.springframework.boot.build.bom.Library.Module;
import org.springframework.boot.build.bom.ResolvedBom.Bom;
import org.springframework.boot.build.bom.ResolvedBom.Id;
import org.springframework.boot.build.bom.ResolvedBom.ResolvedLibrary;
import org.springframework.boot.build.xml.XmlDocument;
import org.springframework.util.Assert;

/**
 * Creates a {@link ResolvedBom resolved bom}.
 *
 * @author Andy Wilkinson
 */
class BomResolver {

	private static final Set<String> SKIPPED_OFFLINE_JAVADOC_LIBRARIES = Set.of("Spring Boot");

	private final ConfigurationContainer configurations;

	private final DependencyHandler dependencies;

	private final DocumentBuilder documentBuilder;

	BomResolver(ConfigurationContainer configurations, DependencyHandler dependencies) {
		this.configurations = configurations;
		this.dependencies = dependencies;
		this.documentBuilder = XmlDocument.builder();
	}

	ResolvedBom resolve(BomExtension bom) {
		Id id = Id.parse(bom.getId());
		List<ResolvedLibrary> resolvedLibraries = bom.getLibraries().stream().map(this::resolve).toList();
		Map<URI, List<String>> offlineJavadocLinks = getOfflineJavadocLinks(bom, resolvedLibraries);
		return new ResolvedBom(id, resolvedLibraries, offlineJavadocLinks);
	}

	private ResolvedLibrary resolve(Library library) {
		List<Id> managedDependencies = new ArrayList<>();
		List<Bom> imports = new ArrayList<>();
		for (Group group : library.getGroups()) {
			for (Module module : group.getModules()) {
				Id managedDependency = new Id(group.getId(), module.getName(), library.getVersion().toString());
				managedDependencies.add(managedDependency);
			}
			for (ImportedBom importedBom : group.getBoms()) {
				String importedId = "%s:%s:%s".formatted(group.getId(), importedBom.name(), library.getVersion());
				imports.add(resolveBom(importedId));
			}
		}
		return new ResolvedLibrary(library.getName(), library.getVersion().toString(), library.getVersionProperty(),
				managedDependencies, imports);
	}

	private File resolveBomFile(String coordinates) {
		Set<ResolvedArtifact> artifacts = this.configurations
			.detachedConfiguration(this.dependencies.create(coordinates + "@pom"))
			.getResolvedConfiguration()
			.getResolvedArtifacts();
		Assert.state(artifacts.size() == 1, "Expected a single artifact but '%s' resolved to %d artifacts"
			.formatted(coordinates, artifacts.size()));
		return artifacts.iterator().next().getFile();
	}

	private Bom bomFrom(File bomFile) {
		try {
			Node bom = nodeFrom(bomFile);
			File parentBomFile = parentBomFile(bom);
			Bom parent = (parentBomFile != null) ? bomFrom(parentBomFile) : null;
			Properties properties = Properties.from(bom, this::nodeFrom);
			List<Node> dependencyNodes = bom.nodesAt("/project/dependencyManagement/dependencies/dependency");
			List<Id> managedDependencies = new ArrayList<>();
			List<Bom> imports = new ArrayList<>();
			for (Node dependency : dependencyNodes) {
				Id dependencyId = getDependencyId(dependency, properties);
				Bom importedBom = (isPomImport(properties, dependency)) ? resolveBom(dependencyId.toString()) : null;
				if (importedBom != null) {
					imports.add(importedBom);
				}
				else {
					managedDependencies.add(dependencyId);
				}
			}
			Id bomId = getBomId(bom, parent);
			return new Bom(bomId, parent, managedDependencies, imports);
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private Id getDependencyId(Node dependency, Properties properties) {
		String groupId = properties.replace(dependency.textAt("groupId"));
		String artifactId = properties.replace(dependency.textAt("artifactId"));
		String version = properties.replace(dependency.textAt("version"));
		String classifier = properties.replace(dependency.textAt("classifier"));
		return new Id(groupId, artifactId, version, classifier);
	}

	private boolean isPomImport(Properties properties, Node dependency) {
		String scope = properties.replace(dependency.textAt("scope"));
		return "import".equals(scope) && "pom".equals(properties.replace(dependency.textAt("type")));
	}

	private Id getBomId(Node bom, Bom parent) {
		String groupId = bom.textAt("/project/groupId");
		if ((groupId == null || groupId.isEmpty()) && parent != null) {
			groupId = parent.id().groupId();
		}
		String artifactId = bom.textAt("/project/artifactId");
		String version = bom.textAt("/project/version");
		if ((version == null || version.isEmpty()) && parent != null) {
			version = parent.id().version();
		}
		return new Id(groupId, artifactId, version);
	}

	Bom resolveBom(String coordinates) {
		return bomFrom(resolveBomFile(coordinates));
	}

	private Node nodeFrom(String coordinates) {
		return nodeFrom(resolveBomFile(coordinates));
	}

	private Node nodeFrom(File bomFile) {
		try {
			Document document = this.documentBuilder.parse(bomFile);
			return new Node(document);
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private File parentBomFile(Node bom) {
		Node parent = bom.nodeAt("/project/parent");
		if (parent != null) {
			String parentGroupId = parent.textAt("groupId");
			String parentArtifactId = parent.textAt("artifactId");
			String parentVersion = parent.textAt("version");
			return resolveBomFile(parentGroupId + ":" + parentArtifactId + ":" + parentVersion);
		}
		return null;
	}

	private Map<URI, List<String>> getOfflineJavadocLinks(BomExtension bom, List<ResolvedLibrary> resolvedLibraries) {
		Map<URI, List<String>> offlineJavadocLinks = new TreeMap<>();
		for (Library library : bom.getLibraries()) {
			if (SKIPPED_OFFLINE_JAVADOC_LIBRARIES.contains(library.getName())) {
				continue;
			}
			ResolvedLibrary resolvedLibrary = ResolvedLibrary.find(resolvedLibraries, library);
			Set<Id> linkedDependencies = new TreeSet<>(resolvedLibrary.allDependencies().toList());
			library.getModuleLinks().forEach((module, moduleLinks) -> {
				moduleLinks.byType().getOrDefault(LinkType.JAVADOC, Collections.emptyList()).forEach((link) -> {
					String version = resolvedLibrary.moduleVersion(module);
					List<String> moduleNames = module.moduleNames();
					addOfflineJavadocLink(offlineJavadocLinks, link, version, moduleNames.stream());
					module.moduleNames()
						.forEach((moduleName) -> linkedDependencies
							.removeIf((candidate) -> candidate.artifactId().equals(moduleName)));
				});
			});
			library.getLinks().byType().getOrDefault(LinkType.JAVADOC, Collections.emptyList()).forEach((link) -> {
				Stream<String> modulesNames = linkedDependencies.stream().map(Id::artifactId);
				addOfflineJavadocLink(offlineJavadocLinks, link, library.getVersion(), modulesNames);
			});
		}
		return offlineJavadocLinks;
	}

	private void addOfflineJavadocLink(Map<URI, List<String>> offlineJavadocLinks, Link link, Object version,
			Stream<String> moduleNames) {
		URI uri = URI.create(link.url(new LinkedVersion(version)));
		List<String> jars = moduleNames.map((name) -> "%s-%s-javadoc.jar".formatted(name, version)).toList();
		offlineJavadocLinks.computeIfAbsent(uri, (key) -> new ArrayList<>()).addAll(jars);
	}

	private static final class Node {

		private final XPath xpath;

		private final org.w3c.dom.Node delegate;

		private Node(org.w3c.dom.Node delegate) {
			this(delegate, XPathFactory.newInstance().newXPath());
		}

		private Node(org.w3c.dom.Node delegate, XPath xpath) {
			this.delegate = delegate;
			this.xpath = xpath;
		}

		private String textAt(String expression) {
			String text = (String) evaluate(expression + "/text()", XPathConstants.STRING);
			return (text != null && !text.isBlank()) ? text : null;
		}

		private Node nodeAt(String expression) {
			org.w3c.dom.Node result = (org.w3c.dom.Node) evaluate(expression, XPathConstants.NODE);
			return (result != null) ? new Node(result, this.xpath) : null;
		}

		private List<Node> nodesAt(String expression) {
			NodeList nodes = (NodeList) evaluate(expression, XPathConstants.NODESET);
			List<Node> things = new ArrayList<>(nodes.getLength());
			for (int i = 0; i < nodes.getLength(); i++) {
				things.add(new Node(nodes.item(i), this.xpath));
			}
			return things;
		}

		private Object evaluate(String expression, QName type) {
			try {
				return this.xpath.evaluate(expression, this.delegate, type);
			}
			catch (XPathExpressionException ex) {
				throw new RuntimeException(ex);
			}
		}

		private String name() {
			return this.delegate.getNodeName();
		}

		private String textContent() {
			return this.delegate.getTextContent();
		}

	}

	private static final class Properties {

		private final Map<String, String> properties;

		private Properties(Map<String, String> properties) {
			this.properties = properties;
		}

		private static Properties from(Node bom, Function<String, Node> resolver) {
			try {
				Map<String, String> properties = new HashMap<>();
				Node current = bom;
				while (current != null) {
					String groupId = current.textAt("/project/groupId");
					if (groupId != null && !groupId.isEmpty()) {
						properties.putIfAbsent("${project.groupId}", groupId);
					}
					String version = current.textAt("/project/version");
					if (version != null && !version.isEmpty()) {
						properties.putIfAbsent("${project.version}", version);
					}
					List<Node> propertyNodes = current.nodesAt("/project/properties/*");
					for (Node property : propertyNodes) {
						properties.putIfAbsent("${%s}".formatted(property.name()), property.textContent());
					}
					current = parent(current, resolver);
				}
				return new Properties(properties);
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		private static Node parent(Node current, Function<String, Node> resolver) {
			Node parent = current.nodeAt("/project/parent");
			if (parent != null) {
				String parentGroupId = parent.textAt("groupId");
				String parentArtifactId = parent.textAt("artifactId");
				String parentVersion = parent.textAt("version");
				return resolver.apply(parentGroupId + ":" + parentArtifactId + ":" + parentVersion);
			}
			return null;
		}

		private String replace(String input) {
			if (input != null && input.startsWith("${") && input.endsWith("}")) {
				String value = this.properties.get(input);
				if (value != null) {
					return replace(value);
				}
				throw new IllegalStateException("No replacement for " + input);
			}
			return input;
		}

	}

}
