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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.DockerApi.ImageApi;
import org.springframework.boot.buildpack.platform.docker.DockerApi.VolumeApi;
import org.springframework.boot.buildpack.platform.docker.transport.DockerEngineException;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.docker.type.VolumeName;
import org.springframework.boot.buildpack.platform.io.FilePermissions;
import org.springframework.boot.gradle.junit.GradleCompatibility;
import org.springframework.boot.testsupport.container.DisabledIfDockerUnavailable;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;
import org.springframework.boot.testsupport.junit.DisabledOnOs;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BootBuildImage}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author Rafael Ceccone
 * @author Yanming Zhou
 */
@GradleCompatibility(configurationCache = true)
@DisabledIfDockerUnavailable
class BootBuildImageIntegrationTests {

	GradleBuild gradleBuild;

	@TestTemplate
	void buildsImageWithDefaultBuilder() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput()).contains("Running detector");
		assertThat(result.getOutput()).contains("Running builder");
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("Network status: HTTP/2 200");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		removeImages(projectName);
	}

	@TestTemplate
	void buildsImageWithTrustBuilder() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput()).contains("Running creator");
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("Network status: HTTP/2 200");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		removeImages(projectName);
	}

	@TestTemplate
	void buildsImageWithWarPackaging() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage", "-PapplyWarPlugin");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		File buildLibs = new File(this.gradleBuild.getProjectDir(), "build/libs");
		assertThat(buildLibs.listFiles())
			.containsExactly(new File(buildLibs, this.gradleBuild.getProjectDir().getName() + ".war"));
		removeImages(projectName);
	}

	@TestTemplate
	void buildsImageWithWarPackagingAndJarConfiguration() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		File buildLibs = new File(this.gradleBuild.getProjectDir(), "build/libs");
		assertThat(buildLibs.listFiles())
			.containsExactly(new File(buildLibs, this.gradleBuild.getProjectDir().getName() + ".war"));
		removeImages(projectName);
	}

	@TestTemplate
	void buildsImageWithCustomName() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("example/test-image-name");
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		removeImages("example/test-image-name");
	}

	@TestTemplate
	void buildsImageWithCustomBuilderAndRunImage() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("example/test-image-custom");
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		removeImages("example/test-image-custom");
	}

	@TestTemplate
	void buildsImageWithCommandLineOptions() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage", "--pullPolicy=IF_NOT_PRESENT",
				"--imageName=example/test-image-cmd", "--builder=ghcr.io/spring-io/spring-boot-cnb-test-builder:0.0.2",
				"--trustBuilder", "--runImage=paketobuildpacks/run-noble-tiny", "--createdDate=2020-07-01T12:34:56Z",
				"--applicationDirectory=/application");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("example/test-image-cmd");
		assertThat(result.getOutput()).contains("Running creator");
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		Image image = new DockerApi().image().inspect(ImageReference.of("example/test-image-cmd"));
		assertThat(image.getCreated()).isEqualTo("2020-07-01T12:34:56Z");
		removeImages("example/test-image-cmd");
	}

	@TestTemplate
	void buildsImageWithPullPolicy() throws IOException {
		writeMainClass();
		writeLongNameResource();
		String projectName = this.gradleBuild.getProjectDir().getName();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("Pulled builder image").contains("Pulled run image");
		result = this.gradleBuild.build("bootBuildImage", "--pullPolicy=IF_NOT_PRESENT");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).doesNotContain("Pulled builder image").doesNotContain("Pulled run image");
		removeImages(projectName);
	}

	@TestTemplate
	void buildsImageWithBuildpackFromBuilder() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput()).contains("---> Test Info buildpack building")
			.contains("---> Test Info buildpack done");
		removeImages(projectName);
	}

	@TestTemplate
	@DisabledOnOs(OS.WINDOWS)
	void buildsImageWithBuildpackFromDirectory() throws IOException {
		writeMainClass();
		writeLongNameResource();
		writeBuildpackContent();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput()).contains("---> Hello World buildpack");
		removeImages(projectName);
	}

	@TestTemplate
	@DisabledOnOs(OS.WINDOWS)
	void buildsImageWithBuildpackFromTarGzip() throws IOException {
		writeMainClass();
		writeLongNameResource();
		writeBuildpackContent();
		tarGzipBuildpackContent();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput()).contains("---> Hello World buildpack");
		removeImages(projectName);
	}

	@TestTemplate
	void buildsImageWithBuildpacksFromImages() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput()).contains("---> Test Info buildpack building")
			.contains("---> Test Info buildpack done");
		removeImages(projectName);
	}

	@TestTemplate
	void buildsImageWithBinding() throws IOException {
		writeMainClass();
		writeLongNameResource();
		writeCertificateBindingFiles();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("binding: certificates/type=ca-certificates");
		assertThat(result.getOutput()).contains("binding: certificates/test1.crt=---certificate one---");
		assertThat(result.getOutput()).contains("binding: certificates/test2.crt=---certificate two---");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		removeImages(projectName);
	}

	@TestTemplate
	void buildsImageWithTag() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		assertThat(result.getOutput()).contains("example.com/myapp:latest");
		removeImages(projectName, "example.com/myapp:latest");
	}

	@TestTemplate
	void buildsImageWithLaunchScript() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		removeImages(projectName);
	}

	@TestTemplate
	void buildsImageWithNetworkModeNone() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("Network status: curl failed");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		removeImages(projectName);
	}

	@TestTemplate
	void buildsImageWithVolumeCaches() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		removeImages(projectName);
		deleteVolumes("cache-" + projectName + ".build", "cache-" + projectName + ".launch");
	}

	@TestTemplate
	@EnabledOnOs(value = OS.LINUX, disabledReason = "Works with Docker Engine on Linux but is not reliable with "
			+ "Docker Desktop on other OSs")
	void buildsImageWithBindCaches(@TempDir Path tempDir) throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		removeImages(projectName);
		Path buildCachePath = tempDir.resolve("junit-image-cache-" + projectName + "-build");
		Path launchCachePath = tempDir.resolve("junit-image-cache-" + projectName + "-launch");
		assertThat(buildCachePath).exists().isDirectory();
		assertThat(launchCachePath).exists().isDirectory();
		cleanupCache(buildCachePath);
		cleanupCache(launchCachePath);
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
	void buildsImageWithCreatedDate() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		Image image = new DockerApi().image().inspect(ImageReference.of("docker.io/library/" + projectName));
		assertThat(image.getCreated()).isEqualTo("2020-07-01T12:34:56Z");
		removeImages(projectName);
	}

	@TestTemplate
	void buildsImageWithCurrentCreatedDate() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		Image image = new DockerApi().image().inspect(ImageReference.of("docker.io/library/" + projectName));
		OffsetDateTime createdDateTime = OffsetDateTime.parse(image.getCreated());
		OffsetDateTime current = OffsetDateTime.now().withOffsetSameInstant(createdDateTime.getOffset());
		assertThat(createdDateTime.getYear()).isEqualTo(current.getYear());
		assertThat(createdDateTime.getMonth()).isEqualTo(current.getMonth());
		assertThat(createdDateTime.getDayOfMonth()).isEqualTo(current.getDayOfMonth());
		removeImages(projectName);
	}

	@TestTemplate
	void buildsImageWithApplicationDirectory() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		removeImages(projectName);
	}

	@TestTemplate
	void buildsImageWithEmptySecurityOptions() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		removeImages(projectName);
	}

	@TestTemplate
	@EnabledOnOs(value = { OS.LINUX, OS.MAC }, architectures = "aarch64",
			disabledReason = "Lifecycle will only run on ARM architecture")
	void buildsImageOnLinuxArmWithImagePlatformLinuxArm() throws IOException {
		writeMainClass();
		writeLongNameResource();
		String builderImage = "ghcr.io/spring-io/spring-boot-cnb-test-builder:0.0.2";
		String runImage = "docker.io/paketobuildpacks/run-noble-tiny:latest";
		String buildpackImage = "ghcr.io/spring-io/spring-boot-test-info:0.0.2";
		removeImages(builderImage, runImage, buildpackImage);
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput())
			.contains("Pulling builder image '" + builderImage + "' for platform 'linux/arm64'");
		assertThat(result.getOutput())
			.contains("Pulling builder image '" + builderImage + "' for platform 'linux/arm64'");
		assertThat(result.getOutput()).contains("Pulling run image '" + runImage + "' for platform 'linux/arm64'");
		assertThat(result.getOutput())
			.contains("Pulling buildpack image '" + buildpackImage + "' for platform 'linux/arm64'");
		assertThat(result.getOutput()).contains("Running detector");
		assertThat(result.getOutput()).contains("Running builder");
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		removeImages(projectName, builderImage, runImage, buildpackImage);
	}

	@TestTemplate
	@EnabledOnOs(value = { OS.LINUX, OS.MAC }, architectures = "amd64",
			disabledReason = "The expected failure condition will not fail on ARM architectures")
	void failsWhenBuildingOnLinuxAmdWithImagePlatformLinuxArm() throws IOException {
		writeMainClass();
		writeLongNameResource();
		String builderImage = "ghcr.io/spring-io/spring-boot-cnb-test-builder:0.0.2";
		String runImage = "docker.io/paketobuildpacks/run-noble-tiny:latest";
		String buildpackImage = "ghcr.io/spring-io/spring-boot-test-info:0.0.2";
		removeImages(builderImage, runImage, buildpackImage);
		BuildResult result = this.gradleBuild.buildAndFail("bootBuildImage");
		String projectName = this.gradleBuild.getProjectDir().getName();
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.FAILED);
		assertThat(result.getOutput()).contains("docker.io/library/" + projectName);
		assertThat(result.getOutput())
			.contains("Pulling builder image '" + builderImage + "' for platform 'linux/arm64'");
		assertThat(result.getOutput()).contains("Pulling run image '" + runImage + "' for platform 'linux/arm64'");
		assertThat(result.getOutput())
			.contains("Pulling buildpack image '" + buildpackImage + "' for platform 'linux/arm64'");
		assertThat(result.getOutput()).contains("exec format error");
		removeImages(builderImage, runImage, buildpackImage);
	}

	@TestTemplate
	void failsWithInvalidCreatedDate() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.buildAndFail("bootBuildImage");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.FAILED);
		assertThat(result.getOutput()).contains("Error parsing 'invalid date' as an image created date");
	}

	@TestTemplate
	void failsWithBuilderError() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.buildAndFail("bootBuildImage");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.FAILED);
		assertThat(result.getOutput()).contains("Forced builder failure");
		assertThat(result.getOutput()).containsPattern("Builder lifecycle '.*' failed with status code");
	}

	@TestTemplate
	void failsWithInvalidImageName() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.buildAndFail("bootBuildImage", "--imageName=example/Invalid-Image-Name");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.FAILED);
		assertThat(result.getOutput()).containsPattern("must be an image reference")
			.containsPattern("example/Invalid-Image-Name");
	}

	@TestTemplate
	void failsWithBuildpackNotInBuilder() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.buildAndFail("bootBuildImage");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.FAILED);
		assertThat(result.getOutput()).contains("'urn:cnb:builder:example/does-not-exist:0.0.1' not found in builder");
	}

	@TestTemplate
	void failsWithInvalidTag() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.buildAndFail("bootBuildImage");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.FAILED);
		assertThat(result.getOutput()).containsPattern("must be an image reference")
			.containsPattern("example/Invalid-Tag-Name");
	}

	@TestTemplate
	void failsWhenCachesAreConfiguredTwice() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.buildAndFail("bootBuildImage");
		assertThat(result.getOutput()).containsPattern("Each image building cache can be configured only once");
	}

	@TestTemplate
	void failsWithIncompatiblePlatform() throws IOException {
		writeMainClass();
		BuildResult result = this.gradleBuild.buildAndFail("bootBuildImage");
		assertThat(result.getOutput()).contains(
				"Image platform mismatch detected. The configured platform 'linux/arm64' is not supported by the image 'ghcr.io/spring-io/spring-boot-cnb-test-builder:0.0.3-amd64'. Requested platform 'linux/arm64' but got 'linux/amd64'");
	}

	private void writeMainClass() throws IOException {
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
			writer.println("    public static void main(String[] args) throws Exception {");
			writer.println("        System.out.println(\"Launched\");");
			writer.println("        synchronized(args) {");
			writer.println("            args.wait(); // Prevent exit");
			writer.println("        }");
			writer.println("    }");
			writer.println();
			writer.println("}");
		}
	}

	private void writeLongNameResource() throws IOException {
		StringBuilder name = new StringBuilder();
		new Random().ints('a', 'z' + 1).limit(128).forEach((i) -> name.append((char) i));
		Path path = this.gradleBuild.getProjectDir()
			.toPath()
			.resolve(Paths.get("src", "main", "resources", name.toString()));
		Files.createDirectories(path.getParent());
		Files.createFile(path);
	}

	private void writeBuildpackContent() throws IOException {
		FileAttribute<Set<PosixFilePermission>> dirAttribute = PosixFilePermissions
			.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x"));
		FileAttribute<Set<PosixFilePermission>> execFileAttribute = PosixFilePermissions
			.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx"));
		File buildpackDir = new File(this.gradleBuild.getProjectDir(), "buildpack/hello-world");
		Files.createDirectories(buildpackDir.toPath(), dirAttribute);
		File binDir = new File(buildpackDir, "bin");
		Files.createDirectories(binDir.toPath(), dirAttribute);
		File descriptor = new File(buildpackDir, "buildpack.toml");
		try (PrintWriter writer = new PrintWriter(new FileWriter(descriptor))) {
			writer.println("api = \"0.10\"");
			writer.println("[buildpack]");
			writer.println("id = \"example/hello-world\"");
			writer.println("version = \"0.0.1\"");
			writer.println("name = \"Hello World Buildpack\"");
			writer.println("homepage = \"https://github.com/buildpacks/samples/tree/main/buildpacks/hello-world\"");
			writer.println("[[stacks]]\n");
			writer.println("id = \"*\"");
		}
		File detect = Files.createFile(Paths.get(binDir.getAbsolutePath(), "detect"), execFileAttribute).toFile();
		try (PrintWriter writer = new PrintWriter(new FileWriter(detect))) {
			writer.println("#!/usr/bin/env bash");
			writer.println("set -eo pipefail");
			writer.println("exit 0");
		}
		File build = Files.createFile(Paths.get(binDir.getAbsolutePath(), "build"), execFileAttribute).toFile();
		try (PrintWriter writer = new PrintWriter(new FileWriter(build))) {
			writer.println("#!/usr/bin/env bash");
			writer.println("set -eo pipefail");
			writer.println("echo \"---> Hello World buildpack\"");
			writer.println("echo \"---> done\"");
			writer.println("exit 0");
		}
	}

	private void tarGzipBuildpackContent() throws IOException {
		Path tarGzipPath = Paths.get(this.gradleBuild.getProjectDir().getAbsolutePath(), "hello-world.tgz");
		try (TarArchiveOutputStream tar = new TarArchiveOutputStream(
				new GzipCompressorOutputStream(Files.newOutputStream(Files.createFile(tarGzipPath))))) {
			File buildpackDir = new File(this.gradleBuild.getProjectDir(), "buildpack/hello-world");
			writeDirectoryToTar(tar, buildpackDir, buildpackDir.getAbsolutePath());
		}
	}

	private void writeDirectoryToTar(TarArchiveOutputStream tar, File dir, String baseDirPath) throws IOException {
		for (File file : dir.listFiles()) {
			String name = file.getAbsolutePath().replace(baseDirPath, "");
			int mode = FilePermissions.umaskForPath(file.toPath());
			if (file.isDirectory()) {
				writeTarEntry(tar, name + "/", mode);
				writeDirectoryToTar(tar, file, baseDirPath);
			}
			else {
				writeTarEntry(tar, file, name, mode);
			}
		}
	}

	private void writeTarEntry(TarArchiveOutputStream tar, String name, int mode) throws IOException {
		TarArchiveEntry entry = new TarArchiveEntry(name);
		entry.setMode(mode);
		tar.putArchiveEntry(entry);
		tar.closeArchiveEntry();
	}

	private void writeTarEntry(TarArchiveOutputStream tar, File file, String name, int mode) throws IOException {
		TarArchiveEntry entry = new TarArchiveEntry(file, name);
		entry.setMode(mode);
		tar.putArchiveEntry(entry);
		StreamUtils.copy(Files.newInputStream(file.toPath()), tar);
		tar.closeArchiveEntry();
	}

	private void writeCertificateBindingFiles() throws IOException {
		File bindingDir = new File(this.gradleBuild.getProjectDir(), "bindings/ca-certificates");
		bindingDir.mkdirs();
		File type = new File(bindingDir, "type");
		try (PrintWriter writer = new PrintWriter(new FileWriter(type))) {
			writer.print("ca-certificates");
		}
		File cert1 = new File(bindingDir, "test1.crt");
		try (PrintWriter writer = new PrintWriter(new FileWriter(cert1))) {
			writer.println("---certificate one---");
		}
		File cert2 = new File(bindingDir, "test2.crt");
		try (PrintWriter writer = new PrintWriter(new FileWriter(cert2))) {
			writer.println("---certificate two---");
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

	private void deleteVolumes(String... names) throws IOException {
		VolumeApi volumeApi = new DockerApi().volume();
		for (String name : names) {
			volumeApi.delete(VolumeName.of(name), false);
		}
	}

}
