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
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.gradle.junit.GradleCompatibilityExtension;
import org.springframework.boot.gradle.testkit.GradleBuild;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the configuration applied by
 * {@link DependencyManagementPluginAction}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(GradleCompatibilityExtension.class)
class DependencyManagementPluginActionIntegrationTests {

	GradleBuild gradleBuild;

	@TestTemplate
	void noDependencyManagementIsAppliedByDefault() {
		assertThat(this.gradleBuild.build("doesNotHaveDependencyManagement").task(":doesNotHaveDependencyManagement")
				.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void bomIsImportedWhenDependencyManagementPluginIsApplied() {
		assertThat(this.gradleBuild.build("hasDependencyManagement", "-PapplyDependencyManagementPlugin")
				.task(":hasDependencyManagement").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void helpfulErrorWhenVersionlessDependencyFailsToResolve() throws IOException {
		File examplePackage = new File(this.gradleBuild.getProjectDir(), "src/main/java/com/example");
		examplePackage.mkdirs();
		FileSystemUtils.copyRecursively(new File("src/test/java/com/example"), examplePackage);
		BuildResult result = this.gradleBuild.buildAndFail("compileJava");
		assertThat(result.task(":compileJava").getOutcome()).isEqualTo(TaskOutcome.FAILED);
		String output = result.getOutput();
		assertThat(output).contains("During the build, one or more dependencies that "
				+ "were declared without a version failed to resolve:");
		assertThat(output).contains("org.springframework.boot:spring-boot-starter-web:");
		assertThat(output).contains("Did you forget to apply the io.spring.dependency-management plugin to the "
				+ this.gradleBuild.getProjectDir().getName() + " project?");
	}

}
