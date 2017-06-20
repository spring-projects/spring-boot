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
 * Integration tests for {@link WarPluginAction}.
 *
 * @author Andy Wilkinson
 */
@RunWith(GradleCompatibilitySuite.class)
public class JavaPluginActionIntegrationTests {

	@Rule
	public GradleBuild gradleBuild;

	@Test
	public void noBootJarTaskWithoutJavaPluginApplied() {
		assertThat(this.gradleBuild.build("taskExists", "-PtaskName=bootJar").getOutput())
				.contains("bootJar exists = false");
	}

	@Test
	public void applyingJavaPluginCreatesBootJarTask() {
		assertThat(this.gradleBuild
				.build("taskExists", "-PtaskName=bootJar", "-PapplyJavaPlugin")
				.getOutput()).contains("bootJar exists = true");
	}

	@Test
	public void noBootRunTaskWithoutJavaPluginApplied() {
		assertThat(this.gradleBuild.build("taskExists", "-PtaskName=bootRun").getOutput())
				.contains("bootRun exists = false");
	}

	@Test
	public void applyingJavaPluginCreatesBootRunTask() {
		assertThat(this.gradleBuild
				.build("taskExists", "-PtaskName=bootRun", "-PapplyJavaPlugin")
				.getOutput()).contains("bootRun exists = true");
	}

	@Test
	public void javaCompileTasksUseUtf8Encoding() {
		assertThat(this.gradleBuild.build("javaCompileEncoding", "-PapplyJavaPlugin")
				.getOutput()).contains("compileJava = UTF-8")
						.contains("compileTestJava = UTF-8");
	}

	@Test
	public void assembleRunsBootJarAndJarIsSkipped() {
		BuildResult result = this.gradleBuild.build("assemble");
		assertThat(result.task(":bootJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.task(":jar").getOutcome()).isEqualTo(TaskOutcome.SKIPPED);
	}

	@Test
	public void jarAndBootJarCanBothBeBuilt() {
		BuildResult result = this.gradleBuild.build("assemble");
		assertThat(result.task(":bootJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.task(":jar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		File buildLibs = new File(this.gradleBuild.getProjectDir(), "build/libs");
		assertThat(buildLibs.listFiles()).containsExactlyInAnyOrder(
				new File(buildLibs, this.gradleBuild.getProjectDir().getName() + ".jar"),
				new File(buildLibs,
						this.gradleBuild.getProjectDir().getName() + "-boot.jar"));
	}

}
