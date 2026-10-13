/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.plugin;

import org.junit.jupiter.api.TestTemplate;

import org.springframework.boot.gradle.junit.GradleCompatibility;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for configuring a project to only use Spring Boot's dependency
 * management.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility
class OnlyDependencyManagementIntegrationTests {

	@SuppressWarnings("NullAway.Init")
	GradleBuild gradleBuild;

	@TestTemplate
	void dependencyManagementCanBeConfiguredUsingCoordinatesConstant() {
		assertThat(this.gradleBuild.build("dependencyManagement").getOutput())
			.contains("org.springframework.boot:spring-boot-starter ");
	}

}
