/*
 * Copyright 2012-2024 the original author or authors.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.gradle.testkit.PluginClasspathGradleBuild;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuildExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link KotlinPluginAction}.
 *
 * @author Andy Wilkinson
 */
@DisabledForJreRange(min = JRE.JAVA_20)
@ExtendWith(GradleBuildExtension.class)
class KotlinPluginActionIntegrationTests {

	GradleBuild gradleBuild = new PluginClasspathGradleBuild();

	@Test
	void noKotlinVersionPropertyWithoutKotlinPlugin() {
		assertThat(this.gradleBuild.build("kotlinVersion").getOutput()).contains("Kotlin version: none");
	}

	@Test
	void kotlinVersionPropertyIsSet() {
		String output = this.gradleBuild.build("kotlinVersion", "dependencies", "--configuration", "compileClasspath")
			.getOutput();
		assertThat(output).containsPattern("Kotlin version: [0-9]\\.[0-9]\\.[0-9]+");
	}

	@Test
	void kotlinCompileTasksUseJavaParametersFlagByDefault() {
		assertThat(this.gradleBuild.build("kotlinCompileTasksJavaParameters").getOutput())
			.contains("compileKotlin java parameters: true")
			.contains("compileTestKotlin java parameters: true");
	}

	@Test
	void kotlinCompileTasksCanOverrideDefaultJavaParametersFlag() {
		assertThat(this.gradleBuild.build("kotlinCompileTasksJavaParameters").getOutput())
			.contains("compileKotlin java parameters: false")
			.contains("compileTestKotlin java parameters: false");
	}

	@Test
	void taskConfigurationIsAvoided() throws IOException {
		BuildResult result = this.gradleBuild.build("help");
		String output = result.getOutput();
		BufferedReader reader = new BufferedReader(new StringReader(output));
		String line;
		Set<String> configured = new HashSet<>();
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("Configuring :")) {
				configured.add(line.substring("Configuring :".length()));
			}
		}
		assertThat(configured).containsExactlyInAnyOrder("help", "compileJava", "clean");
	}

}
