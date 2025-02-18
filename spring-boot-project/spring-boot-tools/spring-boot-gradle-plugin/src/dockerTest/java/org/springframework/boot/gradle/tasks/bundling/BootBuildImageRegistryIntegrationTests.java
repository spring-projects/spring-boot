/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.UpdateListener;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.gradle.junit.GradleCompatibility;
import org.springframework.boot.testsupport.container.RegistryContainer;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BootBuildImage} tasks requiring a Docker image registry.
 *
 * @author Scott Frederick
 */
@GradleCompatibility
@Testcontainers(disabledWithoutDocker = true)
@Disabled("Disabled until differences between running locally and in CI can be diagnosed")
class BootBuildImageRegistryIntegrationTests {

	@Container
	static final RegistryContainer registry = TestImage.container(RegistryContainer.class);

	String registryAddress;

	GradleBuild gradleBuild;

	@BeforeEach
	void setUp() {
		assertThat(registry.isRunning()).isTrue();
		this.registryAddress = registry.getHost() + ":" + registry.getFirstMappedPort();
	}

	@TestTemplate
	void buildsImageAndPublishesToRegistry() throws IOException {
		writeMainClass();
		String repoName = "test-image";
		String imageName = this.registryAddress + "/" + repoName;
		BuildResult result = this.gradleBuild.build("bootBuildImage", "--imageName=" + imageName);
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("Building image")
			.contains("Successfully built image")
			.contains("Pushing image '" + imageName + ":latest" + "'")
			.contains("Pushed image '" + imageName + ":latest" + "'");
		ImageReference imageReference = ImageReference.of(imageName);
		Image pulledImage = new DockerApi().image().pull(imageReference, null, UpdateListener.none());
		assertThat(pulledImage).isNotNull();
		new DockerApi().image().remove(imageReference, false);
	}

	private void writeMainClass() {
		File examplePackage = new File(this.gradleBuild.getProjectDir(), "src/main/java/example");
		examplePackage.mkdirs();
		File main = new File(examplePackage, "Main.java");
		try (PrintWriter writer = new PrintWriter(new FileWriter(main))) {
			writer.println("package example;");
			writer.println();
			writer.println("import java.io.IOException;");
			writer.println();
			writer.println("public class Main {");
			writer.println();
			writer.println("    public static void main(String[] args) {");
			writer.println("    }");
			writer.println();
			writer.println("}");
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
