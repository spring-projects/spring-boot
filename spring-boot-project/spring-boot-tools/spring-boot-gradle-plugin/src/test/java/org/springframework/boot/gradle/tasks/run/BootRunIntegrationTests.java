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

package org.springframework.boot.gradle.tasks.run;

import java.io.File;
import java.io.IOException;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.gradle.junit.GradleCompatibilitySuite;
import org.springframework.boot.gradle.testkit.GradleBuild;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the {@link BootRun} task.
 *
 * @author Andy Wilkinson
 */
@RunWith(GradleCompatibilitySuite.class)
public class BootRunIntegrationTests {

	@Rule
	public GradleBuild gradleBuild;

	@Test
	public void basicExecution() throws IOException {
		copyApplication();
		new File(this.gradleBuild.getProjectDir(), "src/main/resources").mkdirs();
		BuildResult result = this.gradleBuild.build("bootRun");
		assertThat(result.task(":bootRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput())
				.contains("1. " + canonicalPathOf("build/classes/java/main"));
		assertThat(result.getOutput())
				.contains("2. " + canonicalPathOf("build/resources/main"));
		assertThat(result.getOutput())
				.doesNotContain(canonicalPathOf("src/main/resources"));
	}

	@Test
	public void sourceResourcesCanBeUsed() throws IOException {
		copyApplication();
		BuildResult result = this.gradleBuild.build("bootRun");
		assertThat(result.task(":bootRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput())
				.contains("1. " + canonicalPathOf("src/main/resources"));
		assertThat(result.getOutput())
				.contains("2. " + canonicalPathOf("build/classes/java/main"));
		assertThat(result.getOutput())
				.doesNotContain(canonicalPathOf("build/resources/main"));
	}

	@Test
	public void springBootExtensionMainClassNameIsUsed() throws IOException {
		BuildResult result = this.gradleBuild.build("echoMainClassName");
		assertThat(result.task(":echoMainClassName").getOutcome())
				.isEqualTo(TaskOutcome.UP_TO_DATE);
		assertThat(result.getOutput())
				.contains("Main class name = com.example.CustomMainClass");
	}

	@Test
	public void applicationPluginMainClassNameIsUsed() throws IOException {
		BuildResult result = this.gradleBuild.build("echoMainClassName");
		assertThat(result.task(":echoMainClassName").getOutcome())
				.isEqualTo(TaskOutcome.UP_TO_DATE);
		assertThat(result.getOutput())
				.contains("Main class name = com.example.CustomMainClass");
	}

	@Test
	public void applicationPluginMainClassNameIsNotUsedWhenItIsNull() throws IOException {
		copyApplication();
		BuildResult result = this.gradleBuild.build("echoMainClassName");
		assertThat(result.task(":echoMainClassName").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput())
				.contains("Main class name = com.example.BootRunApplication");
	}

	@Test
	public void applicationPluginJvmArgumentsAreUsed() throws IOException {
		BuildResult result = this.gradleBuild.build("echoJvmArguments");
		assertThat(result.task(":echoJvmArguments").getOutcome())
				.isEqualTo(TaskOutcome.UP_TO_DATE);
		assertThat(result.getOutput())
				.contains("JVM arguments = [-Dcom.foo=bar, -Dcom.bar=baz]");
	}

	private void copyApplication() throws IOException {
		File output = new File(this.gradleBuild.getProjectDir(),
				"src/main/java/com/example");
		output.mkdirs();
		FileSystemUtils.copyRecursively(new File("src/test/java/com/example"), output);
	}

	private String canonicalPathOf(String path) throws IOException {
		return new File(this.gradleBuild.getProjectDir(), path).getCanonicalPath();
	}

}
