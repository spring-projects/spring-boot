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

package org.springframework.boot.gradle.docs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.gradle.junit.GradleMultiDslExtension;
import org.springframework.boot.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the documentation about running a Spring Boot application.
 *
 * @author Andy Wilkinson
 * @author Jean-Baptiste Nizet
 */
@ExtendWith(GradleMultiDslExtension.class)
class RunningDocumentationTests {

	GradleBuild gradleBuild;

	@TestTemplate
	@DisabledForJreRange(min = JRE.JAVA_13)
	void bootRunMain() throws IOException {
		writeMainClass();
		assertThat(this.gradleBuild.script("src/docs/gradle/running/boot-run-main").build("bootRun").getOutput())
				.contains("com.example.ExampleApplication");
	}

	@TestTemplate
	void applicationPluginMainClassName() throws IOException {
		writeMainClass();
		assertThat(this.gradleBuild.script("src/docs/gradle/running/application-plugin-main-class-name")
				.build("bootRun").getOutput()).contains("com.example.ExampleApplication");
	}

	@TestTemplate
	void springBootDslMainClassName() throws IOException {
		writeMainClass();
		assertThat(this.gradleBuild.script("src/docs/gradle/running/spring-boot-dsl-main-class-name").build("bootRun")
				.getOutput()).contains("com.example.ExampleApplication");
	}

	@TestTemplate
	void bootRunSourceResources() {
		assertThat(this.gradleBuild.script("src/docs/gradle/running/boot-run-source-resources")
				.build("configuredClasspath").getOutput()).contains(new File("src/main/resources").getPath());
	}

	@TestTemplate
	void bootRunDisableOptimizedLaunch() {
		assertThat(this.gradleBuild.script("src/docs/gradle/running/boot-run-disable-optimized-launch")
				.build("optimizedLaunch").getOutput()).contains("false");
	}

	@TestTemplate
	void bootRunSystemPropertyDefaultValue() {
		assertThat(this.gradleBuild.script("src/docs/gradle/running/boot-run-system-property")
				.build("configuredSystemProperties").getOutput()).contains("com.example.property = default");
	}

	@TestTemplate
	void bootRunSystemPropetry() {
		assertThat(this.gradleBuild.script("src/docs/gradle/running/boot-run-system-property")
				.build("-Pexample=custom", "configuredSystemProperties").getOutput())
						.contains("com.example.property = custom");
	}

	private void writeMainClass() throws IOException {
		File exampleApplication = new File(this.gradleBuild.getProjectDir(),
				"src/main/java/com/example/ExampleApplication.java");
		exampleApplication.getParentFile().mkdirs();
		try (PrintWriter writer = new PrintWriter(new FileWriter(exampleApplication))) {
			writer.println("package com.example;");
			writer.println("public class ExampleApplication {");
			writer.println("    public static void main(String[] args) {");
			writer.println("        System.out.println(ExampleApplication.class.getName());");
			writer.println("    }");
			writer.println("}");
		}
	}

}
