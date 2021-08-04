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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.TestTemplate;

import org.springframework.boot.gradle.junit.GradleCompatibility;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for publishing Boot jars and wars using Gradle's Maven Publish
 * plugin.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility
class MavenPublishingIntegrationTests {

	GradleBuild gradleBuild;

	@TestTemplate
	void bootJarCanBePublished() {
		BuildResult result = this.gradleBuild.build("publish");
		assertThat(result.task(":publish").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(artifactWithSuffix("jar")).isFile();
		assertThat(artifactWithSuffix("pom")).is(pomWith().groupId("com.example")
				.artifactId(this.gradleBuild.getProjectDir().getName()).version("1.0").noPackaging().noDependencies());
	}

	@TestTemplate
	void bootWarCanBePublished() {
		BuildResult result = this.gradleBuild.build("publish");
		assertThat(result.task(":publish").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(artifactWithSuffix("war")).isFile();
		assertThat(artifactWithSuffix("pom"))
				.is(pomWith().groupId("com.example").artifactId(this.gradleBuild.getProjectDir().getName())
						.version("1.0").packaging("war").noDependencies());
	}

	private File artifactWithSuffix(String suffix) {
		String name = this.gradleBuild.getProjectDir().getName();
		return new File(new File(this.gradleBuild.getProjectDir(), "build/repo"),
				String.format("com/example/%s/1.0/%s-1.0.%s", name, name, suffix));
	}

	private PomCondition pomWith() {
		return new PomCondition();
	}

}
