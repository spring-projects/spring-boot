/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle.docs;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the documentation about running a Spring Boot application.
 *
 * @author Andy Wilkinson
 */
public class RunningDocumentationTests {

	@Rule
	public GradleBuild gradleBuild = new GradleBuild();

	@Test
	public void bootRunMain() throws IOException {
		assertThat(this.gradleBuild.script("src/main/gradle/running/boot-run-main.gradle")
				.build("configuredMainClass").getOutput())
						.contains("com.example.ExampleApplication");
	}

	@Test
	public void applicationPluginMainClassName() throws IOException {
		assertThat(this.gradleBuild.script(
				"src/main/gradle/running/application-plugin-main-class-name.gradle")
				.build("configuredMainClass").getOutput())
						.contains("com.example.ExampleApplication");
	}

	@Test
	public void springBootDslMainClassName() throws IOException {
		assertThat(this.gradleBuild
				.script("src/main/gradle/running/spring-boot-dsl-main-class-name.gradle")
				.build("configuredMainClass").getOutput())
						.contains("com.example.ExampleApplication");
	}

	@Test
	public void bootRunSourceResources() throws IOException {
		assertThat(this.gradleBuild
				.script("src/main/gradle/running/boot-run-source-resources.gradle")
				.build("configuredClasspath").getOutput())
						.contains(new File("src/main/resources").getPath());
	}

}
