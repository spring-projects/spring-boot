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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Provides access to the managed dependencies declared in
 * {@literal spring-boot-dependencies}.
 * 
 * @author Phillip Webb
 * @see Dependency
 */
public class ManagedDependencies implements Iterable<Dependency> {

	private static ManagedDependencies instance;

	private final String version;

	private final Map<ArtifactAndGroupId, Dependency> byArtifactAndGroupId;

	private final Map<String, Dependency> byArtifactId;

	ManagedDependencies(String dependenciesPomResource, String effectivePomResource) {
		try {
			Document dependenciesPomDocument = readDocument(dependenciesPomResource);
			this.version = dependenciesPomDocument.getElementsByTagName("version")
					.item(0).getTextContent();

			// Parse all dependencies from the effective POM (with resolved properties)
			Document effectivePomDocument = readDocument(effectivePomResource);
			Map<ArtifactAndGroupId, Dependency> all = new HashMap<ArtifactAndGroupId, Dependency>();
			for (Dependency dependency : readDependencies(effectivePomDocument)) {
				all.put(new ArtifactAndGroupId(dependency), dependency);
			}

			// But only add those from the dependencies POM
			this.byArtifactAndGroupId = new LinkedHashMap<ManagedDependencies.ArtifactAndGroupId, Dependency>();
			this.byArtifactId = new LinkedHashMap<String, Dependency>();
			for (Dependency dependency : readDependencies(dependenciesPomDocument)) {
				ArtifactAndGroupId artifactAndGroupId = new ArtifactAndGroupId(dependency);
				Dependency effectiveDependency = all.get(artifactAndGroupId);
				if (effectiveDependency != null) {
					this.byArtifactAndGroupId
							.put(artifactAndGroupId, effectiveDependency);
					this.byArtifactId.put(effectiveDependency.getArtifactId(),
							effectiveDependency);
				}
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private Document readDocument(String resource) throws Exception {
		InputStream stream = getClass().getResourceAsStream(resource);
		if (stream == null) {
			throw new IllegalStateException("Unable to open resource " + resource);
		}
		DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder();
		Document document = documentBuilder.parse(stream);
		document.getDocumentElement().normalize();
		return document;
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
				dependencies.add(Dependency.fromDependenciesXml((Element) node));
			}
		}
		return dependencies;
	}

	/**
	 * Return the 'spring-boot-dependencies' POM version.
	 */
	public String getVersion() {
		return this.version;
	}

	/**
	 * Find a single dependency for the given group and artifact IDs.
	 * @param groupId the group ID
	 * @param artifactId the artifact ID
	 * @return a {@link Dependency} or {@code null}
	 */
	public Dependency find(String groupId, String artifactId) {
		return this.byArtifactAndGroupId.get(new ArtifactAndGroupId(groupId, artifactId));
	}

	/**
	 * Find a single dependency for the artifact IDs.
	 * @param artifactId the artifact ID
	 * @return a {@link Dependency} or {@code null}
	 */
	public Dependency find(String artifactId) {
		return this.byArtifactId.get(artifactId);
	}

	/**
	 * Provide an {@link Iterator} over all managed {@link Dependency Dependencies}.
	 */
	@Override
	public Iterator<Dependency> iterator() {
		return this.byArtifactAndGroupId.values().iterator();
	}

	/**
	 * @return The Spring Boot managed dependencies.
	 */
	public static ManagedDependencies get() {
		if (instance == null) {
			return new ManagedDependencies("dependencies-pom.xml", "effective-pom.xml");
		}
		return instance;
	}

	/**
	 * Simple holder for an artifact+group ID.
	 */
	private static class ArtifactAndGroupId {

		private final String groupId;

		private final String artifactId;

		public ArtifactAndGroupId(Dependency dependency) {
			this(dependency.getGroupId(), dependency.getArtifactId());
		}

		public ArtifactAndGroupId(String groupId, String artifactId) {
			Assert.notNull(groupId, "GroupId must not be null");
			Assert.notNull(artifactId, "ArtifactId must not be null");
			this.groupId = groupId;
			this.artifactId = artifactId;
		}

		@Override
		public int hashCode() {
			return this.groupId.hashCode() * 31 + this.artifactId.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() == obj.getClass()) {
				ArtifactAndGroupId other = (ArtifactAndGroupId) obj;
				boolean result = true;
				result &= this.groupId.equals(other.groupId);
				result &= this.artifactId.equals(other.artifactId);
				return result;
			}
			return false;
		}

	}
}
