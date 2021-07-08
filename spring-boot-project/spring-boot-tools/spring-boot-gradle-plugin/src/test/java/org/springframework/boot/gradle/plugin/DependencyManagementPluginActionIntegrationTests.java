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

import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.TestTemplate;

import org.springframework.boot.gradle.junit.GradleCompatibility;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the configuration applied by
 * {@link DependencyManagementPluginAction}.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility
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

}
