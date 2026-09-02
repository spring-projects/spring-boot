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

package org.springframework.boot.build.antora;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.gradle.api.Project;

import org.springframework.boot.build.artifacts.ArtifactRelease;
import org.springframework.boot.build.bom.BomExtension;
import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.Library.Link;
import org.springframework.boot.build.bom.Library.LinkType;
import org.springframework.boot.build.bom.Library.LinkedVersion;
import org.springframework.boot.build.bom.ResolvedBom;
import org.springframework.boot.build.bom.ResolvedBom.Id;
import org.springframework.boot.build.bom.ResolvedBom.ResolvedLibrary;
import org.springframework.boot.build.properties.BuildProperties;
import org.springframework.boot.build.properties.BuildType;
import org.springframework.util.Assert;

/**
 * Generates Asciidoctor attributes for use with Antora.
 *
 * @author Phillip Webb
 */
public class AntoraAsciidocAttributes {

	private static final String DASH_SNAPSHOT = "-SNAPSHOT";

	private final String version;

	private final boolean latestVersion;

	private final BuildType buildType;

	private final ArtifactRelease artifactRelease;

	private final List<Library> libraries;

	private final ResolvedBom resolvedBom;

	private final Map<String, String> dependencyVersions;

	private final Map<String, ?> projectProperties;

	public AntoraAsciidocAttributes(Project project, BomExtension dependencyBom, ResolvedBom resolvedBom) {
		this.version = String.valueOf(project.getVersion());
		this.latestVersion = Boolean.parseBoolean(String.valueOf(project.findProperty("latestVersion")));
		this.buildType = BuildProperties.get(project).buildType();
		this.artifactRelease = ArtifactRelease.forProject(project);
		this.libraries = dependencyBom.getLibraries();
		this.resolvedBom = resolvedBom;
		this.dependencyVersions = resolvedBom.dependencyVersions();
		this.projectProperties = project.getProperties();
	}

	public Map<String, String> get() {
		Map<String, String> attributes = new LinkedHashMap<>();
		Map<String, String> internal = new LinkedHashMap<>();
		addBuildTypeAttribute(attributes);
		addGitHubAttributes(attributes);
		addVersionAttributes(attributes, internal);
		addArtifactAttributes(attributes);
		addUrlJava(attributes);
		addLinksAttributes(attributes);
		addPropertyAttributes(attributes, internal);
		return attributes;
	}

	private void addBuildTypeAttribute(Map<String, String> attributes) {
		attributes.put("build-type", this.buildType.toIdentifier());
	}

	private void addGitHubAttributes(Map<String, String> attributes) {
		attributes.put("github-repo", "spring-projects/spring-boot");
		attributes.put("github-ref", determineGitHubRef());
	}

	private String determineGitHubRef() {
		int snapshotIndex = this.version.lastIndexOf(DASH_SNAPSHOT);
		if (snapshotIndex == -1) {
			return "v" + this.version;
		}
		if (this.latestVersion) {
			return "main";
		}
		String versionRoot = this.version.substring(0, snapshotIndex);
		int lastDot = versionRoot.lastIndexOf('.');
		return versionRoot.substring(0, lastDot) + ".x";
	}

	private void addVersionAttributes(Map<String, String> attributes, Map<String, String> internal) {
		this.libraries.forEach((library) -> {
			String name = "version-" + library.getLinkRootName();
			String value = library.getVersion().toString();
			attributes.put(name, value);
		});
		attributes.put("version-native-build-tools", (String) this.projectProperties.get("nativeBuildToolsVersion"));
		attributes.put("version-graal", (String) this.projectProperties.get("graalVersion"));
		addDependencyVersion(attributes, "jackson-annotations", "com.fasterxml.jackson.core:jackson-annotations");
		addDependencyVersion(attributes, "jackson-core", "tools.jackson.core:jackson-core");
		addDependencyVersion(attributes, "jackson-databind", "tools.jackson.core:jackson-databind");
		addDependencyVersion(attributes, "jackson-dataformat-xml", "tools.jackson.dataformat:jackson-dataformat-xml");
		addDependencyVersion(attributes, "jackson2-databind", "com.fasterxml.jackson.core:jackson-databind");
		addSpringDataDependencyVersion(attributes, internal, "spring-data-commons");
		addSpringDataDependencyVersion(attributes, internal, "spring-data-couchbase");
		addSpringDataDependencyVersion(attributes, internal, "spring-data-cassandra");
		addSpringDataDependencyVersion(attributes, internal, "spring-data-elasticsearch");
		addSpringDataDependencyVersion(attributes, internal, "spring-data-jdbc");
		addSpringDataDependencyVersion(attributes, internal, "spring-data-jpa");
		addSpringDataDependencyVersion(attributes, internal, "spring-data-mongodb");
		addSpringDataDependencyVersion(attributes, internal, "spring-data-neo4j");
		addSpringDataDependencyVersion(attributes, internal, "spring-data-r2dbc");
		addSpringDataDependencyVersion(attributes, internal, "spring-data-redis");
		addSpringDataDependencyVersion(attributes, internal, "spring-data-rest", "spring-data-rest-core");
		addSpringDataDependencyVersion(attributes, internal, "spring-data-ldap");
		addDependencyVersion(attributes, "pulsar-client-api", "org.apache.pulsar:pulsar-client-api");
	}

	private void addSpringDataDependencyVersion(Map<String, String> attributes, Map<String, String> internal,
			String artifactId) {
		addSpringDataDependencyVersion(attributes, internal, artifactId, artifactId);
	}

	private void addSpringDataDependencyVersion(Map<String, String> attributes, Map<String, String> internal,
			String name, String artifactId) {
		String groupAndArtifactId = "org.springframework.data:" + artifactId;
		addDependencyVersion(attributes, name, groupAndArtifactId);
		String version = getVersion(groupAndArtifactId);
		String majorMinor = Arrays.stream(version.split("\\.")).limit(2).collect(Collectors.joining("."));
		String antoraVersion = version.endsWith(DASH_SNAPSHOT) ? majorMinor + DASH_SNAPSHOT : majorMinor;
		internal.put("antoraversion-" + name, antoraVersion);
		internal.put("dotxversion-" + name, majorMinor + ".x");
	}

	private void addDependencyVersion(Map<String, String> attributes, String name, String groupAndArtifactId) {
		attributes.put("version-" + name, getVersion(groupAndArtifactId));
	}

	private String getVersion(String groupAndArtifactId) {
		String version = this.dependencyVersions.get(groupAndArtifactId);
		Assert.notNull(version, () -> "No version found for " + groupAndArtifactId);
		return version;
	}

	private void addArtifactAttributes(Map<String, String> attributes) {
		attributes.put("url-artifact-repository", this.artifactRelease.getDownloadRepo());
		attributes.put("artifact-release-type", this.artifactRelease.getType());
		attributes.put("build-and-artifact-release-type",
				this.buildType.toIdentifier() + "-" + this.artifactRelease.getType());
	}

	private void addUrlJava(Map<String, String> attributes) {
		attributes.put("url-javase-javadoc", "https://docs.oracle.com/en/java/javase/17/docs/api");
		attributes.put("javadoc-location-java", "{url-javase-javadoc}/java.base");
		attributes.put("javadoc-location-java-beans", "{url-javase-javadoc}/java.desktop");
		attributes.put("javadoc-location-java-sql", "{url-javase-javadoc}/java.sql");
		attributes.put("javadoc-location-javax", "{url-javase-javadoc}/java.base");
		attributes.put("javadoc-location-javax-management", "{url-javase-javadoc}/java.management");
		attributes.put("javadoc-location-javax-net", "{url-javase-javadoc}/java.base");
		attributes.put("javadoc-location-javax-sql", "{url-javase-javadoc}/java.sql");
		attributes.put("javadoc-location-javax-xml", "{url-javase-javadoc}/java.xml");
	}

	private void addLinksAttributes(Map<String, String> attributes) {
		LinkAttributes linkAttributes = new LinkAttributes();
		this.libraries.forEach(linkAttributes::add);
		linkAttributes.copyTo(attributes);
	}

	private void addPropertyAttributes(Map<String, String> attributes, Map<String, String> internal) {
		Properties properties = new Properties() {

			@Override
			public synchronized Object put(Object key, Object value) {
				// Put directly because order is important for us
				return attributes.put(key.toString(), resolve(value.toString(), internal));
			}

		};
		try (InputStream in = getClass().getResourceAsStream("antora-asciidoc-attributes.properties")) {
			properties.load(in);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private String resolve(String value, Map<String, String> internal) {
		for (Map.Entry<String, String> entry : internal.entrySet()) {
			value = value.replace("{" + entry.getKey() + "}", entry.getValue());
		}
		return value;
	}

	class LinkAttributes {

		private final Map<String, String> urlAttributes = new TreeMap<>();

		private final Map<String, String> packageAttributes = new TreeMap<>();

		void add(Library library) {
			ResolvedLibrary resolvedLibrary = AntoraAsciidocAttributes.this.resolvedBom.library(library);
			LinkedVersion libraryVersion = new LinkedVersion(library.getVersion());
			String libraryName = library.getLinkRootName();
			library.getLinks().forEachLink((type, link) -> addLinkAttributes(libraryName, type, link, libraryVersion));
			library.getModuleLinks().forEach((moduleName, links) -> {
				Id resolvedModule = resolvedLibrary.module(moduleName);
				LinkedVersion moduleVersion = new LinkedVersion(resolvedModule.version());
				links.forEachLink((type, link) -> addLinkAttributes(moduleName, type, link, moduleVersion));
			});
		}

		private void addLinkAttributes(String root, LinkType type, Link link, LinkedVersion version) {
			String urlAttributeName = "url-" + root + "-" + type.attributeName();
			this.urlAttributes.put(urlAttributeName, link.url(version));
			link.packages()
				.stream()
				.map(this::packageAttributeName)
				.forEach((packageAttributeName) -> this.packageAttributes.put(packageAttributeName,
						"{" + urlAttributeName + "}"));
		}

		private String packageAttributeName(String packageName) {
			return "javadoc-location-" + packageName.replace('.', '-');
		}

		void copyTo(Map<String, String> attributes) {
			// URL attributes first since package attributes reference them
			attributes.putAll(this.urlAttributes);
			attributes.putAll(this.packageAttributes);
		}

	}

}
