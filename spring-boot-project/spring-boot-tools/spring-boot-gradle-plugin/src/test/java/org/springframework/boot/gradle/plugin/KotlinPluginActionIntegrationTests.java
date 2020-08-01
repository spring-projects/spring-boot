/*
 * Copyright 2012-2019 the original author or authors.
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

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.gradle.junit.GradleCompatibilityExtension;
import org.springframework.boot.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link KotlinPluginAction}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(GradleCompatibilityExtension.class)
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

}
