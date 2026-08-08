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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.jar.JarFile;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.TestTemplate;

import org.springframework.boot.gradle.junit.GradleCompatibility;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Gradle's isolated projects feature.
 *
 * @author Greg Taube
 */
@GradleCompatibility(minimumVersion = "9.6")
class IsolatedProjectsIntegrationTests {

	@SuppressWarnings("NullAway.Init")
	GradleBuild gradleBuild;

	@TestTemplate
	void bootJarUsesProjectDependencyCoordinates() throws IOException {
		writeProjectFiles();
		this.gradleBuild.configurationCache();
		BuildResult result = this.gradleBuild.build("bootJar", "-Dorg.gradle.unsafe.isolated-projects=true");
		BuildTask task = result.task(":bootJar");
		assertThat(task).isNotNull();
		assertThat(task.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		File archive = new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0];
		try (JarFile jarFile = new JarFile(archive)) {
			String layersIndex = new String(
					jarFile.getInputStream(jarFile.getEntry("BOOT-INF/layers.idx")).readAllBytes(),
					StandardCharsets.UTF_8);
			assertThat(layersIndex).contains("- \"project-dependencies\":\n  - \"BOOT-INF/lib/library-1.2.3.jar\"");
		}
		result = this.gradleBuild.build("bootJar", "-Dorg.gradle.unsafe.isolated-projects=true");
		assertThat(result.getOutput()).contains("Reusing configuration cache.");
		task = result.task(":bootJar");
		assertThat(task).isNotNull();
		assertThat(task.getOutcome()).isEqualTo(TaskOutcome.UP_TO_DATE);
	}

	private void writeProjectFiles() throws IOException {
		Files.writeString(new File(this.gradleBuild.getProjectDir(), "settings.gradle").toPath(),
				"include 'library'\n");
		File library = new File(this.gradleBuild.getProjectDir(), "library");
		library.mkdirs();
		Files.writeString(new File(library, "build.gradle").toPath(), """
				plugins {
					id 'java'
				}
				group = 'org.example.projects'
				version = '1.2.3'
				""");
	}

}
