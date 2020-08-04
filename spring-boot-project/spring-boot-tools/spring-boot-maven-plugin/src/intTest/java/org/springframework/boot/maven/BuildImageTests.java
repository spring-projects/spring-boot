/*
 * Copyright 2012-2020 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.type.ImageName;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.testsupport.testcontainers.DisabledIfDockerUnavailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Maven plugin's image support.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
@ExtendWith(MavenBuildExtension.class)
@DisabledIfDockerUnavailable
public class BuildImageTests extends AbstractArchiveIntegrationTests {

	@TestTemplate
	void whenBuildImageIsInvokedWithoutRepackageTheArchiveIsRepackagedOnTheFly(MavenBuild mavenBuild) {
		mavenBuild.project("build-image").goals("package").prepare(this::writeLongNameResource).execute((project) -> {
			File jar = new File(project, "target/build-image-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar).isFile();
			File original = new File(project, "target/build-image-0.0.1.BUILD-SNAPSHOT.jar.original");
			assertThat(original).doesNotExist();
			assertThat(buildLog(project)).contains("Building image").contains("paketo-buildpacks/builder")
					.contains("docker.io/library/build-image:0.0.1.BUILD-SNAPSHOT")
					.contains("Successfully built image");
			ImageReference imageReference = ImageReference.of(ImageName.of("build-image"), "0.0.1.BUILD-SNAPSHOT");
			try (GenericContainer<?> container = new GenericContainer<>(imageReference.toString())) {
				container.waitingFor(Wait.forLogMessage("Launched\\n", 1)).start();
			}
			finally {
				removeImage(imageReference);
			}
		});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithCustomImageName(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-custom-name").goals("package")
				.systemProperty("spring-boot.build-image.imageName", "example.com/test/property-ignored:pom-preferred")
				.execute((project) -> {
					File jar = new File(project, "target/build-image-custom-name-0.0.1.BUILD-SNAPSHOT.jar");
					assertThat(jar).isFile();
					File original = new File(project,
							"target/build-image-custom-name-0.0.1.BUILD-SNAPSHOT.jar.original");
					assertThat(original).doesNotExist();
					assertThat(buildLog(project)).contains("Building image")
							.contains("example.com/test/build-image:0.0.1.BUILD-SNAPSHOT")
							.contains("Successfully built image");
					ImageReference imageReference = ImageReference
							.of("example.com/test/build-image:0.0.1.BUILD-SNAPSHOT");
					try (GenericContainer<?> container = new GenericContainer<>(imageReference.toString())) {
						container.waitingFor(Wait.forLogMessage("Launched\\n", 1)).start();
					}
					finally {
						removeImage(imageReference);
					}
				});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithCommandLineParameters(MavenBuild mavenBuild) {
		mavenBuild.project("build-image").goals("package")
				.systemProperty("spring-boot.build-image.imageName", "example.com/test/cmd-property-name:v1")
				.systemProperty("spring-boot.build-image.builder",
						"gcr.io/paketo-buildpacks/builder:full-cf-platform-api-0.3")
				.systemProperty("spring-boot.build-image.runImage", "gcr.io/paketo-buildpacks/run:full-cnb-cf")
				.execute((project) -> {
					assertThat(buildLog(project)).contains("Building image")
							.contains("example.com/test/cmd-property-name:v1")
							.contains("paketo-buildpacks/builder:full-cf-platform-api-0.3")
							.contains("paketo-buildpacks/run:full-cnb-cf").contains("Successfully built image");
					ImageReference imageReference = ImageReference.of("example.com/test/cmd-property-name:v1");
					try (GenericContainer<?> container = new GenericContainer<>(imageReference.toString())) {
						container.waitingFor(Wait.forLogMessage("Launched\\n", 1)).start();
					}
					finally {
						removeImage(imageReference);
					}
				});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithCustomBuilderImageAndRunImage(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-custom-builder").goals("package").execute((project) -> {
			assertThat(buildLog(project)).contains("Building image")
					.contains("paketo-buildpacks/builder:full-cf-platform-api-0.3")
					.contains("paketo-buildpacks/run:full-cnb-cf")
					.contains("docker.io/library/build-image-v2-builder:0.0.1.BUILD-SNAPSHOT")
					.contains("Successfully built image");
			ImageReference imageReference = ImageReference
					.of("docker.io/library/build-image-v2-builder:0.0.1.BUILD-SNAPSHOT");
			try (GenericContainer<?> container = new GenericContainer<>(imageReference.toString())) {
				container.waitingFor(Wait.forLogMessage("Launched\\n", 1)).start();
			}
			finally {
				removeImage(imageReference);
			}
		});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithEmptyEnvEntry(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-empty-env-entry").goals("package").prepare(this::writeLongNameResource)
				.execute((project) -> {
					assertThat(buildLog(project)).contains("Building image").contains("paketo-buildpacks/builder")
							.contains("docker.io/library/build-image-empty-env-entry:0.0.1.BUILD-SNAPSHOT")
							.contains("Successfully built image");
					ImageReference imageReference = ImageReference.of(ImageName.of("build-image-empty-env-entry"),
							"0.0.1.BUILD-SNAPSHOT");
					try (GenericContainer<?> container = new GenericContainer<>(imageReference.toString())) {
						container.waitingFor(Wait.forLogMessage("Launched\\n", 1)).start();
					}
					finally {
						removeImage(imageReference);
					}
				});
	}

	@TestTemplate
	void failsWhenBuilderFails(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-builder-error").goals("package")
				.executeAndFail((project) -> assertThat(buildLog(project)).contains("Building image")
						.containsPattern("Builder lifecycle '.*' failed with status code"));
	}

	private void writeLongNameResource(File project) {
		StringBuilder name = new StringBuilder();
		new Random().ints('a', 'z' + 1).limit(128).forEach((i) -> name.append((char) i));
		try {
			Path path = project.toPath().resolve(Paths.get("src", "main", "resources", name.toString()));
			Files.createDirectories(path.getParent());
			Files.createFile(path);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void removeImage(ImageReference imageReference) {
		try {
			new DockerApi().image().remove(imageReference, false);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to remove docker image " + imageReference, ex);
		}
	}

}
