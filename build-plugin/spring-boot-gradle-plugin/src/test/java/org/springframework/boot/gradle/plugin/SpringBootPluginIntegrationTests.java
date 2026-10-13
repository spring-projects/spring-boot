/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.plugin;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.gradle.testkit.PluginClasspathGradleBuild;
import org.springframework.boot.testsupport.BuildOutput;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuildExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SpringBootPlugin}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(GradleBuildExtension.class)
class SpringBootPluginIntegrationTests {

	final GradleBuild gradleBuild = new PluginClasspathGradleBuild(new BuildOutput(getClass()));

	@Test
	@DisabledForJreRange(min = JRE.JAVA_24)
	void failFastWithVersionOfGradle8LowerThanRequired() {
		BuildResult result = this.gradleBuild.gradleVersion("8.13").buildAndFail();
		assertThat(result.getOutput()).contains(
				"Spring Boot plugin requires Gradle 8.x (8.14 or later) or 9.x. The current version is Gradle 8.13");
	}

}
