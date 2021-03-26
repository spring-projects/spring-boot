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
	void whenBuildImageIsInvokedWithClassifierWithoutRepackageTheArchiveIsRepackagedOnTheFly(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-classifier").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
				.prepare(this::writeLongNameResource).execute((project) -> {
					File jar = new File(project, "target/build-image-classifier-0.0.1.BUILD-SNAPSHOT.jar");
					assertThat(jar).isFile();
					File classifier = new File(project, "target/build-image-classifier-0.0.1.BUILD-SNAPSHOT-test.jar");
					assertThat(classifier).doesNotExist();
					assertThat(buildLog(project)).contains("Building image")
							.contains("docker.io/library/build-image-classifier:0.0.1.BUILD-SNAPSHOT")
							.contains("Successfully built image");
					ImageReference imageReference = ImageReference.of(ImageName.of("build-image-classifier"),
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
	void whenBuildImageIsInvokedWithClassifierSourceWithoutRepackageTheArchiveIsRepackagedOnTheFly(
			MavenBuild mavenBuild) {
		mavenBuild.project("build-image-classifier-source").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
				.prepare(this::writeLongNameResource).execute((project) -> {
					File jar = new File(project, "target/build-image-classifier-source-0.0.1.BUILD-SNAPSHOT-test.jar");
					assertThat(jar).isFile();
					File original = new File(project,
							"target/build-image-classifier-source-0.0.1.BUILD-SNAPSHOT-test.jar.original");
					assertThat(original).doesNotExist();
					assertThat(buildLog(project)).contains("Building image")
							.contains("docker.io/library/build-image-classifier-source:0.0.1.BUILD-SNAPSHOT")
							.contains("Successfully built image");
					ImageReference imageReference = ImageReference.of(ImageName.of("build-image-classifier-source"),
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
	void whenBuildImageIsInvokedWithRepackageTheExistingArchiveIsUsed(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-with-repackage").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
				.prepare(this::writeLongNameResource).execute((project) -> {
					File jar = new File(project, "target/build-image-with-repackage-0.0.1.BUILD-SNAPSHOT.jar");
					assertThat(jar).isFile();
					File original = new File(project,
							"target/build-image-with-repackage-0.0.1.BUILD-SNAPSHOT.jar.original");
					assertThat(original).isFile();
					assertThat(buildLog(project)).contains("Building image")
							.contains("docker.io/library/build-image-with-repackage:0.0.1.BUILD-SNAPSHOT")
							.contains("Successfully built image");
					ImageReference imageReference = ImageReference.of(ImageName.of("build-image-with-repackage"),
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
	void whenBuildImageIsInvokedWithClassifierAndRepackageTheExistingArchiveIsUsed(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-classifier-with-repackage").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
				.prepare(this::writeLongNameResource).execute((project) -> {
					File jar = new File(project,
							"target/build-image-classifier-with-repackage-0.0.1.BUILD-SNAPSHOT.jar");
					assertThat(jar).isFile();
					File original = new File(project,
							"target/build-image-classifier-with-repackage-0.0.1.BUILD-SNAPSHOT-test.jar");
					assertThat(original).isFile();
					assertThat(buildLog(project)).contains("Building image")
							.contains("docker.io/library/build-image-classifier-with-repackage:0.0.1.BUILD-SNAPSHOT")
							.contains("Successfully built image");
					ImageReference imageReference = ImageReference
							.of(ImageName.of("build-image-classifier-with-repackage"), "0.0.1.BUILD-SNAPSHOT");
					try (GenericContainer<?> container = new GenericContainer<>(imageReference.toString())) {
						container.waitingFor(Wait.forLogMessage("Launched\\n", 1)).start();
					}
					finally {
						removeImage(imageReference);
					}
				});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithClassifierSourceAndRepackageTheExistingArchiveIsUsed(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-classifier-source-with-repackage").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
				.prepare(this::writeLongNameResource).execute((project) -> {
					File jar = new File(project,
							"target/build-image-classifier-source-with-repackage-0.0.1.BUILD-SNAPSHOT-test.jar");
					assertThat(jar).isFile();
					File original = new File(project,
							"target/build-image-classifier-source-with-repackage-0.0.1.BUILD-SNAPSHOT-test.jar.original");
					assertThat(original).isFile();
					assertThat(buildLog(project)).contains("Building image").contains(
							"docker.io/library/build-image-classifier-source-with-repackage:0.0.1.BUILD-SNAPSHOT")
							.contains("Successfully built image");
					ImageReference imageReference = ImageReference
							.of(ImageName.of("build-image-classifier-source-with-repackage"), "0.0.1.BUILD-SNAPSHOT");
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
	void failsWithWarPackaging(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-war-packaging").goals("package").executeAndFail(
				(project) -> assertThat(buildLog(project)).contains("Executable jar file required for building image"));
	}

	@TestTemplate
	void failsWhenFinalNameIsMisconfigured(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-final-name").goals("package")
				.executeAndFail((project) -> assertThat(buildLog(project)).contains("final-name.jar.original")
						.contains("is required for building an image"));
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
