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

package org.springframework.boot.build.testing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integrations tests for {@link TestFailuresPlugin}.
 *
 * @author Andy Wilkinson
 */
class TestFailuresPluginIntegrationTests {

	private File projectDir;

	@BeforeEach
	void setup(@TempDir File projectDir) throws IOException {
		this.projectDir = projectDir;
	}

	@Test
	void singleProject() throws IOException {
		createProject(this.projectDir);
		BuildResult result = GradleRunner.create().withDebug(true).withProjectDir(this.projectDir)
				.withArguments("build").withPluginClasspath().buildAndFail();
		assertThat(readLines(result.getOutput())).containsSequence("Found test failures in 1 test task:", "", ":test",
				"    example.ExampleTests > bad()", "    example.ExampleTests > fail()",
				"    example.MoreTests > bad()", "    example.MoreTests > fail()", "");
	}

	@Test
	void multiProject() throws IOException {
		createMultiProjectBuild();
		BuildResult result = GradleRunner.create().withDebug(true).withProjectDir(this.projectDir)
				.withArguments("build").withPluginClasspath().buildAndFail();
		assertThat(readLines(result.getOutput())).containsSequence("Found test failures in 1 test task:", "",
				":project-one:test", "    example.ExampleTests > bad()", "    example.ExampleTests > fail()",
				"    example.MoreTests > bad()", "    example.MoreTests > fail()", "");
	}

	@Test
	void multiProjectContinue() throws IOException {
		createMultiProjectBuild();
		BuildResult result = GradleRunner.create().withDebug(true).withProjectDir(this.projectDir)
				.withArguments("build", "--continue").withPluginClasspath().buildAndFail();
		assertThat(readLines(result.getOutput())).containsSequence("Found test failures in 2 test tasks:", "",
				":project-one:test", "    example.ExampleTests > bad()", "    example.ExampleTests > fail()",
				"    example.MoreTests > bad()", "    example.MoreTests > fail()", "", ":project-two:test",
				"    example.ExampleTests > bad()", "    example.ExampleTests > fail()",
				"    example.MoreTests > bad()", "    example.MoreTests > fail()", "");
	}

	@Test
	void multiProjectParallel() throws IOException {
		createMultiProjectBuild();
		BuildResult result = GradleRunner.create().withDebug(true).withProjectDir(this.projectDir)
				.withArguments("build", "--parallel").withPluginClasspath().buildAndFail();
		assertThat(readLines(result.getOutput())).containsSequence("Found test failures in 2 test tasks:", "",
				":project-one:test", "    example.ExampleTests > bad()", "    example.ExampleTests > fail()",
				"    example.MoreTests > bad()", "    example.MoreTests > fail()", "", ":project-two:test",
				"    example.ExampleTests > bad()", "    example.ExampleTests > fail()",
				"    example.MoreTests > bad()", "    example.MoreTests > fail()", "");
	}

	private void createProject(File dir) {
		File examplePackage = new File(dir, "src/test/java/example");
		examplePackage.mkdirs();
		createTestSource("ExampleTests", examplePackage);
		createTestSource("MoreTests", examplePackage);
		createBuildScript(dir);
	}

	private void createMultiProjectBuild() {
		createProject(new File(this.projectDir, "project-one"));
		createProject(new File(this.projectDir, "project-two"));
		withPrintWriter(new File(this.projectDir, "settings.gradle"), (writer) -> {
			writer.println("include 'project-one'");
			writer.println("include 'project-two'");
		});
	}

	private void createTestSource(String name, File dir) {
		withPrintWriter(new File(dir, name + ".java"), (writer) -> {
			writer.println("package example;");
			writer.println();
			writer.println("import org.junit.jupiter.api.Test;");
			writer.println();
			writer.println("import static org.assertj.core.api.Assertions.assertThat;");
			writer.println();
			writer.println("class " + name + "{");
			writer.println();
			writer.println("	@Test");
			writer.println("	void fail() {");
			writer.println("		assertThat(true).isFalse();");
			writer.println("	}");
			writer.println();
			writer.println("	@Test");
			writer.println("	void bad() {");
			writer.println("		assertThat(5).isLessThan(4);");
			writer.println("	}");
			writer.println();
			writer.println("	@Test");
			writer.println("	void ok() {");
			writer.println("	}");
			writer.println();
			writer.println("}");
		});
	}

	private void createBuildScript(File dir) {
		withPrintWriter(new File(dir, "build.gradle"), (writer) -> {
			writer.println("plugins {");
			writer.println("	id 'java'");
			writer.println("	id 'org.springframework.boot.test-failures'");
			writer.println("}");
			writer.println();
			writer.println("repositories {");
			writer.println("	mavenCentral()");
			writer.println("}");
			writer.println();
			writer.println("dependencies {");
			writer.println("	testImplementation 'org.junit.jupiter:junit-jupiter:5.6.0'");
			writer.println("	testImplementation 'org.assertj:assertj-core:3.11.1'");
			writer.println("}");
			writer.println();
			writer.println("test {");
			writer.println("	useJUnitPlatform()");
			writer.println("}");
		});
	}

	private void withPrintWriter(File file, Consumer<PrintWriter> consumer) {
		try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
			consumer.accept(writer);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private List<String> readLines(String output) {
		try (BufferedReader reader = new BufferedReader(new StringReader(output))) {
			return reader.lines().collect(Collectors.toList());
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
