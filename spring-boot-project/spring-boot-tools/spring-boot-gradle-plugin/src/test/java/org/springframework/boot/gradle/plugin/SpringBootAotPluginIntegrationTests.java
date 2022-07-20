/*
 * Copyright 2012-2022 the original author or authors.
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
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.TestTemplate;

import org.springframework.boot.gradle.junit.GradleCompatibility;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SpringBootAotPlugin}.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility
class SpringBootAotPluginIntegrationTests {

	GradleBuild gradleBuild;

	@TestTemplate
	void noGenerateAotSourcesTaskWithoutAotPluginApplied() {
		assertThat(this.gradleBuild.build("taskExists", "-PtaskName=generateAotSources").getOutput())
				.contains("generateAotSources exists = false");
	}

	@TestTemplate
	void applyingAotPluginCreatesGenerateAotSourcesTask() {
		assertThat(this.gradleBuild.build("taskExists", "-PtaskName=generateAotSources").getOutput())
				.contains("generateAotSources exists = true");
	}

	@TestTemplate
	void generateAotSourcesHasLibraryResourcesOnItsClasspath() throws IOException {
		File settings = new File(this.gradleBuild.getProjectDir(), "settings.gradle");
		Files.write(settings.toPath(), List.of("include 'library'"));
		File library = new File(this.gradleBuild.getProjectDir(), "library");
		library.mkdirs();
		Files.write(library.toPath().resolve("build.gradle"), List.of("plugins {", "    id 'java-library'", "}"));
		assertThat(this.gradleBuild.build("generateAotSourcesClasspath").getOutput()).contains("library.jar");
	}

}
