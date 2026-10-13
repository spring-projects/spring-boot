/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.plugin;

import org.gradle.testkit.runner.BuildTask;
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

	@SuppressWarnings("NullAway.Init")
	GradleBuild gradleBuild;

	@TestTemplate
	void noDependencyManagementIsAppliedByDefault() {
		BuildTask task = this.gradleBuild.build("doesNotHaveDependencyManagement")
			.task(":doesNotHaveDependencyManagement");
		assertThat(task).isNotNull();
		assertThat(task.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void bomIsImportedWhenDependencyManagementPluginIsApplied() {
		BuildTask task = this.gradleBuild.build("hasDependencyManagement", "-PapplyDependencyManagementPlugin")
			.task(":hasDependencyManagement");
		assertThat(task).isNotNull();
		assertThat(task.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

}
