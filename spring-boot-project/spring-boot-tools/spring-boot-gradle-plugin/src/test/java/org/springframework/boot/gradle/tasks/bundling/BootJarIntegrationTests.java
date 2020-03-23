/*
 * Copyright 2012-2020 the original author or authors.
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;

import org.gradle.testkit.runner.InvalidRunnerConfigurationException;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.junit.jupiter.api.TestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BootJar}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class BootJarIntegrationTests extends AbstractBootArchiveIntegrationTests {

	BootJarIntegrationTests() {
		super("bootJar");
	}

	@TestTemplate
	void upToDateWhenBuiltTwiceWithLayers() throws InvalidRunnerConfigurationException, UnexpectedBuildFailure {
		assertThat(this.gradleBuild.build("-Playered=true", "bootJar").task(":bootJar").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build("-Playered=true", "bootJar").task(":bootJar").getOutcome())
				.isEqualTo(TaskOutcome.UP_TO_DATE);
	}

	@TestTemplate
	void notUpToDateWhenBuiltWithoutLayersAndThenWithLayers()
			throws InvalidRunnerConfigurationException, UnexpectedBuildFailure {
		assertThat(this.gradleBuild.build("bootJar").task(":bootJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build("-Playered=true", "bootJar").task(":bootJar").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void notUpToDateWhenBuiltWithLayersAndToolsAndThenWithLayersAndWithoutTools()
			throws InvalidRunnerConfigurationException, UnexpectedBuildFailure {
		assertThat(this.gradleBuild.build("-Playered=true", "bootJar").task(":bootJar").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build("-Playered=true", "-PexcludeTools=true", "bootJar").task(":bootJar")
				.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void implicitLayers() throws IOException {
		writeMainClass();
		writeResource();
		assertThat(this.gradleBuild.build("bootJar").task(":bootJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		try (JarFile jarFile = new JarFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0])) {
			assertThat(jarFile.getEntry("BOOT-INF/layers/dependencies/lib/spring-boot-jarmode-layertools.jar"))
					.isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/layers/dependencies/lib/commons-lang3-3.9.jar")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/layers/snapshot-dependencies/lib/commons-io-2.7-SNAPSHOT.jar"))
					.isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/layers/application/classes/example/Main.class")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/layers/application/classes/static/file.txt")).isNotNull();
		}
	}

	@TestTemplate
	void customLayers() throws IOException {
		writeMainClass();
		writeResource();
		assertThat(this.gradleBuild.build("bootJar").task(":bootJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		try (JarFile jarFile = new JarFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0])) {
			assertThat(jarFile.getEntry("BOOT-INF/layers/dependencies/lib/spring-boot-jarmode-layertools.jar"))
					.isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/layers/commons-dependencies/lib/commons-lang3-3.9.jar")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/layers/snapshot-dependencies/lib/commons-io-2.7-SNAPSHOT.jar"))
					.isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/layers/app/classes/example/Main.class")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/layers/static/classes/static/file.txt")).isNotNull();
		}
	}

	private void writeMainClass() {
		File examplePackage = new File(this.gradleBuild.getProjectDir(), "src/main/java/example");
		examplePackage.mkdirs();
		File main = new File(examplePackage, "Main.java");
		try (PrintWriter writer = new PrintWriter(new FileWriter(main))) {
			writer.println("package example;");
			writer.println();
			writer.println("import java.io.IOException;");
			writer.println();
			writer.println("public class Main {");
			writer.println();
			writer.println("    public static void main(String[] args) {");
			writer.println("    }");
			writer.println();
			writer.println("}");
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void writeResource() {
		try {
			Path path = this.gradleBuild.getProjectDir().toPath()
					.resolve(Paths.get("src", "main", "resources", "static", "file.txt"));
			Files.createDirectories(path.getParent());
			Files.createFile(path);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
