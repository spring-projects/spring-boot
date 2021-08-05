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

package org.springframework.boot.maven;

import java.time.Duration;

import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.UpdateListener;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Maven plugin's image support using a Docker image registry.
 *
 * @author Scott Frederick
 */
@ExtendWith(MavenBuildExtension.class)
@Testcontainers(disabledWithoutDocker = true)
@Disabled("Disabled until differences between running locally and in CI can be diagnosed")
public class BuildImageRegistryIntegrationTests extends AbstractArchiveIntegrationTests {

	@Container
	static final RegistryContainer registry = new RegistryContainer().withStartupAttempts(5)
			.withStartupTimeout(Duration.ofMinutes(3));

	DockerClient dockerClient;

	String registryAddress;

	@BeforeEach
	void setUp() {
		assertThat(registry.isRunning()).isTrue();
		this.dockerClient = registry.getDockerClient();
		this.registryAddress = registry.getHost() + ":" + registry.getFirstMappedPort();
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithPublish(MavenBuild mavenBuild) {
		String repoName = "test-image";
		String imageName = this.registryAddress + "/" + repoName;
		mavenBuild.project("build-image-publish").goals("package")
				.systemProperty("spring-boot.build-image.imageName", imageName).execute((project) -> {
					assertThat(buildLog(project)).contains("Building image").contains("Successfully built image")
							.contains("Pushing image '" + imageName + ":latest" + "'")
							.contains("Pushed image '" + imageName + ":latest" + "'");
					ImageReference imageReference = ImageReference.of(imageName);
					DockerApi.ImageApi imageApi = new DockerApi().image();
					Image pulledImage = imageApi.pull(imageReference, UpdateListener.none());
					assertThat(pulledImage).isNotNull();
					imageApi.remove(imageReference, false);
				});
	}

	private static class RegistryContainer extends GenericContainer<RegistryContainer> {

		RegistryContainer() {
			super(DockerImageNames.registry());
			addExposedPorts(5000);
			addEnv("SERVER_NAME", "localhost");
		}

	}

}
