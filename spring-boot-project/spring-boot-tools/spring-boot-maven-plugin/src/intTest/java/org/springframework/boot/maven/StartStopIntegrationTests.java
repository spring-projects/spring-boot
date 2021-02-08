/*
 * Copyright 2012-2020 the original author or authors.
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
	void startStopWithForkDisabledWaitsForApplicationToBeReadyAndThenRequestsShutdown(MavenBuild mavenBuild) {
		mavenBuild.project("start-stop-fork-disabled").goals("verify").execute(
				(project) -> assertThat(buildLog(project)).contains("isReady: true").contains("Shutdown requested"));
	}

	@TestTemplate
	void startStopWaitsForApplicationToBeReadyAndThenRequestsShutdown(MavenBuild mavenBuild) {
		mavenBuild.project("start-stop").goals("verify").execute(
				(project) -> assertThat(buildLog(project)).contains("isReady: true").contains("Shutdown requested"));
	}

	@TestTemplate
	void whenSkipIsTrueStartAndStopAreSkipped(MavenBuild mavenBuild) {
		mavenBuild.project("start-stop-skip").goals("verify").execute((project) -> assertThat(buildLog(project))
				.doesNotContain("Ooops, I haz been run").doesNotContain("Stopping application"));
	}

	private String buildLog(File project) {
		return contentOf(new File(project, "target/build.log"));
	}

}
