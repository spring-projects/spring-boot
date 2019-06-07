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

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.gradle.junit.GradleCompatibilitySuite;
import org.springframework.boot.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JavaPluginAction}.
 *
 * @author Andy Wilkinson
 */
@RunWith(GradleCompatibilitySuite.class)
public class WarPluginActionIntegrationTests {

	@Rule
	public GradleBuild gradleBuild;

	@Test
	public void noBootWarTaskWithoutWarPluginApplied() {
		assertThat(this.gradleBuild.build("taskExists", "-PtaskName=bootWar").getOutput())
				.contains("bootWar exists = false");
	}

	@Test
	public void applyingWarPluginCreatesBootWarTask() {
		assertThat(this.gradleBuild.build("taskExists", "-PtaskName=bootWar", "-PapplyWarPlugin").getOutput())
				.contains("bootWar exists = true");
	}

	@Test
	public void assembleRunsBootWarAndWarIsSkipped() {
		BuildResult result = this.gradleBuild.build("assemble");
		assertThat(result.task(":bootWar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.task(":war").getOutcome()).isEqualTo(TaskOutcome.SKIPPED);
	}

	@Test
	public void warAndBootWarCanBothBeBuilt() {
		BuildResult result = this.gradleBuild.build("assemble");
		assertThat(result.task(":bootWar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.task(":war").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		File buildLibs = new File(this.gradleBuild.getProjectDir(), "build/libs");
		assertThat(buildLibs.listFiles()).containsExactlyInAnyOrder(
				new File(buildLibs, this.gradleBuild.getProjectDir().getName() + ".war"),
				new File(buildLibs, this.gradleBuild.getProjectDir().getName() + "-boot.war"));
	}

	@Test
	public void errorMessageIsHelpfulWhenMainClassCannotBeResolved() {
		BuildResult result = this.gradleBuild.buildAndFail("build", "-PapplyWarPlugin");
		assertThat(result.task(":bootWar").getOutcome()).isEqualTo(TaskOutcome.FAILED);
		assertThat(result.getOutput()).contains("Main class name has not been configured and it could not be resolved");
	}

}
