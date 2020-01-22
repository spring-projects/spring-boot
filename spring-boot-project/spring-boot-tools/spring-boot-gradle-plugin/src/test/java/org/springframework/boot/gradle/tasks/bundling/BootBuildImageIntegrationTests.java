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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

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
 */
@ExtendWith(GradleCompatibilityExtension.class)
@DisabledIfDockerUnavailable
class BootBuildImageIntegrationTests {

	GradleBuild gradleBuild;

	@TestTemplate
	void bootBuildImageBuildsImage() throws IOException {
		writeMainClass();
		BuildResult result = this.gradleBuild.build("bootBuildImage");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		ImageReference imageReference = ImageReference.of(ImageName.of(this.gradleBuild.getProjectDir().getName()));
		try (GenericContainer<?> container = new GenericContainer<>(imageReference.toString())) {
			container.waitingFor(Wait.forLogMessage("Launched\\n", 1)).start();
		}
		finally {
			new DockerApi().image().remove(imageReference, false);
		}
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

}
