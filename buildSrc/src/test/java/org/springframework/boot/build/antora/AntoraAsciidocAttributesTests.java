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

package org.springframework.boot.build.antora;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.Library.Group;
import org.springframework.boot.build.bom.Library.LibraryVersion;
import org.springframework.boot.build.bom.Library.ProhibitedVersion;
import org.springframework.boot.build.bom.Library.VersionAlignment;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AntoraAsciidocAttributes}.
 *
 * @author Phillip Webb
 */
class AntoraAsciidocAttributesTests {

	@Test
	void githubRefWhenReleasedVersionIsTag() {
		AntoraAsciidocAttributes attributes = new AntoraAsciidocAttributes("1.2.3", true, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("github-ref", "v1.2.3");
	}

	@Test
	void githubRefWhenLatestSnapshotVersionIsMainBranch() {
		AntoraAsciidocAttributes attributes = new AntoraAsciidocAttributes("1.2.3-SNAPSHOT", true, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("github-ref", "main");
	}

	@Test
	void githubRefWhenOlderSnapshotVersionIsBranch() {
		AntoraAsciidocAttributes attributes = new AntoraAsciidocAttributes("1.2.3-SNAPSHOT", false, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("github-ref", "1.2.x");
	}

	@Test
	void githubRefWhenOlderSnapshotHotFixVersionIsBranch() {
		AntoraAsciidocAttributes attributes = new AntoraAsciidocAttributes("1.2.3.1-SNAPSHOT", false, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("github-ref", "1.2.3.x");
	}

	@Test
	void versionReferenceFromLibrary() {
		Library library = mockLibrary(Collections.emptyMap());
		AntoraAsciidocAttributes attributes = new AntoraAsciidocAttributes("1.2.3.1-SNAPSHOT", false, List.of(library),
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("version-spring-framework", "1.2.3");
	}

	@Test
	void versionReferenceFromSpringDataDependencyVersion() {
		AntoraAsciidocAttributes attributes = new AntoraAsciidocAttributes("1.2.3", true, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("version-spring-data-mongodb", "1.2.3");
	}

	@Test
	void versionNativeBuildTools() {
		AntoraAsciidocAttributes attributes = new AntoraAsciidocAttributes("1.2.3", true, null,
				mockDependencyVersions(), Map.of("nativeBuildToolsVersion", "3.4.5"));
		assertThat(attributes.get()).containsEntry("version-native-build-tools", "3.4.5");
	}

	@Test
	void urlArtifactRepositoryWhenRelease() {
		AntoraAsciidocAttributes attributes = new AntoraAsciidocAttributes("1.2.3", true, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("url-artifact-repository", "https://repo.maven.apache.org/maven2");
	}

	@Test
	void urlArtifactRepositoryWhenMilestone() {
		AntoraAsciidocAttributes attributes = new AntoraAsciidocAttributes("1.2.3-M1", true, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("url-artifact-repository", "https://repo.spring.io/milestone");
	}

	@Test
	void urlArtifactRepositoryWhenSnapshot() {
		AntoraAsciidocAttributes attributes = new AntoraAsciidocAttributes("1.2.3-SNAPSHOT", true, null,
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("url-artifact-repository", "https://repo.spring.io/snapshot");
	}

	@Test
	void urlLinksFromLibrary() {
		Map<String, Function<LibraryVersion, String>> links = new LinkedHashMap<>();
		links.put("site", (version) -> "https://example.com/site/" + version);
		links.put("docs", (version) -> "https://example.com/docs/" + version);
		Library library = mockLibrary(links);
		AntoraAsciidocAttributes attributes = new AntoraAsciidocAttributes("1.2.3.1-SNAPSHOT", false, List.of(library),
				mockDependencyVersions(), null);
		assertThat(attributes.get()).containsEntry("url-spring-framework-site", "https://example.com/site/1.2.3")
			.containsEntry("url-spring-framework-docs", "https://example.com/docs/1.2.3");
	}

	@Test
	void linksFromProperties() {
		Map<String, String> attributes = new AntoraAsciidocAttributes("1.2.3-SNAPSHOT", true, null,
				mockDependencyVersions(), null)
			.get();
		assertThat(attributes).containsEntry("include-java", "ROOT:example$java/org/springframework/boot/docs");
		assertThat(attributes).containsEntry("url-spring-data-cassandra-site",
				"https://spring.io/projects/spring-data-cassandra");
		List<String> keys = new ArrayList<>(attributes.keySet());
		assertThat(keys.indexOf("include-java")).isLessThan(keys.indexOf("code-spring-boot-latest"));
	}

	private Library mockLibrary(Map<String, Function<LibraryVersion, String>> links) {
		String name = "Spring Framework";
		String calendarName = null;
		LibraryVersion version = new LibraryVersion(DependencyVersion.parse("1.2.3"));
		List<Group> groups = Collections.emptyList();
		List<ProhibitedVersion> prohibitedVersion = Collections.emptyList();
		boolean considerSnapshots = false;
		VersionAlignment versionAlignment = null;
		String alignsWithBom = null;
		String linkRootName = null;
		Library library = new Library(name, calendarName, version, groups, prohibitedVersion, considerSnapshots,
				versionAlignment, alignsWithBom, linkRootName, links);
		return library;
	}

	private Map<String, String> mockDependencyVersions() {
		Map<String, String> versions = new LinkedHashMap<>();
		addMockSpringDataVersion(versions, "spring-data-commons");
		addMockSpringDataVersion(versions, "spring-data-couchbase");
		addMockSpringDataVersion(versions, "spring-data-elasticsearch");
		addMockSpringDataVersion(versions, "spring-data-jdbc");
		addMockSpringDataVersion(versions, "spring-data-jpa");
		addMockSpringDataVersion(versions, "spring-data-mongodb");
		addMockSpringDataVersion(versions, "spring-data-neo4j");
		addMockSpringDataVersion(versions, "spring-data-r2dbc");
		addMockSpringDataVersion(versions, "spring-data-rest-core");
		return versions;
	}

	private void addMockSpringDataVersion(Map<String, String> versions, String artifactId) {
		versions.put("org.springframework.data:" + artifactId, "1.2.3");
	}

}
