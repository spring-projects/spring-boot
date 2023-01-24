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
	void whenProjectVersionIsMilestoneThenRepositoryIsArtifactoryMilestone() {
		Project project = ProjectBuilder.builder().build();
		project.setVersion("1.2.3-M1");
		assertThat(ArtifactRelease.forProject(project).getDownloadRepo()).contains("repo.spring.io/milestone");
	}

	@Test
	void whenProjectVersionIsReleaseCandidateThenRepositoryIsArtifactoryMilestone() {
		Project project = ProjectBuilder.builder().build();
		project.setVersion("1.2.3-RC1");
		assertThat(ArtifactRelease.forProject(project).getDownloadRepo()).contains("repo.spring.io/milestone");
	}

	@Test
	void whenProjectVersionIsReleaseThenRepositoryIsMavenCentral() {
		Project project = ProjectBuilder.builder().build();
		project.setVersion("1.2.3");
		assertThat(ArtifactRelease.forProject(project).getDownloadRepo())
				.contains("https://repo.maven.apache.org/maven2");
	}

}
