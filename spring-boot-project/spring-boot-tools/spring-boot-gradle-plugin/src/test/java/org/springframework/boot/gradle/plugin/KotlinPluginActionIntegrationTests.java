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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.TestTemplate;

import org.springframework.boot.gradle.junit.GradleCompatibility;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link KotlinPluginAction}.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility
class KotlinPluginActionIntegrationTests {

	GradleBuild gradleBuild;

	@TestTemplate
	void noKotlinVersionPropertyWithoutKotlinPlugin() {
		assertThat(this.gradleBuild.build("kotlinVersion").getOutput()).contains("Kotlin version: none");
	}

	@TestTemplate
	void kotlinVersionPropertyIsSet() {
		String output = this.gradleBuild.build("kotlinVersion", "dependencies", "--configuration", "compileClasspath")
				.getOutput();
		assertThat(output).containsPattern("Kotlin version: [0-9]\\.[0-9]\\.[0-9]+");
	}

	@TestTemplate
	void kotlinCompileTasksUseJavaParametersFlagByDefault() {
		assertThat(this.gradleBuild.build("kotlinCompileTasksJavaParameters").getOutput())
				.contains("compileKotlin java parameters: true").contains("compileTestKotlin java parameters: true");
	}

	@TestTemplate
	void kotlinCompileTasksCanOverrideDefaultJavaParametersFlag() {
		assertThat(this.gradleBuild.build("kotlinCompileTasksJavaParameters").getOutput())
				.contains("compileKotlin java parameters: false").contains("compileTestKotlin java parameters: false");
	}

	@TestTemplate
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
		if (GradleVersion.version(this.gradleBuild.getGradleVersion()).compareTo(GradleVersion.version("7.3.3")) < 0) {
			assertThat(configured).containsExactly("help");
		}
		else {
			assertThat(configured).containsExactlyInAnyOrder("help", "clean", "compileKotlin", "compileTestKotlin");
		}
	}

}
