/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.gradle.plugin;

import org.gradle.testkit.runner.BuildResult;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SpringBootPlugin}.
 *
 * @author Andy Wilkinson
 */
public class SpringBootPluginIntegrationTests {

	@Rule
	public final GradleBuild gradleBuild = new GradleBuild();

	@Test
	public void failFastWithVersionOfGradleLowerThanRequired() {
		BuildResult result = this.gradleBuild.gradleVersion("3.5.1").buildAndFail();
		assertThat(result.getOutput()).contains("Spring Boot plugin requires Gradle 4.0"
				+ " or later. The current version is Gradle 3.5.1");
	}

	@Test
	public void succeedWithVersionOfGradleHigherThanRequired() {
		this.gradleBuild.gradleVersion("4.0.1").build();
	}

	@Test
	public void succeedWithVersionOfGradleMatchingWhatIsRequired() {
		this.gradleBuild.gradleVersion("4.0").build();
	}

}
