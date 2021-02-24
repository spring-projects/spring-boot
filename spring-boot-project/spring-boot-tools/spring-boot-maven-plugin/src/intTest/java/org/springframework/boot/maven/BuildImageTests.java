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
		mavenBuild.project("build-image").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
				.prepare(this::writeLongNameResource).execute((project) -> {
					File jar = new File(project, "target/build-image-0.0.1.BUILD-SNAPSHOT.jar");
					assertThat(jar).isFile();
					File original = new File(project, "target/build-image-0.0.1.BUILD-SNAPSHOT.jar.original");
					assertThat(original).doesNotExist();
					assertThat(buildLog(project)).contains("Building image")
							.contains("docker.io/library/build-image:0.0.1.BUILD-SNAPSHOT")
							.contains("Successfully built image");
					ImageReference imageReference = ImageReference.of(ImageName.of("build-image"),
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
	void whenBuildImageIsInvokedWithWarPackaging(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-war-packaging").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
				.prepare(this::writeLongNameResource).execute((project) -> {
					File war = new File(project, "target/build-image-war-packaging-0.0.1.BUILD-SNAPSHOT.war");
					assertThat(war).isFile();
					File original = new File(project,
							"target/build-image-war-packaging-0.0.1.BUILD-SNAPSHOT.war.original");
					assertThat(original).doesNotExist();
					assertThat(buildLog(project)).contains("Building image")
							.contains("docker.io/library/build-image-war-packaging:0.0.1.BUILD-SNAPSHOT")
							.contains("Successfully built image");
					ImageReference imageReference = ImageReference.of(ImageName.of("build-image-war-packaging"),
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
	void whenBuildImageIsInvokedWithCustomImageName(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-custom-name").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
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
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
				.systemProperty("spring-boot.build-image.imageName", "example.com/test/cmd-property-name:v1")
				.systemProperty("spring-boot.build-image.builder", "paketobuildpacks/builder:full")
				.systemProperty("spring-boot.build-image.runImage", "paketobuildpacks/run:full-cnb")
				.execute((project) -> {
					assertThat(buildLog(project)).contains("Building image")
							.contains("example.com/test/cmd-property-name:v1").contains("Successfully built image");
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
		mavenBuild.project("build-image-custom-builder").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT").execute((project) -> {
					assertThat(buildLog(project)).contains("Building image")
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
		mavenBuild.project("build-image-empty-env-entry").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
				.prepare(this::writeLongNameResource).execute((project) -> {
					assertThat(buildLog(project)).contains("Building image")
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
	void whenBuildImageIsInvokedWithBuildpacks(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-custom-buildpacks").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT").execute((project) -> {
					assertThat(buildLog(project)).contains("Building image")
							.contains("docker.io/library/build-image-custom-buildpacks:0.0.1.BUILD-SNAPSHOT")
							.contains("Successfully built image");
					ImageReference imageReference = ImageReference
							.of("docker.io/library/build-image-custom-buildpacks:0.0.1.BUILD-SNAPSHOT");
					try (GenericContainer<?> container = new GenericContainer<>(imageReference.toString())) {
						container.waitingFor(Wait.forLogMessage("Launched\\n", 1)).start();
					}
					finally {
						removeImage(imageReference);
					}
				});
	}

	@TestTemplate
	void failsWithBindingContainingInvalidCertificate(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-bindings").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
				.executeAndFail((project) -> assertThat(buildLog(project)).contains("Building image")
						.contains("failed to decode certificate")
						.contains("/platform/bindings/ca-certificates/test.crt"));
	}

	@TestTemplate
	void failsWhenPublishWithoutPublishRegistryConfigured(MavenBuild mavenBuild) {
		mavenBuild.project("build-image").goals("package").systemProperty("spring-boot.build-image.publish", "true")
				.executeAndFail((project) -> assertThat(buildLog(project)).contains("requires docker.publishRegistry"));
	}

	@TestTemplate
	void failsWhenBuilderFails(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-builder-error").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
				.executeAndFail((project) -> assertThat(buildLog(project)).contains("Building image")
						.containsPattern("Builder lifecycle '.*' failed with status code"));
	}

	@TestTemplate
	void failsWithBuildpackNotInBuilder(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-bad-buildpack").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
				.executeAndFail((project) -> assertThat(buildLog(project))
						.contains("'urn:cnb:builder:example/does-not-exist:0.0.1' not found in builder"));
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
