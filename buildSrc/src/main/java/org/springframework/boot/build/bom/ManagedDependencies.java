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
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.springframework.boot.build.bom.Library.Group;
import org.springframework.boot.build.bom.Library.Module;

/**
 * Managed dependencies from a bom or library.
 *
 * @author Andy Wilkinson
 */
class ManagedDependencies {

	private final Set<String> ids;

	ManagedDependencies(Set<String> ids) {
		this.ids = ids;
	}

	Set<String> getIds() {
		return this.ids;
	}

	Difference diff(ManagedDependencies other) {
		Set<String> missing = new HashSet<>(this.ids);
		missing.removeAll(other.ids);
		Set<String> unexpected = new HashSet<>(other.ids);
		unexpected.removeAll(this.ids);
		return new Difference(missing, unexpected);
	}

	static ManagedDependencies ofBom(File bom) {
		try {
			Document bomDocument = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder()
				.parse(new InputSource(new FileReader(bom)));
			XPath xpath = XPathFactory.newInstance().newXPath();
			NodeList dependencyNodes = (NodeList) xpath
				.evaluate("/project/dependencyManagement/dependencies/dependency", bomDocument, XPathConstants.NODESET);
			NodeList propertyNodes = (NodeList) xpath.evaluate("/project/properties/*", bomDocument,
					XPathConstants.NODESET);
			Map<String, String> properties = new HashMap<>();
			for (int i = 0; i < propertyNodes.getLength(); i++) {
				Node property = propertyNodes.item(i);
				String name = property.getNodeName();
				String value = property.getTextContent();
				properties.put("${%s}".formatted(name), value);
			}
			Set<String> managedDependencies = new HashSet<>();
			for (int i = 0; i < dependencyNodes.getLength(); i++) {
				Node dependency = dependencyNodes.item(i);
				String groupId = (String) xpath.evaluate("groupId/text()", dependency, XPathConstants.STRING);
				String artifactId = (String) xpath.evaluate("artifactId/text()", dependency, XPathConstants.STRING);
				String version = (String) xpath.evaluate("version/text()", dependency, XPathConstants.STRING);
				String classifier = (String) xpath.evaluate("classifier/text()", dependency, XPathConstants.STRING);
				if (version.startsWith("${") && version.endsWith("}")) {
					version = properties.get(version);
				}
				managedDependencies.add(asId(groupId, artifactId, version, classifier));
			}
			return new ManagedDependencies(managedDependencies);
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	static String asId(String groupId, String artifactId, String version, String classifier) {
		String id = groupId + ":" + artifactId + ":" + version;
		if (classifier != null && !classifier.isEmpty()) {
			id = id + ":" + classifier;
		}
		return id;
	}

	static ManagedDependencies ofLibrary(Library library) {
		Set<String> managedByLibrary = new HashSet<>();
		for (Group group : library.getGroups()) {
			for (Module module : group.getModules()) {
				managedByLibrary.add(asId(group.getId(), module.getName(), library.getVersion().getVersion().toString(),
						module.getClassifier()));
			}
		}
		return new ManagedDependencies(managedByLibrary);
	}

	record Difference(Set<String> missing, Set<String> unexpected) {

		boolean isEmpty() {
			return this.missing.isEmpty() && this.unexpected.isEmpty();
		}

	}

}
