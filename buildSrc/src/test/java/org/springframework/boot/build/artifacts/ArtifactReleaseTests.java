/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.artifacts;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ArtifactRelease}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class ArtifactReleaseTests {

	@Test
	void whenProjectVersionIsSnapshotThenTypeIsSnapshot() {
		Project project = ProjectBuilder.builder().build();
		project.setVersion("1.2.3-SNAPSHOT");
		assertThat(ArtifactRelease.forProject(project).getType()).isEqualTo("snapshot");
	}

	@Test
	void whenProjectVersionIsMilestoneThenTypeIsMilestone() {
		Project project = ProjectBuilder.builder().build();
		project.setVersion("1.2.3-M1");
		assertThat(ArtifactRelease.forProject(project).getType()).isEqualTo("milestone");
	}

	@Test
	void whenProjectVersionIsReleaseCandidateThenTypeIsMilestone() {
		Project project = ProjectBuilder.builder().build();
		project.setVersion("1.2.3-RC1");
		assertThat(ArtifactRelease.forProject(project).getType()).isEqualTo("milestone");
	}

	@Test
	void whenProjectVersionIsReleaseThenTypeIsRelease() {
		Project project = ProjectBuilder.builder().build();
		project.setVersion("1.2.3");
		assertThat(ArtifactRelease.forProject(project).getType()).isEqualTo("release");
	}

	@Test
	void whenProjectVersionIsSnapshotThenRepositoryIsArtifactorySnapshot() {
		Project project = ProjectBuilder.builder().build();
		project.setVersion("1.2.3-SNAPSHOT");
		assertThat(ArtifactRelease.forProject(project).getDownloadRepo()).contains("repo.spring.io/snapshot");
	}

	@Test
	void whenProjectVersionIsMilestoneThenRepositoryIsMavenCentral() {
		Project project = ProjectBuilder.builder().build();
		project.setVersion("4.0.0-M1");
		assertThat(ArtifactRelease.forProject(project).getDownloadRepo())
			.contains("https://repo.maven.apache.org/maven2");
	}

	@Test
	void whenProjectVersionIsReleaseCandidateThenRepositoryIsMavenCentral() {
		Project project = ProjectBuilder.builder().build();
		project.setVersion("4.0.0-RC1");
		assertThat(ArtifactRelease.forProject(project).getDownloadRepo())
			.contains("https://repo.maven.apache.org/maven2");
	}

	@Test
	void whenProjectVersionIsReleaseThenRepositoryIsMavenCentral() {
		Project project = ProjectBuilder.builder().build();
		project.setVersion("1.2.3");
		assertThat(ArtifactRelease.forProject(project).getDownloadRepo())
			.contains("https://repo.maven.apache.org/maven2");
	}

}
