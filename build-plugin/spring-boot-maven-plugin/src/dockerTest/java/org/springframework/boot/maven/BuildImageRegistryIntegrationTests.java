/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.maven;

import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.UpdateListener;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.testsupport.container.RegistryContainer;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Maven plugin's image support using a Docker image registry.
 *
 * @author Scott Frederick
 */
@ExtendWith(MavenBuildExtension.class)
@Testcontainers(disabledWithoutDocker = true)
@Disabled("Disabled until differences between running locally and in CI can be diagnosed")
class BuildImageRegistryIntegrationTests extends AbstractArchiveIntegrationTests {

	@Container
	static final RegistryContainer registry = TestImage.forContainer(RegistryContainer.class);

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
		mavenBuild.project("dockerTest", "build-image-publish")
			.goals("package")
			.systemProperty("spring-boot.build-image.imageName", imageName)
			.execute((project) -> {
				assertThat(buildLog(project)).contains("Building image")
					.contains("Successfully built image")
					.contains("Pushing image '" + imageName + ":latest" + "'")
					.contains("Pushed image '" + imageName + ":latest" + "'");
				ImageReference imageReference = ImageReference.of(imageName);
				DockerApi.ImageApi imageApi = new DockerApi().image();
				Image pulledImage = imageApi.pull(imageReference, null, UpdateListener.none());
				assertThat(pulledImage).isNotNull();
				imageApi.remove(imageReference, false);
			});
	}

}
