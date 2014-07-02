/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.dependency.tools;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.boot.dependency.tools.Dependency.Exclusion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * {@link Dependencies} implementation backed a maven POM.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.1.0
 */
public class PomDependencies extends AbstractDependencies {

	/**
	 * Create a new {@link PomDependencies} instance.
	 * @param effectivePomInputStream the effective POM containing resolved versions. The
	 * input stream will be closed once content has been loaded.
	 */
	public PomDependencies(InputStream effectivePomInputStream) {
		try {
			Document effectivePom = readDocument(effectivePomInputStream);
			for (Dependency dependency : readDependencies(effectivePom)) {
				add(new ArtifactAndGroupId(dependency), dependency);
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private Document readDocument(InputStream inputStream) throws Exception {
		if (inputStream == null) {
			return null;
		}
		try {
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			Document document = builder.parse(inputStream);
			document.getDocumentElement().normalize();
			return document;
		}
		finally {
			inputStream.close();
		}
	}

	private List<Dependency> readDependencies(Document document) throws Exception {
		Element element = (Element) document.getElementsByTagName("project").item(0);
		element = (Element) element.getElementsByTagName("dependencyManagement").item(0);
		element = (Element) element.getElementsByTagName("dependencies").item(0);
		NodeList nodes = element.getChildNodes();
		List<Dependency> dependencies = new ArrayList<Dependency>();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node instanceof Element) {
				dependencies.add(createDependency((Element) node));
			}
		}
		return dependencies;
	}

	private Dependency createDependency(Element element) throws Exception {
		String groupId = getTextContent(element, "groupId");
		String artifactId = getTextContent(element, "artifactId");
		String version = getTextContent(element, "version");
		List<Exclusion> exclusions = createExclusions(element
				.getElementsByTagName("exclusions"));
		return new Dependency(groupId, artifactId, version, exclusions);
	}

	private List<Exclusion> createExclusions(NodeList exclusion) {
		if (exclusion == null || exclusion.getLength() == 0) {
			return Collections.emptyList();
		}
		return createExclusions(exclusion.item(0));
	}

	private List<Exclusion> createExclusions(Node item) {
		List<Exclusion> exclusions = new ArrayList<Dependency.Exclusion>();
		NodeList children = item.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				exclusions.add(createExclusion((Element) child));
			}
		}
		return exclusions;
	}

	private Exclusion createExclusion(Element element) {
		String groupId = getTextContent(element, "groupId");
		String artifactId = getTextContent(element, "artifactId");
		return new Exclusion(groupId, artifactId);
	}

	private String getTextContent(Element element, String tagName) {
		return element.getElementsByTagName(tagName).item(0).getTextContent();
	}

}
