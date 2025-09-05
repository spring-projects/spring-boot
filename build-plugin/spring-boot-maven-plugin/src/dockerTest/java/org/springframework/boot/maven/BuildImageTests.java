/*
 * Copyright 2012-present the original author or authors.
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
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.DockerApi.ImageApi;
import org.springframework.boot.buildpack.platform.docker.DockerApi.VolumeApi;
import org.springframework.boot.buildpack.platform.docker.transport.DockerEngineException;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageName;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.docker.type.VolumeName;
import org.springframework.boot.testsupport.container.DisabledIfDockerUnavailable;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Maven plugin's image support.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @author Rafael Ceccone
 * @author Yanming Zhou
 */
@ExtendWith(MavenBuildExtension.class)
@DisabledIfDockerUnavailable
class BuildImageTests extends AbstractArchiveIntegrationTests {

	@TestTemplate
	void whenBuildImageIsInvokedWithoutRepackageTheArchiveIsRepackagedOnTheFly(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.prepare(this::writeLongNameResource)
			.execute((project) -> {
				File jar = new File(project, "target/build-image-0.0.1.BUILD-SNAPSHOT.jar");
				assertThat(jar).isFile();
				File original = new File(project, "target/build-image-0.0.1.BUILD-SNAPSHOT.jar.original");
				assertThat(original).doesNotExist();
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image:0.0.1.BUILD-SNAPSHOT")
					.contains("Running detector")
					.contains("Running builder")
					.contains("---> Test Info buildpack building")
					.contains("---> Test Info buildpack done")
					.contains("Successfully built image");
				removeImage("build-image", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedOnTheCommandLineWithoutRepackageTheArchiveIsRepackagedOnTheFly(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-cmd-line")
			.goals("spring-boot:build-image")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.prepare(this::writeLongNameResource)
			.execute((project) -> {
				File jar = new File(project, "target/build-image-cmd-line-0.0.1.BUILD-SNAPSHOT.jar");
				assertThat(jar).isFile();
				File original = new File(project, "target/build-image-cmd-line-0.0.1.BUILD-SNAPSHOT.jar.original");
				assertThat(original).doesNotExist();
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-cmd-line:0.0.1.BUILD-SNAPSHOT")
					.contains("Running detector")
					.contains("Running builder")
					.contains("---> Test Info buildpack building")
					.contains("---> Test Info buildpack done")
					.contains("Successfully built image");
				removeImage("build-image-cmd-line", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenPackageIsInvokedWithClassifierTheOriginalArchiveIsFound(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-classifier")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.prepare(this::writeLongNameResource)
			.execute((project) -> {
				File jar = new File(project, "target/build-image-classifier-0.0.1.BUILD-SNAPSHOT.jar");
				assertThat(jar).isFile();
				File classifier = new File(project, "target/build-image-classifier-0.0.1.BUILD-SNAPSHOT-test.jar");
				assertThat(classifier).doesNotExist();
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-classifier:0.0.1.BUILD-SNAPSHOT")
					.contains("---> Test Info buildpack building")
					.contains("---> Test Info buildpack done")
					.contains("Successfully built image");
				removeImage("build-image-classifier", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithClassifierAndRepackageTheOriginalArchiveIsFound(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-fork-classifier")
			.goals("spring-boot:build-image")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.prepare(this::writeLongNameResource)
			.execute((project) -> {
				File jar = new File(project, "target/build-image-fork-classifier-0.0.1.BUILD-SNAPSHOT.jar");
				assertThat(jar).isFile();
				File classifier = new File(project, "target/build-image-fork-classifier-0.0.1.BUILD-SNAPSHOT-exec.jar");
				assertThat(classifier).exists();
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-fork-classifier:0.0.1.BUILD-SNAPSHOT")
					.contains("---> Test Info buildpack building")
					.contains("---> Test Info buildpack done")
					.contains("Successfully built image");
				removeImage("build-image-fork-classifier", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithClassifierSourceWithoutRepackageTheArchiveIsRepackagedOnTheFly(
			MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-classifier-source")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.prepare(this::writeLongNameResource)
			.execute((project) -> {
				File jar = new File(project, "target/build-image-classifier-source-0.0.1.BUILD-SNAPSHOT-test.jar");
				assertThat(jar).isFile();
				File original = new File(project,
						"target/build-image-classifier-source-0.0.1.BUILD-SNAPSHOT-test.jar.original");
				assertThat(original).doesNotExist();
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-classifier-source:0.0.1.BUILD-SNAPSHOT")
					.contains("---> Test Info buildpack building")
					.contains("---> Test Info buildpack done")
					.contains("Successfully built image");
				removeImage("build-image-classifier-source", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithRepackageTheExistingArchiveIsUsed(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-with-repackage")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.prepare(this::writeLongNameResource)
			.execute((project) -> {
				File jar = new File(project, "target/build-image-with-repackage-0.0.1.BUILD-SNAPSHOT.jar");
				assertThat(jar).isFile();
				File original = new File(project,
						"target/build-image-with-repackage-0.0.1.BUILD-SNAPSHOT.jar.original");
				assertThat(original).isFile();
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-with-repackage:0.0.1.BUILD-SNAPSHOT")
					.contains("---> Test Info buildpack building")
					.contains("---> Test Info buildpack done")
					.contains("Successfully built image");
				removeImage("build-image-with-repackage", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithClassifierAndRepackageTheExistingArchiveIsUsed(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-classifier-with-repackage")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.prepare(this::writeLongNameResource)
			.execute((project) -> {
				File jar = new File(project, "target/build-image-classifier-with-repackage-0.0.1.BUILD-SNAPSHOT.jar");
				assertThat(jar).isFile();
				File original = new File(project,
						"target/build-image-classifier-with-repackage-0.0.1.BUILD-SNAPSHOT-test.jar");
				assertThat(original).isFile();
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-classifier-with-repackage:0.0.1.BUILD-SNAPSHOT")
					.contains("---> Test Info buildpack building")
					.contains("---> Test Info buildpack done")
					.contains("Successfully built image");
				removeImage("build-image-classifier-with-repackage", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithClassifierSourceAndRepackageTheExistingArchiveIsUsed(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-classifier-source-with-repackage")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.prepare(this::writeLongNameResource)
			.execute((project) -> {
				File jar = new File(project,
						"target/build-image-classifier-source-with-repackage-0.0.1.BUILD-SNAPSHOT-test.jar");
				assertThat(jar).isFile();
				File original = new File(project,
						"target/build-image-classifier-source-with-repackage-0.0.1.BUILD-SNAPSHOT-test.jar.original");
				assertThat(original).isFile();
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-classifier-source-with-repackage:0.0.1.BUILD-SNAPSHOT")
					.contains("---> Test Info buildpack building")
					.contains("---> Test Info buildpack done")
					.contains("Successfully built image");
				removeImage("build-image-classifier-source-with-repackage", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithWarPackaging(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-war-packaging")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.prepare(this::writeLongNameResource)
			.execute((project) -> {
				File war = new File(project, "target/build-image-war-packaging-0.0.1.BUILD-SNAPSHOT.war");
				assertThat(war).isFile();
				File original = new File(project, "target/build-image-war-packaging-0.0.1.BUILD-SNAPSHOT.war.original");
				assertThat(original).doesNotExist();
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-war-packaging:0.0.1.BUILD-SNAPSHOT")
					.contains("---> Test Info buildpack building")
					.contains("---> Test Info buildpack done")
					.contains("Successfully built image");
				removeImage("build-image-war-packaging", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithCustomImageName(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-custom-name")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.systemProperty("spring-boot.build-image.imageName", "example.com/test/property-ignored:pom-preferred")
			.execute((project) -> {
				File jar = new File(project, "target/build-image-custom-name-0.0.1.BUILD-SNAPSHOT.jar");
				assertThat(jar).isFile();
				File original = new File(project, "target/build-image-custom-name-0.0.1.BUILD-SNAPSHOT.jar.original");
				assertThat(original).doesNotExist();
				assertThat(buildLog(project)).contains("Building image")
					.contains("example.com/test/build-image:0.0.1.BUILD-SNAPSHOT")
					.contains("---> Test Info buildpack building")
					.contains("---> Test Info buildpack done")
					.contains("Successfully built image");
				removeImage("example.com/test/build-image", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithCommandLineParameters(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.systemProperty("spring-boot.build-image.imageName", "example.com/test/cmd-property-name:v1")
			.systemProperty("spring-boot.build-image.builder", "ghcr.io/spring-io/spring-boot-cnb-test-builder:0.0.2")
			.systemProperty("spring-boot.build-image.trustBuilder", "true")
			.systemProperty("spring-boot.build-image.runImage", "paketobuildpacks/run-noble-tiny")
			.systemProperty("spring-boot.build-image.createdDate", "2020-07-01T12:34:56Z")
			.systemProperty("spring-boot.build-image.applicationDirectory", "/application")
			.execute((project) -> {
				assertThat(buildLog(project)).contains("Building image")
					.contains("example.com/test/cmd-property-name:v1")
					.contains("Running creator")
					.contains("---> Test Info buildpack building")
					.contains("---> Test Info buildpack done")
					.contains("Successfully built image");
				Image image = new DockerApi().image()
					.inspect(ImageReference.of("example.com/test/cmd-property-name:v1"));
				assertThat(image.getCreated()).isEqualTo("2020-07-01T12:34:56Z");
				removeImage("example.com/test/cmd-property-name", "v1");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithCustomBuilderImageAndRunImage(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-custom-builder")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.execute((project) -> {
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-v2-builder:0.0.1.BUILD-SNAPSHOT")
					.contains("---> Test Info buildpack building")
					.contains("---> Test Info buildpack done")
					.contains("Successfully built image");
				removeImage("docker.io/library/build-image-v2-builder", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithTrustBuilder(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-trust-builder")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.execute((project) -> {
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-v2-trust-builder:0.0.1.BUILD-SNAPSHOT")
					.contains("Running creator")
					.contains("---> Test Info buildpack building")
					.contains("---> Test Info buildpack done")
					.contains("Successfully built image");
				removeImage("docker.io/library/build-image-v2-trust-builder", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithEmptyEnvEntry(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-empty-env-entry")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.prepare(this::writeLongNameResource)
			.execute((project) -> {
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-empty-env-entry:0.0.1.BUILD-SNAPSHOT")
					.contains("---> Test Info buildpack building")
					.contains("---> Test Info buildpack done")
					.contains("Successfully built image");
				removeImage("build-image-empty-env-entry", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithZipPackaging(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-zip-packaging")
			.goals("package")
			.prepare(this::writeLongNameResource)
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.execute((project) -> {
				File jar = new File(project, "target/build-image-zip-packaging-0.0.1.BUILD-SNAPSHOT.jar");
				assertThat(jar).isFile();
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-zip-packaging:0.0.1.BUILD-SNAPSHOT")
					.contains("Main-Class: org.springframework.boot.loader.launch.PropertiesLauncher")
					.contains("Successfully built image");
				removeImage("build-image-zip-packaging", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithBuildpacks(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-custom-buildpacks")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.execute((project) -> {
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-custom-buildpacks:0.0.1.BUILD-SNAPSHOT")
					.contains("Successfully built image");
				removeImage("build-image-custom-buildpacks", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithBinding(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-bindings")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.execute((project) -> {
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-bindings:0.0.1.BUILD-SNAPSHOT")
					.contains("binding: ca-certificates/type=ca-certificates")
					.contains("binding: ca-certificates/test.crt=---certificate one---")
					.contains("Successfully built image");
				removeImage("build-image-bindings", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithNetworkModeNone(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-network")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.execute((project) -> {
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-network:0.0.1.BUILD-SNAPSHOT")
					.contains("Network status: curl failed")
					.contains("Successfully built image");
				removeImage("build-image-network", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedOnMultiModuleProjectWithPackageGoal(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-multi-module")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.execute((project) -> {
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-multi-module-app:0.0.1.BUILD-SNAPSHOT")
					.contains("Successfully built image");
				removeImage("build-image-multi-module-app", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithTags(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-tags")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.execute((project) -> {
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-tags:0.0.1.BUILD-SNAPSHOT")
					.contains("Successfully built image")
					.contains("docker.io/library/build-image-tags:latest")
					.contains("Successfully created image tag");
				removeImage("build-image-tags", "0.0.1.BUILD-SNAPSHOT");
				removeImage("build-image-tags", "latest");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithVolumeCaches(MavenBuild mavenBuild) {
		String testBuildId = randomString();
		mavenBuild.project("dockerTest", "build-image-volume-caches")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.systemProperty("test-build-id", testBuildId)
			.execute((project) -> {
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-volume-caches:0.0.1.BUILD-SNAPSHOT")
					.contains("Successfully built image");
				removeImage("build-image-volume-caches", "0.0.1.BUILD-SNAPSHOT");
				deleteVolumes("cache-" + testBuildId + ".build", "cache-" + testBuildId + ".launch");
			});
	}

	@TestTemplate
	@EnabledOnOs(value = OS.LINUX, disabledReason = "Works with Docker Engine on Linux but is not reliable with "
			+ "Docker Desktop on other OSs")
	void whenBuildImageIsInvokedWithBindCaches(MavenBuild mavenBuild, @TempDir Path tempDir) {
		String testBuildId = randomString();
		mavenBuild.project("dockerTest", "build-image-bind-caches")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.systemProperty("test-build-id", testBuildId)
			.execute((project) -> {
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-bind-caches:0.0.1.BUILD-SNAPSHOT")
					.contains("Successfully built image");
				removeImage("build-image-bind-caches", "0.0.1.BUILD-SNAPSHOT");
				Path buildCachePath = tempDir.resolve("junit-image-cache-" + testBuildId + "-build");
				Path launchCachePath = tempDir.resolve("junit-image-cache-" + testBuildId + "-launch");
				assertThat(buildCachePath).exists().isDirectory();
				assertThat(launchCachePath).exists().isDirectory();
				cleanupCache(buildCachePath);
				cleanupCache(launchCachePath);
			});
	}

	private static void cleanupCache(Path cachePath) {
		try {
			FileSystemUtils.deleteRecursively(cachePath);
		}
		catch (Exception ex) {
			// ignore
		}
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithCreatedDate(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-created-date")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.execute((project) -> {
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-created-date:0.0.1.BUILD-SNAPSHOT")
					.contains("Successfully built image");
				Image image = new DockerApi().image()
					.inspect(ImageReference.of("docker.io/library/build-image-created-date:0.0.1.BUILD-SNAPSHOT"));
				assertThat(image.getCreated()).isEqualTo("2020-07-01T12:34:56Z");
				removeImage("build-image-created-date", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithCurrentCreatedDate(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-current-created-date")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.execute((project) -> {
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-current-created-date:0.0.1.BUILD-SNAPSHOT")
					.contains("Successfully built image");
				Image image = new DockerApi().image()
					.inspect(ImageReference
						.of("docker.io/library/build-image-current-created-date:0.0.1.BUILD-SNAPSHOT"));
				OffsetDateTime createdDateTime = OffsetDateTime.parse(image.getCreated());
				OffsetDateTime current = OffsetDateTime.now().withOffsetSameInstant(createdDateTime.getOffset());
				assertThat(createdDateTime.getYear()).isEqualTo(current.getYear());
				assertThat(createdDateTime.getMonth()).isEqualTo(current.getMonth());
				assertThat(createdDateTime.getDayOfMonth()).isEqualTo(current.getDayOfMonth());
				removeImage("build-image-current-created-date", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithApplicationDirectory(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-app-dir")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.execute((project) -> {
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-app-dir:0.0.1.BUILD-SNAPSHOT")
					.contains("Successfully built image");
				removeImage("build-image-app-dir", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithEmptySecurityOptions(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-security-opts")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.execute((project) -> {
				assertThat(buildLog(project)).contains("Building image")
					.contains("docker.io/library/build-image-security-opts:0.0.1.BUILD-SNAPSHOT")
					.contains("Successfully built image");
				removeImage("build-image-security-opts", "0.0.1.BUILD-SNAPSHOT");
			});
	}

	@TestTemplate
	@EnabledOnOs(value = { OS.LINUX, OS.MAC }, architectures = "aarch64",
			disabledReason = "Lifecycle will only run on ARM architecture")
	void whenBuildImageIsInvokedOnLinuxArmWithImagePlatformLinuxArm(MavenBuild mavenBuild) throws IOException {
		String builderImage = "ghcr.io/spring-io/spring-boot-cnb-test-builder:0.0.2";
		String runImage = "docker.io/paketobuildpacks/run-noble-tiny:latest";
		String buildpackImage = "ghcr.io/spring-io/spring-boot-test-info:0.0.2";
		removeImages(builderImage, runImage, buildpackImage);
		mavenBuild.project("dockerTest", "build-image-platform-linux-arm").goals("package").execute((project) -> {
			File jar = new File(project, "target/build-image-platform-linux-arm-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar).isFile();
			assertThat(buildLog(project)).contains("Building image")
				.contains("docker.io/library/build-image-platform-linux-arm:0.0.1.BUILD-SNAPSHOT")
				.contains("Pulling builder image '" + builderImage + "' for platform 'linux/arm64'")
				.contains("Pulling run image '" + runImage + "' for platform 'linux/arm64'")
				.contains("Pulling buildpack image '" + buildpackImage + "' for platform 'linux/arm64'")
				.contains("---> Test Info buildpack building")
				.contains("---> Test Info buildpack done")
				.contains("Successfully built image");
			removeImage("docker.io/library/build-image-platform-linux-arm", "0.0.1.BUILD-SNAPSHOT");
		});
		removeImages(builderImage, runImage, buildpackImage);
	}

	@TestTemplate
	@EnabledOnOs(value = { OS.LINUX, OS.MAC }, architectures = "amd64",
			disabledReason = "The expected failure condition will not fail on ARM architectures")
	void failsWhenBuildImageIsInvokedOnLinuxAmdWithImagePlatformLinuxArm(MavenBuild mavenBuild) throws IOException {
		String builderImage = "ghcr.io/spring-io/spring-boot-cnb-test-builder:0.0.2";
		String runImage = "docker.io/paketobuildpacks/run-noble-tiny:latest";
		String buildpackImage = "ghcr.io/spring-io/spring-boot-test-info:0.0.2";
		removeImages(buildpackImage, runImage, buildpackImage);
		mavenBuild.project("dockerTest", "build-image-platform-linux-arm")
			.goals("package")
			.executeAndFail((project) -> assertThat(buildLog(project)).contains("Building image")
				.contains("docker.io/library/build-image-platform-linux-arm:0.0.1.BUILD-SNAPSHOT")
				.contains("Pulling builder image '" + builderImage + "' for platform 'linux/arm64'")
				.contains("Pulling run image '" + runImage + "' for platform 'linux/arm64'")
				.contains("Pulling buildpack image '" + buildpackImage + "' for platform 'linux/arm64'")
				.contains("exec format error"));
		removeImages(builderImage, runImage, buildpackImage);
	}

	@TestTemplate
	void failsWhenBuildImageIsInvokedOnMultiModuleProjectWithBuildImageGoal(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-multi-module")
			.goals("spring-boot:build-image")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.executeAndFail((project) -> assertThat(buildLog(project)).contains("Error packaging archive for image"));
	}

	@TestTemplate
	void failsWhenBuilderFails(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-builder-error")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.executeAndFail((project) -> assertThat(buildLog(project)).contains("Building image")
				.contains("---> Test Info buildpack building")
				.contains("Forced builder failure")
				.containsPattern("Builder lifecycle '.*' failed with status code"));
	}

	@TestTemplate
	void failsWithBuildpackNotInBuilder(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-bad-buildpack")
			.goals("package")
			.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
			.executeAndFail((project) -> assertThat(buildLog(project))
				.contains("'urn:cnb:builder:example/does-not-exist:0.0.1' not found in builder"));
	}

	@TestTemplate
	void failsWhenFinalNameIsMisconfigured(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-final-name")
			.goals("package")
			.executeAndFail((project) -> assertThat(buildLog(project)).contains("final-name.jar.original")
				.contains("is required for building an image"));
	}

	@TestTemplate
	void failsWhenCachesAreConfiguredTwice(MavenBuild mavenBuild) {
		mavenBuild.project("dockerTest", "build-image-caches-multiple")
			.goals("package")
			.executeAndFail((project) -> assertThat(buildLog(project))
				.contains("Each image building cache can be configured only once"));
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

	private void removeImages(String... names) throws IOException {
		ImageApi imageApi = new DockerApi().image();
		for (String name : names) {
			try {
				imageApi.remove(ImageReference.of(name), false);
			}
			catch (DockerEngineException ex) {
				// ignore image remove failures
			}
		}
	}

	private void removeImage(String name, String version) {
		ImageReference imageReference = ImageReference.of(ImageName.of(name), version);
		try {
			new DockerApi().image().remove(imageReference, false);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to remove docker image " + imageReference, ex);
		}
	}

	private void deleteVolumes(String... names) throws IOException {
		VolumeApi volumeApi = new DockerApi().volume();
		for (String name : names) {
			volumeApi.delete(VolumeName.of(name), false);
		}
	}

	private String randomString() {
		IntStream chars = new Random().ints('a', 'z' + 1).limit(10);
		return chars.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
	}

}
