/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.maven;

import java.io.File;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * Integration tests for the Maven plugin's war support.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(MavenBuildExtension.class)
class StartStopIntegrationTests {

	@TestTemplate
	void startStopWaitsForApplicationToBeReadyAndThenRequestsShutdown(MavenBuild mavenBuild) {
		testStartStop(mavenBuild, "start-stop");

	}

	@TestTemplate
	void startStopWithTestClasspathWaitsForApplicationToBeReadyAndThenRequestsShutdown(MavenBuild mavenBuild) {
		testStartStop(mavenBuild, "start-stop-test");
	}

	private void testStartStop(MavenBuild mavenBuild, String projectId) {
		mavenBuild.project(projectId)
			.goals("verify")
			.execute((project) -> assertThat(buildLog(project)).contains("isReady: true")
				.contains("Shutdown requested"));
	}

	@TestTemplate
	void whenSkipIsTrueStartAndStopAreSkipped(MavenBuild mavenBuild) {
		mavenBuild.project("start-stop-skip")
			.goals("verify")
			.execute((project) -> assertThat(buildLog(project)).doesNotContain("Ooops, I haz been run")
				.doesNotContain("Stopping application"));
	}

	private String buildLog(File project) {
		return contentOf(new File(project, "target/build.log"));
	}

}
