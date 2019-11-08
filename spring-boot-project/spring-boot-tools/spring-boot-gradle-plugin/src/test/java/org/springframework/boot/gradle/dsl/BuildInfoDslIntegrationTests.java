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

package org.springframework.boot.gradle.dsl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.gradle.tasks.buildinfo.BuildInfo;
import org.springframework.boot.gradle.testkit.GradleBuild;
import org.springframework.boot.gradle.testkit.GradleBuildExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BuildInfo} created using the
 * {@link org.springframework.boot.gradle.dsl.SpringBootExtension DSL}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(GradleBuildExtension.class)
class BuildInfoDslIntegrationTests {

	final GradleBuild gradleBuild = new GradleBuild();

	@Test
	void basicJar() throws IOException {
		assertThat(this.gradleBuild.build("bootBuildInfo", "--stacktrace").task(":bootBuildInfo").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		Properties properties = buildInfoProperties();
		assertThat(properties).containsEntry("build.name", this.gradleBuild.getProjectDir().getName());
		assertThat(properties).containsEntry("build.artifact", this.gradleBuild.getProjectDir().getName());
		assertThat(properties).containsEntry("build.group", "com.example");
		assertThat(properties).containsEntry("build.version", "1.0");
	}

	@Test
	void jarWithCustomName() throws IOException {
		assertThat(this.gradleBuild.build("bootBuildInfo", "--stacktrace").task(":bootBuildInfo").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		Properties properties = buildInfoProperties();
		assertThat(properties).containsEntry("build.name", this.gradleBuild.getProjectDir().getName());
		assertThat(properties).containsEntry("build.artifact", "foo");
		assertThat(properties).containsEntry("build.group", "com.example");
		assertThat(properties).containsEntry("build.version", "1.0");
	}

	@Test
	void basicWar() throws IOException {
		assertThat(this.gradleBuild.build("bootBuildInfo", "--stacktrace").task(":bootBuildInfo").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		Properties properties = buildInfoProperties();
		assertThat(properties).containsEntry("build.name", this.gradleBuild.getProjectDir().getName());
		assertThat(properties).containsEntry("build.artifact", this.gradleBuild.getProjectDir().getName());
		assertThat(properties).containsEntry("build.group", "com.example");
		assertThat(properties).containsEntry("build.version", "1.0");
	}

	@Test
	void warWithCustomName() throws IOException {
		assertThat(this.gradleBuild.build("bootBuildInfo", "--stacktrace").task(":bootBuildInfo").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		Properties properties = buildInfoProperties();
		assertThat(properties).containsEntry("build.name", this.gradleBuild.getProjectDir().getName());
		assertThat(properties).containsEntry("build.artifact", "foo");
		assertThat(properties).containsEntry("build.group", "com.example");
		assertThat(properties).containsEntry("build.version", "1.0");
	}

	@Test
	void additionalProperties() throws IOException {
		assertThat(this.gradleBuild.build("bootBuildInfo", "--stacktrace").task(":bootBuildInfo").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		Properties properties = buildInfoProperties();
		assertThat(properties).containsEntry("build.name", this.gradleBuild.getProjectDir().getName());
		assertThat(properties).containsEntry("build.artifact", this.gradleBuild.getProjectDir().getName());
		assertThat(properties).containsEntry("build.group", "com.example");
		assertThat(properties).containsEntry("build.version", "1.0");
		assertThat(properties).containsEntry("build.a", "alpha");
		assertThat(properties).containsEntry("build.b", "bravo");
	}

	@Test
	void classesDependency() throws IOException {
		assertThat(this.gradleBuild.build("classes", "--stacktrace").task(":bootBuildInfo").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
	}

	private Properties buildInfoProperties() {
		File file = new File(this.gradleBuild.getProjectDir(), "build/resources/main/META-INF/build-info.properties");
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
