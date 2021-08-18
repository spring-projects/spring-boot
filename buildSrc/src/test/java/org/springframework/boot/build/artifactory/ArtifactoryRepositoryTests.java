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

package org.springframework.boot.build.artifactory;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ArtifactoryRepository}.
 *
 * @author Andy Wilkinson
 */
class ArtifactoryRepositoryTests {

	@Test
	void whenProjectVersionIsMilestoneThenRepositoryIsMilestone() {
		Project project = ProjectBuilder.builder().build();
		project.setVersion("1.2.3-M1");
		assertThat(ArtifactoryRepository.forProject(project).getName()).isEqualTo("milestone");
	}

	@Test
	void whenProjectVersionIsReleaseCandidateThenRepositoryIsMilestone() {
		Project project = ProjectBuilder.builder().build();
		project.setVersion("1.2.3-RC1");
		assertThat(ArtifactoryRepository.forProject(project).getName()).isEqualTo("milestone");
	}

	@Test
	void whenProjectVersionIsReleaseThenRepositoryIsRelease() {
		Project project = ProjectBuilder.builder().build();
		project.setVersion("1.2.3");
		assertThat(ArtifactoryRepository.forProject(project).getName()).isEqualTo("release");
	}

	@Test
	void whenProjectVersionIsSnapshotThenRepositoryIsSnapshot() {
		Project project = ProjectBuilder.builder().build();
		project.setVersion("1.2.3-SNAPSHOT");
		assertThat(ArtifactoryRepository.forProject(project).getName()).isEqualTo("snapshot");
	}

}
