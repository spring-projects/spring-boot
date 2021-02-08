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

package org.springframework.boot.gradle.tasks.run;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.gradle.api.JavaVersion;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.TestTemplate;

import org.springframework.boot.gradle.junit.GradleCompatibility;
import org.springframework.boot.gradle.testkit.GradleBuild;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the {@link BootRun} task.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility(configurationCache = true)
class BootRunIntegrationTests {

	GradleBuild gradleBuild;

	@TestTemplate
	void basicExecution() throws IOException {
		copyClasspathApplication();
		new File(this.gradleBuild.getProjectDir(), "src/main/resources").mkdirs();
		BuildResult result = this.gradleBuild.build("bootRun");
		assertThat(result.task(":bootRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("1. " + canonicalPathOf("build/classes/java/main"));
		assertThat(result.getOutput()).contains("2. " + canonicalPathOf("build/resources/main"));
		assertThat(result.getOutput()).doesNotContain(canonicalPathOf("src/main/resources"));
	}

	@TestTemplate
	void sourceResourcesCanBeUsed() throws IOException {
		copyClasspathApplication();
		BuildResult result = this.gradleBuild.build("bootRun");
		assertThat(result.task(":bootRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("1. " + canonicalPathOf("src/main/resources"));
		assertThat(result.getOutput()).contains("2. " + canonicalPathOf("build/classes/java/main"));
		assertThat(result.getOutput()).doesNotContain(canonicalPathOf("build/resources/main"));
	}

	@TestTemplate
	void springBootExtensionMainClassNameIsUsed() throws IOException {
		copyMainClassApplication();
		BuildResult result = this.gradleBuild.build("bootRun");
		assertThat(result.task(":bootRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("com.example.bootrun.main.CustomMainClass");
	}

	@TestTemplate
	void applicationPluginMainClassNameIsUsed() throws IOException {
		copyMainClassApplication();
		BuildResult result = this.gradleBuild.build("bootRun");
		assertThat(result.task(":bootRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("com.example.bootrun.main.CustomMainClass");
	}

	@TestTemplate
	void applicationPluginMainClassNameIsNotUsedWhenItIsNull() throws IOException {
		copyClasspathApplication();
		BuildResult result = this.gradleBuild.build("bootRun");
		assertThat(result.task(":bootRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput())
				.contains("Main class name = com.example.bootrun.classpath.BootRunClasspathApplication");
	}

	@TestTemplate
	void defaultJvmArgs() throws IOException {
		copyJvmArgsApplication();
		BuildResult result = this.gradleBuild.build("bootRun");
		assertThat(result.task(":bootRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_13)) {
			assertThat(result.getOutput()).contains("1. -XX:TieredStopAtLevel=1");
		}
		else {
			assertThat(result.getOutput()).contains("1. -Xverify:none").contains("2. -XX:TieredStopAtLevel=1");
		}
	}

	@TestTemplate
	void optimizedLaunchDisabledJvmArgs() throws IOException {
		copyJvmArgsApplication();
		BuildResult result = this.gradleBuild.build("bootRun");
		assertThat(result.task(":bootRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).doesNotContain("-Xverify:none").doesNotContain("-XX:TieredStopAtLevel=1");
	}

	@TestTemplate
	void applicationPluginJvmArgumentsAreUsed() throws IOException {
		copyJvmArgsApplication();
		BuildResult result = this.gradleBuild.build("bootRun");
		assertThat(result.task(":bootRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_13)) {
			assertThat(result.getOutput()).contains("1. -Dcom.bar=baz").contains("2. -Dcom.foo=bar")
					.contains("3. -XX:TieredStopAtLevel=1");
		}
		else {
			assertThat(result.getOutput()).contains("1. -Dcom.bar=baz").contains("2. -Dcom.foo=bar")
					.contains("3. -Xverify:none").contains("4. -XX:TieredStopAtLevel=1");
		}
	}

	@TestTemplate
	void jarTypeFilteringIsAppliedToTheClasspath() throws IOException {
		copyClasspathApplication();
		File flatDirRepository = new File(this.gradleBuild.getProjectDir(), "repository");
		createDependenciesStarterJar(new File(flatDirRepository, "starter.jar"));
		createStandardJar(new File(flatDirRepository, "standard.jar"));
		BuildResult result = this.gradleBuild.build("bootRun");
		assertThat(result.task(":bootRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("standard.jar").doesNotContain("starter.jar");
	}

	private void copyMainClassApplication() throws IOException {
		copyApplication("main");
	}

	private void copyClasspathApplication() throws IOException {
		copyApplication("classpath");
	}

	private void copyJvmArgsApplication() throws IOException {
		copyApplication("jvmargs");
	}

	private void copyApplication(String name) throws IOException {
		File output = new File(this.gradleBuild.getProjectDir(), "src/main/java/com/example/bootrun/" + name);
		output.mkdirs();
		FileSystemUtils.copyRecursively(new File("src/test/java/com/example/bootrun/" + name), output);
	}

	private String canonicalPathOf(String path) throws IOException {
		return new File(this.gradleBuild.getProjectDir(), path).getCanonicalPath();
	}

	private void createStandardJar(File location) throws IOException {
		createJar(location, (attributes) -> {
		});
	}

	private void createDependenciesStarterJar(File location) throws IOException {
		createJar(location, (attributes) -> attributes.putValue("Spring-Boot-Jar-Type", "dependencies-starter"));
	}

	private void createJar(File location, Consumer<Attributes> attributesConfigurer) throws IOException {
		location.getParentFile().mkdirs();
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		attributesConfigurer.accept(attributes);
		new JarOutputStream(new FileOutputStream(location), manifest).close();
	}

}
