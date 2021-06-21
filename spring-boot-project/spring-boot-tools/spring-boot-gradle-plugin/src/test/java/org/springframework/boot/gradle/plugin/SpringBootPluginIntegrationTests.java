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

package org.springframework.boot.gradle.plugin;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.gradle.testkit.GradleBuild;
import org.springframework.boot.gradle.testkit.GradleBuildExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SpringBootPlugin}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(GradleBuildExtension.class)
class SpringBootPluginIntegrationTests {

	final GradleBuild gradleBuild = new GradleBuild();

	@DisabledForJreRange(min = JRE.JAVA_14)
	@Test
	void failFastWithVersionOfGradle6LowerThanRequired() {
		BuildResult result = this.gradleBuild.gradleVersion("6.7.1").buildAndFail();
		assertThat(result.getOutput()).contains(
				"Spring Boot plugin requires Gradle 6.8.x, 6.9.x, or 7.x. The current version is Gradle 6.7.1");
	}

	@DisabledForJreRange(min = JRE.JAVA_16)
	@Test
	void succeedWithVersionOfGradle6MatchingWithIsRequired() {
		this.gradleBuild.gradleVersion("6.8").build();
	}

}
