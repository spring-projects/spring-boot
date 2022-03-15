/*
 * Copyright 2012-2022 the original author or authors.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.TestTemplate;

import org.springframework.boot.gradle.junit.GradleCompatibility;
import org.springframework.boot.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JavaPluginAction}.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility
class WarPluginActionIntegrationTests {

	GradleBuild gradleBuild;

	@TestTemplate
	void noBootWarTaskWithoutWarPluginApplied() {
		assertThat(this.gradleBuild.build("taskExists", "-PtaskName=bootWar").getOutput())
				.contains("bootWar exists = false");
	}

	@TestTemplate
	void applyingWarPluginCreatesBootWarTask() {
		assertThat(this.gradleBuild.build("taskExists", "-PtaskName=bootWar", "-PapplyWarPlugin").getOutput())
				.contains("bootWar exists = true");
	}

	@TestTemplate
	void assembleRunsBootWarAndWar() {
		BuildResult result = this.gradleBuild.build("assemble");
		assertThat(result.task(":bootWar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.task(":war").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		File buildLibs = new File(this.gradleBuild.getProjectDir(), "build/libs");
		assertThat(buildLibs.listFiles()).containsExactlyInAnyOrder(
				new File(buildLibs, this.gradleBuild.getProjectDir().getName() + ".war"),
				new File(buildLibs, this.gradleBuild.getProjectDir().getName() + "-plain.war"));
	}

	@TestTemplate
	void errorMessageIsHelpfulWhenMainClassCannotBeResolved() {
		BuildResult result = this.gradleBuild.buildAndFail("build", "-PapplyWarPlugin");
		assertThat(result.task(":bootWar").getOutcome()).isEqualTo(TaskOutcome.FAILED);
		assertThat(result.getOutput()).contains("Main class name has not been configured and it could not be resolved");
	}

	@TestTemplate
	void taskConfigurationIsAvoided() throws IOException {
		BuildResult result = this.gradleBuild.build("help");
		String output = result.getOutput();
		BufferedReader reader = new BufferedReader(new StringReader(output));
		String line;
		Set<String> configured = new HashSet<>();
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("Configuring :")) {
				configured.add(line.substring("Configuring :".length()));
			}
		}
		if (GradleVersion.version(this.gradleBuild.getGradleVersion()).compareTo(GradleVersion.version("7.3.3")) < 0) {
			assertThat(configured).containsExactly("help");
		}
		else {
			assertThat(configured).containsExactlyInAnyOrder("help", "clean");
		}
	}

}
