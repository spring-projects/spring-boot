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

package org.springframework.boot.gradle.plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.TestTemplate;

import org.springframework.boot.gradle.junit.GradleCompatibility;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CyclonedxPluginAction}.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility
class CyclonedxPluginActionIntegrationTests {

	@SuppressWarnings("NullAway.Init")
	GradleBuild gradleBuild;

	@TestTemplate
	void sbomIsIncludedInUberJar() throws IOException {
		sbomIsIncludedInUberArchive("bootJar", "");
	}

	@TestTemplate
	void sbomIsIncludedInUberWar() throws IOException {
		sbomIsIncludedInUberArchive("bootWar", "WEB-INF/classes/");
	}

	private void sbomIsIncludedInUberArchive(String taskName, String sbomLocationPrefix) throws IOException {
		BuildResult result = this.gradleBuild.expectDeprecationWarningsWithAtLeastVersion("7.6.6").build(taskName);
		BuildTask task = result.task(":cyclonedxBom");
		assertThat(task).isNotNull().extracting(BuildTask::getOutcome).isEqualTo(TaskOutcome.SUCCESS);
		File[] libs = new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles();
		assertThat(libs).hasSize(1);
		try (JarFile jar = new JarFile(libs[0])) {
			assertThat(jar.getManifest().getMainAttributes().getValue("Sbom-Format")).isEqualTo("CycloneDX");
			String sbomLocation = jar.getManifest().getMainAttributes().getValue("Sbom-Location");
			assertThat(sbomLocation).isEqualTo(sbomLocationPrefix + "META-INF/sbom/application.cdx.json");
			List<String> entryNames = jar.stream().map(JarEntry::getName).toList();
			assertThat(entryNames).contains(sbomLocation);
		}
	}

}
