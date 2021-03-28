/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.gradle.tasks.buildinfo;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Properties;

import org.gradle.testkit.runner.InvalidRunnerConfigurationException;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.junit.jupiter.api.TestTemplate;

import org.springframework.boot.gradle.junit.GradleCompatibility;
import org.springframework.boot.gradle.testkit.GradleBuild;
import org.springframework.boot.loader.tools.FileUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the {@link BuildInfo} task.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility(configurationCache = true)
class BuildInfoIntegrationTests {

	GradleBuild gradleBuild;

	@TestTemplate
	void defaultValues() {
		assertThat(this.gradleBuild.build("buildInfo").task(":buildInfo").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		Properties buildInfoProperties = buildInfoProperties();
		assertThat(buildInfoProperties).containsKey("build.time");
		assertThat(buildInfoProperties).containsEntry("build.artifact", "unspecified");
		assertThat(buildInfoProperties).containsEntry("build.group", "");
		assertThat(buildInfoProperties).containsEntry("build.name", this.gradleBuild.getProjectDir().getName());
		assertThat(buildInfoProperties).containsEntry("build.version", "unspecified");
	}

	@TestTemplate
	void basicExecution() {
		assertThat(this.gradleBuild.build("buildInfo").task(":buildInfo").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		Properties buildInfoProperties = buildInfoProperties();
		assertThat(buildInfoProperties).containsKey("build.time");
		assertThat(buildInfoProperties).containsEntry("build.artifact", "foo");
		assertThat(buildInfoProperties).containsEntry("build.group", "foo");
		assertThat(buildInfoProperties).containsEntry("build.additional", "foo");
		assertThat(buildInfoProperties).containsEntry("build.name", "foo");
		assertThat(buildInfoProperties).containsEntry("build.version", "0.1.0");
	}

	@TestTemplate
	void notUpToDateWhenExecutedTwiceAsTimeChanges() {
		assertThat(this.gradleBuild.build("buildInfo").task(":buildInfo").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		Properties first = buildInfoProperties();
		String firstBuildTime = first.getProperty("build.time");
		assertThat(firstBuildTime).isNotNull();
		assertThat(this.gradleBuild.build("buildInfo").task(":buildInfo").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		Properties second = buildInfoProperties();
		String secondBuildTime = second.getProperty("build.time");
		assertThat(secondBuildTime).isNotNull();
		assertThat(Instant.parse(firstBuildTime)).isBefore(Instant.parse(secondBuildTime));
	}

	@TestTemplate
	void upToDateWhenExecutedTwiceWithFixedTime() {
		assertThat(this.gradleBuild.build("buildInfo", "-PnullTime").task(":buildInfo").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build("buildInfo", "-PnullTime").task(":buildInfo").getOutcome())
				.isEqualTo(TaskOutcome.UP_TO_DATE);
	}

	@TestTemplate
	void notUpToDateWhenExecutedTwiceWithFixedTimeAndChangedProjectVersion() throws IOException {
		assertThat(this.gradleBuild.scriptProperty("projectVersion", "0.1.0").build("buildInfo").task(":buildInfo")
				.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.scriptProperty("projectVersion", "0.2.0").build("buildInfo").task(":buildInfo")
				.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void notUpToDateWhenExecutedTwiceWithFixedTimeAndChangedGradlePropertiesProjectVersion() throws IOException {
		Path gradleProperties = new File(this.gradleBuild.getProjectDir(), "gradle.properties").toPath();
		Files.write(gradleProperties, "version=0.1.0".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		assertThat(this.gradleBuild.build("buildInfo").task(":buildInfo").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		Files.write(gradleProperties, "version=0.2.0".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		assertThat(this.gradleBuild.build("buildInfo").task(":buildInfo").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void reproducibleOutputWithFixedTime()
			throws InvalidRunnerConfigurationException, UnexpectedBuildFailure, IOException, InterruptedException {
		assertThat(this.gradleBuild.build("buildInfo", "-PnullTime").task(":buildInfo").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		File buildInfoProperties = new File(this.gradleBuild.getProjectDir(), "build/build-info.properties");
		String firstHash = FileUtils.sha1Hash(buildInfoProperties);
		assertThat(buildInfoProperties.delete()).isTrue();
		Thread.sleep(1500);
		assertThat(this.gradleBuild.build("buildInfo", "-PnullTime").task(":buildInfo").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		String secondHash = FileUtils.sha1Hash(buildInfoProperties);
		assertThat(firstHash).isEqualTo(secondHash);
	}

	private Properties buildInfoProperties() {
		File file = new File(this.gradleBuild.getProjectDir(), "build/build-info.properties");
		assertThat(file).isFile();
		Properties properties = new Properties();
		try (FileReader reader = new FileReader(file)) {
			properties.load(reader);
			return properties;
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
