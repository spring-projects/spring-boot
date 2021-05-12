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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.type.ImageName;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.gradle.junit.GradleCompatibilityExtension;
import org.springframework.boot.gradle.testkit.GradleBuild;
import org.springframework.boot.testsupport.testcontainers.DisabledIfDockerUnavailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BootBuildImage}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
@ExtendWith(GradleCompatibilityExtension.class)
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
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("env: BP_JVM_VERSION=8.*");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		removeImage(projectName);
	}

	@TestTemplate
	void buildsImageWithCustomName() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("example/test-image-name");
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("env: BP_JVM_VERSION=8.*");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		removeImage("example/test-image-name");
	}

	@TestTemplate
	void buildsImageWithCustomBuilderAndRunImage() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("example/test-image-custom");
		assertThat(result.getOutput())
				.contains("projects.registry.vmware.com/springboot/spring-boot-cnb-builder:0.0.1");
		assertThat(result.getOutput()).contains("projects.registry.vmware.com/springboot/run:tiny-cnb");
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		removeImage("example/test-image-custom");
	}

	@TestTemplate
	void buildsImageWithCommandLineOptions() throws IOException {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.build("bootBuildImage", "--imageName=example/test-image-cmd",
				"--builder=projects.registry.vmware.com/springboot/spring-boot-cnb-builder:0.0.1",
				"--runImage=projects.registry.vmware.com/springboot/run:tiny-cnb");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("example/test-image-cmd");
		assertThat(result.getOutput())
				.contains("projects.registry.vmware.com/springboot/spring-boot-cnb-builder:0.0.1");
		assertThat(result.getOutput()).contains("projects.registry.vmware.com/springboot/run:tiny-cnb");
		assertThat(result.getOutput()).contains("---> Test Info buildpack building");
		assertThat(result.getOutput()).contains("---> Test Info buildpack done");
		removeImage("example/test-image-cmd");
	}

	@TestTemplate
	void failsWithLaunchScript() {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.buildAndFail("bootBuildImage");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.FAILED);
		assertThat(result.getOutput()).contains("not compatible with buildpacks");
	}

	@TestTemplate
	void failsWithBuilderError() {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.buildAndFail("bootBuildImage");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.FAILED);
		assertThat(result.getOutput()).contains("Forced builder failure");
		assertThat(result.getOutput()).containsPattern("Builder lifecycle '.*' failed with status code");
	}

	@TestTemplate
	void failsWithInvalidImageName() {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.buildAndFail("bootBuildImage", "--imageName=example/Invalid-Image-Name");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.FAILED);
		assertThat(result.getOutput()).containsPattern("Unable to parse image reference")
				.containsPattern("example/Invalid-Image-Name");
	}

	@TestTemplate
	void failsWithWarPackaging() {
		writeMainClass();
		writeLongNameResource();
		BuildResult result = this.gradleBuild.buildAndFail("bootBuildImage", "-PapplyWarPlugin");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.FAILED);
		assertThat(result.getOutput()).contains("Executable jar file required for building image");
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
		removeImage(projectName);
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
			writer.println("    public static void main(String[] args) throws Exception {");
			writer.println("        System.out.println(\"Launched\");");
			writer.println("        synchronized(args) {");
			writer.println("            args.wait(); // Prevent exit");
			writer.println("        }");
			writer.println("    }");
			writer.println();
			writer.println("}");
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void writeLongNameResource() {
		StringBuilder name = new StringBuilder();
		new Random().ints('a', 'z' + 1).limit(128).forEach((i) -> name.append((char) i));
		try {
			Path path = this.gradleBuild.getProjectDir().toPath()
					.resolve(Paths.get("src", "main", "resources", name.toString()));
			Files.createDirectories(path.getParent());
			Files.createFile(path);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void removeImage(String name) throws IOException {
		ImageReference imageReference = ImageReference.of(ImageName.of(name));
		new DockerApi().image().remove(imageReference, false);
	}

}
