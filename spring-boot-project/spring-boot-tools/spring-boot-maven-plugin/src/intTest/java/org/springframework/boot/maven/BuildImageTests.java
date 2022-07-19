/*
 * Copyright 2012-2022 the original author or authors.
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
import java.util.stream.IntStream;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.DockerApi.VolumeApi;
import org.springframework.boot.buildpack.platform.docker.type.ImageName;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.docker.type.VolumeName;
import org.springframework.boot.testsupport.testcontainers.DisabledIfDockerUnavailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Maven plugin's image support.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @author Rafael Ceccone
 */
@ExtendWith(MavenBuildExtension.class)
@DisabledIfDockerUnavailable
class BuildImageTests extends AbstractArchiveIntegrationTests {

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
							.contains("---> Test Info buildpack building").contains("env: BP_JVM_VERSION=8.*")
							.contains("---> Test Info buildpack done").contains("Successfully built image");
					removeImage("build-image", "0.0.1.BUILD-SNAPSHOT");
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
							.contains("---> Test Info buildpack building").contains("env: BP_JVM_VERSION=8.*")
							.contains("---> Test Info buildpack done").contains("Successfully built image");
					removeImage("build-image-classifier", "0.0.1.BUILD-SNAPSHOT");
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
							.contains("---> Test Info buildpack building").contains("---> Test Info buildpack done")
							.contains("Successfully built image");
					removeImage("build-image-classifier-source", "0.0.1.BUILD-SNAPSHOT");
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
							.contains("---> Test Info buildpack building").contains("---> Test Info buildpack done")
							.contains("Successfully built image");
					removeImage("build-image-with-repackage", "0.0.1.BUILD-SNAPSHOT");
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
							.contains("---> Test Info buildpack building").contains("---> Test Info buildpack done")
							.contains("Successfully built image");
					removeImage("build-image-classifier-with-repackage", "0.0.1.BUILD-SNAPSHOT");
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
							.contains("---> Test Info buildpack building").contains("---> Test Info buildpack done")
							.contains("Successfully built image");
					removeImage("build-image-classifier-source-with-repackage", "0.0.1.BUILD-SNAPSHOT");
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
							.contains("---> Test Info buildpack building").contains("---> Test Info buildpack done")
							.contains("Successfully built image");
					removeImage("build-image-war-packaging", "0.0.1.BUILD-SNAPSHOT");
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
							.contains("---> Test Info buildpack building").contains("---> Test Info buildpack done")
							.contains("Successfully built image");
					removeImage("example.com/test/build-image", "0.0.1.BUILD-SNAPSHOT");
				});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithCommandLineParameters(MavenBuild mavenBuild) {
		mavenBuild.project("build-image").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
				.systemProperty("spring-boot.build-image.imageName", "example.com/test/cmd-property-name:v1")
				.systemProperty("spring-boot.build-image.builder",
						"projects.registry.vmware.com/springboot/spring-boot-cnb-builder:0.0.1")
				.systemProperty("spring-boot.build-image.runImage",
						"projects.registry.vmware.com/springboot/run:tiny-cnb")
				.execute((project) -> {
					assertThat(buildLog(project)).contains("Building image")
							.contains("example.com/test/cmd-property-name:v1")
							.contains("---> Test Info buildpack building").contains("---> Test Info buildpack done")
							.contains("Successfully built image");
					removeImage("example.com/test/cmd-property-name", "v1");
				});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithCustomBuilderImageAndRunImage(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-custom-builder").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT").execute((project) -> {
					assertThat(buildLog(project)).contains("Building image")
							.contains("docker.io/library/build-image-v2-builder:0.0.1.BUILD-SNAPSHOT")
							.contains("---> Test Info buildpack building").contains("---> Test Info buildpack done")
							.contains("Successfully built image");
					removeImage("docker.io/library/build-image-v2-builder", "0.0.1.BUILD-SNAPSHOT");
				});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithEmptyEnvEntry(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-empty-env-entry").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
				.prepare(this::writeLongNameResource).execute((project) -> {
					assertThat(buildLog(project)).contains("Building image")
							.contains("docker.io/library/build-image-empty-env-entry:0.0.1.BUILD-SNAPSHOT")
							.contains("---> Test Info buildpack building").contains("---> Test Info buildpack done")
							.contains("Successfully built image");
					removeImage("build-image-empty-env-entry", "0.0.1.BUILD-SNAPSHOT");
				});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithZipPackaging(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-zip-packaging").goals("package").prepare(this::writeLongNameResource)
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT").execute((project) -> {
					File jar = new File(project, "target/build-image-zip-packaging-0.0.1.BUILD-SNAPSHOT.jar");
					assertThat(jar).isFile();
					assertThat(buildLog(project)).contains("Building image")
							.contains("docker.io/library/build-image-zip-packaging:0.0.1.BUILD-SNAPSHOT")
							.contains("Main-Class: org.springframework.boot.loader.PropertiesLauncher")
							.contains("Successfully built image");
					removeImage("build-image-zip-packaging", "0.0.1.BUILD-SNAPSHOT");
				});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithBuildpacks(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-custom-buildpacks").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT").execute((project) -> {
					assertThat(buildLog(project)).contains("Building image")
							.contains("docker.io/library/build-image-custom-buildpacks:0.0.1.BUILD-SNAPSHOT")
							.contains("Successfully built image");
					removeImage("build-image-custom-buildpacks", "0.0.1.BUILD-SNAPSHOT");
				});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithBinding(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-bindings").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT").execute((project) -> {
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
		mavenBuild.project("build-image-network").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT").execute((project) -> {
					assertThat(buildLog(project)).contains("Building image")
							.contains("docker.io/library/build-image-network:0.0.1.BUILD-SNAPSHOT")
							.contains("Network status: curl failed").contains("Successfully built image");
					removeImage("build-image-network", "0.0.1.BUILD-SNAPSHOT");
				});
	}

	@TestTemplate
	void whenBuildImageIsInvokedOnMultiModuleProjectWithPackageGoal(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-multi-module").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT").execute((project) -> {
					assertThat(buildLog(project)).contains("Building image")
							.contains("docker.io/library/build-image-multi-module-app:0.0.1.BUILD-SNAPSHOT")
							.contains("Successfully built image");
					removeImage("build-image-multi-module-app", "0.0.1.BUILD-SNAPSHOT");
				});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithTags(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-tags").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT").execute((project) -> {
					assertThat(buildLog(project)).contains("Building image")
							.contains("docker.io/library/build-image-tags:0.0.1.BUILD-SNAPSHOT")
							.contains("Successfully built image").contains("docker.io/library/build-image-tags:latest")
							.contains("Successfully created image tag");
					removeImage("build-image-tags", "0.0.1.BUILD-SNAPSHOT");
					removeImage("build-image-tags", "latest");
				});
	}

	@TestTemplate
	void whenBuildImageIsInvokedWithVolumeCaches(MavenBuild mavenBuild) {
		String testBuildId = randomString();
		mavenBuild.project("build-image-caches").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
				.systemProperty("test-build-id", testBuildId).execute((project) -> {
					assertThat(buildLog(project)).contains("Building image")
							.contains("docker.io/library/build-image-caches:0.0.1.BUILD-SNAPSHOT")
							.contains("Successfully built image");
					removeImage("build-image-caches", "0.0.1.BUILD-SNAPSHOT");
					deleteVolumes("cache-" + testBuildId + ".build", "cache-" + testBuildId + ".launch");
				});
	}

	@TestTemplate
	void failsWhenBuildImageIsInvokedOnMultiModuleProjectWithBuildImageGoal(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-multi-module").goals("spring-boot:build-image")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT").executeAndFail(
						(project) -> assertThat(buildLog(project)).contains("Error packaging archive for image"));
	}

	@TestTemplate
	void failsWhenBuilderFails(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-builder-error").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
				.executeAndFail((project) -> assertThat(buildLog(project)).contains("Building image")
						.contains("---> Test Info buildpack building").contains("Forced builder failure")
						.containsPattern("Builder lifecycle '.*' failed with status code"));
	}

	@TestTemplate
	void failsWithBuildpackNotInBuilder(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-bad-buildpack").goals("package")
				.systemProperty("spring-boot.build-image.pullPolicy", "IF_NOT_PRESENT")
				.executeAndFail((project) -> assertThat(buildLog(project))
						.contains("'urn:cnb:builder:example/does-not-exist:0.0.1' not found in builder"));
	}

	@TestTemplate
	void failsWhenFinalNameIsMisconfigured(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-final-name").goals("package")
				.executeAndFail((project) -> assertThat(buildLog(project)).contains("final-name.jar.original")
						.contains("is required for building an image"));
	}

	@TestTemplate
	void failsWhenCachesAreConfiguredTwice(MavenBuild mavenBuild) {
		mavenBuild.project("build-image-caches-multiple").goals("package")
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
