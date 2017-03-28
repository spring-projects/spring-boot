/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle.buildinfo;

import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BuildInfo}.
 *
 * @author Andy Wilkinson
 */
public class BuildInfoIntegrationTests {

	@Rule
	public final GradleBuild gradleBuild = new GradleBuild();

	@Test
	public void basicExecution() {
		assertThat(this.gradleBuild.build("buildInfo").task(":buildInfo").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
	}

	@Test
	public void upToDateWhenExecutedTwice() {
		assertThat(this.gradleBuild.build("buildInfo").task(":buildInfo").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build("buildInfo").task(":buildInfo").getOutcome())
				.isEqualTo(TaskOutcome.UP_TO_DATE);
	}

	@Test
	public void notUpToDateWhenDestinationDirChanges() {
		notUpToDateWithChangeToProperty("buildInfoDestinationDir");
	}

	@Test
	public void notUpToDateWhenProjectArtifactChanges() {
		notUpToDateWithChangeToProperty("buildInfoProjectArtifact");
	}

	@Test
	public void notUpToDateWhenProjectGroupChanges() {
		notUpToDateWithChangeToProperty("buildInfoProjectGroup");
	}

	@Test
	public void notUpToDateWhenProjectVersionChanges() {
		notUpToDateWithChangeToProperty("buildInfoProjectVersion");
	}

	@Test
	public void notUpToDateWhenProjectNameChanges() {
		notUpToDateWithChangeToProperty("buildInfoProjectName");
	}

	@Test
	public void notUpToDateWhenAdditionalPropertyChanges() {
		notUpToDateWithChangeToProperty("buildInfoAdditionalProperty");
	}

	private void notUpToDateWithChangeToProperty(String name) {
		assertThat(this.gradleBuild.build("buildInfo").task(":buildInfo").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build("buildInfo", "-P" + name + "=changed")
				.task(":buildInfo").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

}
