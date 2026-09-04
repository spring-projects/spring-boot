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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.junit.jupiter.api.Test;

import org.springframework.boot.build.bom.BomExtension;
import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.Library.BomAlignment;
import org.springframework.boot.build.bom.Library.FirstParty;
import org.springframework.boot.build.bom.Library.Group;
import org.springframework.boot.build.bom.Library.Link;
import org.springframework.boot.build.bom.Library.LinkType;
import org.springframework.boot.build.bom.Library.LinkedModule;
import org.springframework.boot.build.bom.Library.LinkedVersion;
import org.springframework.boot.build.bom.Library.Links;
import org.springframework.boot.build.bom.Library.ProhibitedVersion;
import org.springframework.boot.build.bom.Library.VersionAlignment;
import org.springframework.boot.build.bom.ResolvedBom;
import org.springframework.boot.build.bom.ResolvedBom.ResolvedLibrary;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;
import org.springframework.boot.build.properties.BuildType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AntoraAsciidocAttributes}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class AntoraAsciidocAttributesTests {

	@Test
	void buildTypeWhenOpenSource() {
		AntoraAsciidocAttributes attributes = attributes("1.2.3", true, BuildType.OPEN_SOURCE, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("build-type", "opensource");
	}

	@Test
	void buildTypeWhenCommercial() {
		AntoraAsciidocAttributes attributes = attributes("1.2.3", true, BuildType.COMMERCIAL, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("build-type", "commercial");
	}

	@Test
	void githubRefWhenReleasedVersionIsTag() {
		AntoraAsciidocAttributes attributes = attributes("1.2.3", true, BuildType.OPEN_SOURCE, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("github-ref", "v1.2.3");
	}

	@Test
	void githubRefWhenLatestSnapshotVersionIsMainBranch() {
		AntoraAsciidocAttributes attributes = attributes("1.2.3-SNAPSHOT", true, BuildType.OPEN_SOURCE, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("github-ref", "main");
	}

	@Test
	void githubRefWhenOlderSnapshotVersionIsBranch() {
		AntoraAsciidocAttributes attributes = attributes("1.2.3-SNAPSHOT", false, BuildType.OPEN_SOURCE, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("github-ref", "1.2.x");
	}

	@Test
	void githubRefWhenOlderSnapshotHotFixVersionIsBranch() {
		AntoraAsciidocAttributes attributes = attributes("1.2.3.1-SNAPSHOT", false, BuildType.OPEN_SOURCE, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("github-ref", "1.2.3.x");
	}

	@Test
	void versionReferenceFromLibrary() {
		Library library = mockLibrary(Collections.emptyMap());
		AntoraAsciidocAttributes attributes = attributes("1.2.3.1-SNAPSHOT", false, BuildType.OPEN_SOURCE,
				List.of(library), mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("version-spring-framework", "1.2.3");
	}

	@Test
	void versionNativeBuildTools() {
		AntoraAsciidocAttributes attributes = attributes("1.2.3", true, BuildType.OPEN_SOURCE, null,
				mockDependencyVersions(), Map.of("nativeBuildToolsVersion", "3.4.5"));
		assertThat(attributes.get()).containsEntry("version-native-build-tools", "3.4.5");
	}

	@Test
	void urlArtifactRepositoryWhenRelease() {
		AntoraAsciidocAttributes attributes = attributes("1.2.3", true, BuildType.OPEN_SOURCE, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("url-artifact-repository", "https://repo.maven.apache.org/maven2");
	}

	@Test
	void urlArtifactRepositoryWhenMilestone() {
		AntoraAsciidocAttributes attributes = attributes("1.2.3-M1", true, BuildType.OPEN_SOURCE, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("url-artifact-repository", "https://repo.maven.apache.org/maven2");
	}

	@Test
	void urlArtifactRepositoryWhenSnapshot() {
		AntoraAsciidocAttributes attributes = attributes("1.2.3-SNAPSHOT", true, BuildType.OPEN_SOURCE, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("url-artifact-repository", "https://repo.spring.io/snapshot");
	}

	@Test
	void artifactReleaseTypeWhenOpenSourceRelease() {
		AntoraAsciidocAttributes attributes = attributes("1.2.3", true, BuildType.OPEN_SOURCE, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("artifact-release-type", "release");
		assertThat(attributes.get()).containsEntry("build-and-artifact-release-type", "opensource-release");
	}

	@Test
	void artifactReleaseTypeWhenOpenSourceMilestone() {
		AntoraAsciidocAttributes attributes = attributes("1.2.3-M1", true, BuildType.OPEN_SOURCE, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("artifact-release-type", "milestone");
		assertThat(attributes.get()).containsEntry("build-and-artifact-release-type", "opensource-milestone");
	}

	@Test
	void artifactReleaseTypeWhenOpenSourceSnapshot() {
		AntoraAsciidocAttributes attributes = attributes("1.2.3-SNAPSHOT", true, BuildType.OPEN_SOURCE, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("artifact-release-type", "snapshot");
		assertThat(attributes.get()).containsEntry("build-and-artifact-release-type", "opensource-snapshot");
	}

	@Test
	void artifactReleaseTypeWhenCommercialRelease() {
		AntoraAsciidocAttributes attributes = attributes("1.2.3", true, BuildType.COMMERCIAL, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("artifact-release-type", "release");
		assertThat(attributes.get()).containsEntry("build-and-artifact-release-type", "commercial-release");
	}

	@Test
	void artifactReleaseTypeWhenCommercialMilestone() {
		AntoraAsciidocAttributes attributes = attributes("1.2.3-M1", true, BuildType.COMMERCIAL, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("artifact-release-type", "milestone");
		assertThat(attributes.get()).containsEntry("build-and-artifact-release-type", "commercial-milestone");
	}

	@Test
	void artifactReleaseTypeWhenCommercialSnapshot() {
		AntoraAsciidocAttributes attributes = attributes("1.2.3-SNAPSHOT", true, BuildType.COMMERCIAL, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("artifact-release-type", "snapshot");
		assertThat(attributes.get()).containsEntry("build-and-artifact-release-type", "commercial-snapshot");
	}

	@Test
	void urlLinksFromLibrary() {
		Map<LinkType, List<Link>> links = new LinkedHashMap<>();
		links.put(LinkType.SITE, singleLink((version) -> "https://example.com/site/" + version));
		links.put(LinkType.DOCS, singleLink((version) -> "https://example.com/docs/" + version));
		links.put(LinkType.JAVADOC,
				singleLink((version) -> "https://example.com/api/" + version, "org.springframework.[core|util]"));
		Library library = mockLibrary(links);
		AntoraAsciidocAttributes attributes = attributes("1.2.3.1-SNAPSHOT", false, BuildType.OPEN_SOURCE,
				List.of(library), mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("url-spring-framework-site", "https://example.com/site/1.2.3")
			.containsEntry("url-spring-framework-docs", "https://example.com/docs/1.2.3")
			.containsEntry("url-spring-framework-javadoc", "https://example.com/api/1.2.3");
		assertThat(attributes.get())
			.containsEntry("javadoc-location-org-springframework-core", "{url-spring-framework-javadoc}")
			.containsEntry("javadoc-location-org-springframework-util", "{url-spring-framework-javadoc}");
	}

	@Test
	void urlLinksFromLibraryModule() {
		Links links = new Links(Map.of(LinkType.SITE, singleLink((version) -> "https://example.com/site/" + version)));
		Map<LinkedModule, Links> moduleLinks = new LinkedHashMap<>();
		moduleLinks.put(LinkedModule.of("example-module"), new Links(Map.of(LinkType.JAVADOC,
				singleLink((version) -> "https://example.com/moduleapi/" + version, "com.example"))));
		Library library = mockLibrary(links, moduleLinks);
		Map<String, String> dependencyVersions = mockDependencyVersions();
		dependencyVersions.put("example-module", "3.4.5");
		AntoraAsciidocAttributes attributes = attributes("1.2.3.1-SNAPSHOT", false, BuildType.OPEN_SOURCE,
				List.of(library), dependencyVersions, null);
		assertThat(attributes.get()).containsEntry("url-spring-framework-site", "https://example.com/site/1.2.3")
			.containsEntry("url-example-module-javadoc", "https://example.com/moduleapi/3.4.5");
		assertThat(attributes.get()).containsEntry("javadoc-location-com-example", "{url-example-module-javadoc}");
	}

	private List<Link> singleLink(Function<LinkedVersion, String> factory, String... packages) {
		Link link = new Link(factory, List.of(packages));
		return List.of(link);
	}

	@Test
	void linksFromProperties() {
		Map<String, String> attributes = attributes("1.2.3-SNAPSHOT", true, BuildType.OPEN_SOURCE, null,
				mockDependencyVersions(), null)
			.get();
		assertThat(attributes).containsEntry("include-java", "ROOT:example$java/org/springframework/boot/docs");
		assertThat(attributes).containsEntry("url-github-wiki", "https://github.com/{github-repo}/wiki");
		List<String> keys = new ArrayList<>(attributes.keySet());
		assertThat(keys.indexOf("include-java")).isLessThan(keys.indexOf("code-spring-boot-latest"));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private AntoraAsciidocAttributes attributes(String version, boolean latestVersion, BuildType buildType,
			List<Library> libraries, Map<String, String> dependencyVersions, Map<String, ?> projectProperties) {
		libraries = (libraries != null) ? libraries : Collections.emptyList();
		projectProperties = (projectProperties != null) ? projectProperties : Collections.emptyMap();
		Project project = mock();
		ExtensionContainer extensions = mock();
		ExtraPropertiesExtension extraPropertiesExtension = mock();
		given(project.getVersion()).willReturn(version);
		given(project.findProperty("latestVersion")).willReturn(String.valueOf(latestVersion));
		given(project.findProperty("spring.build-type"))
			.willReturn((buildType == BuildType.OPEN_SOURCE) ? "oss" : "commercial");
		given(project.getProperties()).willReturn((Map) projectProperties);
		given(project.getExtensions()).willReturn(extensions);
		given(extensions.getExtraProperties()).willReturn(extraPropertiesExtension);
		BomExtension dependencyBom = mock();
		given(dependencyBom.getLibraries()).willReturn(libraries);
		ResolvedBom resolvedBom = mock();
		given(resolvedBom.dependencyVersions()).willReturn(dependencyVersions);
		ResolvedLibrary resolvedLibrary = mock();
		given(resolvedBom.library(any())).willReturn(resolvedLibrary);
		given(resolvedLibrary.moduleVersion(any())).willAnswer((invocation) -> {
			LinkedModule module = invocation.getArgument(0);
			return dependencyVersions.get(module.moduleNames().get(0));
		});
		return new AntoraAsciidocAttributes(project, dependencyBom, resolvedBom);
	}

	private Library mockLibrary(Map<LinkType, List<Link>> links) {
		return mockLibrary(new Links(links), Collections.emptyMap());
	}

	private Library mockLibrary(Links links, Map<LinkedModule, Links> moduleLinks) {
		String name = "Spring Framework";
		String calendarName = null;
		DependencyVersion version = DependencyVersion.parse("1.2.3");
		List<Group> groups = Collections.emptyList();
		List<ProhibitedVersion> prohibitedVersion = Collections.emptyList();
		FirstParty firstParty = null;
		VersionAlignment versionAlignment = null;
		BomAlignment alignsWithBom = null;
		String linkRootName = null;
		return new Library(name, calendarName, version, groups, null, prohibitedVersion, firstParty, versionAlignment,
				alignsWithBom, linkRootName, links, moduleLinks);
	}

	private Map<String, String> mockDependencyVersions() {
		return mockDependencyVersions("1.2.3");
	}

	private Map<String, String> mockDependencyVersions(String version) {
		Map<String, String> versions = new LinkedHashMap<>();
		addMockSpringDataVersion(versions, "spring-data-commons", version);
		addMockSpringDataVersion(versions, "spring-data-cassandra", version);
		addMockSpringDataVersion(versions, "spring-data-couchbase", version);
		addMockSpringDataVersion(versions, "spring-data-elasticsearch", version);
		addMockSpringDataVersion(versions, "spring-data-jdbc", version);
		addMockSpringDataVersion(versions, "spring-data-jpa", version);
		addMockSpringDataVersion(versions, "spring-data-mongodb", version);
		addMockSpringDataVersion(versions, "spring-data-neo4j", version);
		addMockSpringDataVersion(versions, "spring-data-r2dbc", version);
		addMockSpringDataVersion(versions, "spring-data-redis", version);
		addMockSpringDataVersion(versions, "spring-data-rest-core", version);
		addMockSpringDataVersion(versions, "spring-data-ldap", version);
		addMockTestcontainersVersion(versions, "activemq", version);
		addMockTestcontainersVersion(versions, "cassandra", version);
		addMockTestcontainersVersion(versions, "clickhouse", version);
		addMockTestcontainersVersion(versions, "couchbase", version);
		addMockTestcontainersVersion(versions, "elasticsearch", version);
		addMockTestcontainersVersion(versions, "grafana", version);
		addMockTestcontainersVersion(versions, "jdbc", version);
		addMockTestcontainersVersion(versions, "kafka", version);
		addMockTestcontainersVersion(versions, "mariadb", version);
		addMockTestcontainersVersion(versions, "mongodb", version);
		addMockTestcontainersVersion(versions, "mssqlserver", version);
		addMockTestcontainersVersion(versions, "mysql", version);
		addMockTestcontainersVersion(versions, "neo4j", version);
		addMockTestcontainersVersion(versions, "oracle-xe", version);
		addMockTestcontainersVersion(versions, "oracle-free", version);
		addMockTestcontainersVersion(versions, "postgresql", version);
		addMockTestcontainersVersion(versions, "pulsar", version);
		addMockTestcontainersVersion(versions, "rabbitmq", version);
		addMockTestcontainersVersion(versions, "redpanda", version);
		addMockTestcontainersVersion(versions, "r2dbc", version);
		addMockJackson2CoreVersion(versions, "jackson-annotations", version);
		addMockJackson2CoreVersion(versions, "jackson-databind", version);
		addMockJacksonCoreVersion(versions, "jackson-core", version);
		addMockJacksonCoreVersion(versions, "jackson-databind", version);
		addMockJacksonCoreVersion(versions, "jackson-databind", version);
		versions.put("org.apache.pulsar:pulsar-client-api", version);
		versions.put("tools.jackson.dataformat:jackson-dataformat-xml", version);
		return versions;
	}

	private void addMockSpringDataVersion(Map<String, String> versions, String artifactId, String version) {
		versions.put("org.springframework.data:" + artifactId, version);
	}

	private void addMockTestcontainersVersion(Map<String, String> versions, String artifactId, String version) {
		versions.put("org.testcontainers:" + artifactId, version);
	}

	private void addMockJackson2CoreVersion(Map<String, String> versions, String artifactId, String version) {
		versions.put("com.fasterxml.jackson.core:" + artifactId, version);
	}

	private void addMockJacksonCoreVersion(Map<String, String> versions, String artifactId, String version) {
		versions.put("tools.jackson.core:" + artifactId, version);
	}

}
