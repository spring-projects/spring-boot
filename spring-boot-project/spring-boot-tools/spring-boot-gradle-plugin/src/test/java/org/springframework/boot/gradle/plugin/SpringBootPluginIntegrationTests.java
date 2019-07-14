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

import java.io.File;
import java.io.IOException;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
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

	@Test
	void failFastWithVersionOfGradleLowerThanRequired() {
		BuildResult result = this.gradleBuild.gradleVersion("4.9").buildAndFail();
		assertThat(result.getOutput())
				.contains("Spring Boot plugin requires Gradle 4.10 or later. The current version is Gradle 4.9");
	}

	@Test
	void succeedWithVersionOfGradleHigherThanRequired() {
		this.gradleBuild.gradleVersion("4.10.1").build();
	}

	@Test
	void succeedWithVersionOfGradleMatchingWhatIsRequired() {
		this.gradleBuild.gradleVersion("4.10").build();
	}

	@Test
	void unresolvedDependenciesAreAnalyzedWhenDependencyResolutionFails() throws IOException {
		createMinimalMainSource();
		BuildResult result = this.gradleBuild.buildAndFail("compileJava");
		assertThat(result.getOutput())
				.contains("During the build, one or more dependencies that were declared without a"
						+ " version failed to resolve:")
				.contains("    org.springframework.boot:spring-boot-starter:");
	}

	private void createMinimalMainSource() throws IOException {
		File examplePackage = new File(this.gradleBuild.getProjectDir(), "src/main/java/com/example");
		examplePackage.mkdirs();
		new File(examplePackage, "Application.java").createNewFile();
	}

}
