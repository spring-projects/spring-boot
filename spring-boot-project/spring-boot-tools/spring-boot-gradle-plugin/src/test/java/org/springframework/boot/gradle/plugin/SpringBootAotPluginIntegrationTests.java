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

import org.gradle.testkit.runner.TaskOutcome;
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
	void noProcessAotTaskWithoutAotPluginApplied() {
		assertThat(this.gradleBuild.build("taskExists", "-PtaskName=processAot").getOutput())
				.contains("processAot exists = false");
	}

	@TestTemplate
	void noProcessTestAotTaskWithoutAotPluginApplied() {
		assertThat(this.gradleBuild.build("taskExists", "-PtaskName=processTestAot").getOutput())
				.contains("processTestAot exists = false");
	}

	@TestTemplate
	void applyingAotPluginCreatesProcessAotTask() {
		assertThat(this.gradleBuild.build("taskExists", "-PtaskName=processAot").getOutput())
				.contains("processAot exists = true");
	}

	@TestTemplate
	void applyingAotPluginCreatesProcessTestAotTask() {
		assertThat(this.gradleBuild.build("taskExists", "-PtaskName=processTestAot").getOutput())
				.contains("processTestAot exists = true");
	}

	@TestTemplate
	void processAotHasLibraryResourcesOnItsClasspath() throws IOException {
		File settings = new File(this.gradleBuild.getProjectDir(), "settings.gradle");
		Files.write(settings.toPath(), List.of("include 'library'"));
		File library = new File(this.gradleBuild.getProjectDir(), "library");
		library.mkdirs();
		Files.write(library.toPath().resolve("build.gradle"), List.of("plugins {", "    id 'java-library'", "}"));
		assertThat(this.gradleBuild.build("processAotClasspath").getOutput()).contains("library.jar");
	}

	@TestTemplate
	void processTestAotHasLibraryResourcesOnItsClasspath() throws IOException {
		File settings = new File(this.gradleBuild.getProjectDir(), "settings.gradle");
		Files.write(settings.toPath(), List.of("include 'library'"));
		File library = new File(this.gradleBuild.getProjectDir(), "library");
		library.mkdirs();
		Files.write(library.toPath().resolve("build.gradle"), List.of("plugins {", "    id 'java-library'", "}"));
		assertThat(this.gradleBuild.build("processTestAotClasspath").getOutput()).contains("library.jar");
	}

	@TestTemplate
	void processAotHasTransitiveRuntimeDependenciesOnItsClasspath() {
		String output = this.gradleBuild.build("processAotClasspath").getOutput();
		assertThat(output).contains("org.jboss.logging" + File.separatorChar + "jboss-logging");
	}

	@TestTemplate
	void processTestAotHasTransitiveRuntimeDependenciesOnItsClasspath() {
		String output = this.gradleBuild.build("processTestAotClasspath").getOutput();
		assertThat(output).contains("org.jboss.logging" + File.separatorChar + "jboss-logging");
	}

	@TestTemplate
	void processAotRunsWhenProjectHasMainSource() throws IOException {
		writeMainClass("org.springframework.boot", "SpringApplicationAotProcessor");
		writeMainClass("com.example", "Main");
		assertThat(this.gradleBuild.build("processAot").task(":processAot").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void processTestAotIsSkippedWhenProjectHasNoTestSource() {
		assertThat(this.gradleBuild.build("processTestAot").task(":processTestAot").getOutcome())
				.isEqualTo(TaskOutcome.NO_SOURCE);
	}

	private void writeMainClass(String packageName, String className) throws IOException {
		File java = new File(this.gradleBuild.getProjectDir(),
				"src/main/java/" + packageName.replace(".", "/") + "/" + className + ".java");
		java.getParentFile().mkdirs();
		Files.writeString(java.toPath(), """
				package %s;

				public class %s {

					public static void main(String[] args) {

					}

				}
				""".formatted(packageName, className));
	}

}
