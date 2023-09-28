/*
 * Copyright 2012-2023 the original author or authors.
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

import org.assertj.core.api.Assumptions;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.TestTemplate;

import org.springframework.boot.gradle.junit.GradleCompatibility;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the {@link BootRun} task configured to use the test source set.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility(configurationCache = true)
class BootTestRunIntegrationTests {

	GradleBuild gradleBuild;

	@TestTemplate
	void basicExecution() throws IOException {
		copyClasspathApplication();
		BuildResult result = this.gradleBuild.build("bootTestRun");
		assertThat(result.task(":bootTestRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("1. " + canonicalPathOf("build/classes/java/test"))
			.contains("2. " + canonicalPathOf("build/resources/test"))
			.contains("3. " + canonicalPathOf("build/classes/java/main"))
			.contains("4. " + canonicalPathOf("build/resources/main"));
	}

	@TestTemplate
	void defaultJvmArgs() throws IOException {
		copyJvmArgsApplication();
		BuildResult result = this.gradleBuild.build("bootTestRun");
		assertThat(result.task(":bootTestRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("-XX:TieredStopAtLevel=1");
	}

	@TestTemplate
	void optimizedLaunchDisabledJvmArgs() throws IOException {
		copyJvmArgsApplication();
		BuildResult result = this.gradleBuild.build("bootTestRun");
		assertThat(result.task(":bootTestRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).doesNotContain("-Xverify:none").doesNotContain("-XX:TieredStopAtLevel=1");
	}

	@TestTemplate
	void applicationPluginJvmArgumentsAreUsed() throws IOException {
		if (this.gradleBuild.isConfigurationCache()) {
			// https://github.com/gradle/gradle/pull/23924
			GradleVersion gradleVersion = GradleVersion.version(this.gradleBuild.getGradleVersion());
			Assumptions.assumeThat(gradleVersion)
				.isLessThan(GradleVersion.version("8.0"))
				.isGreaterThanOrEqualTo(GradleVersion.version("8.1-rc-1"));
		}
		copyJvmArgsApplication();
		BuildResult result = this.gradleBuild.build("bootTestRun");
		assertThat(result.task(":bootTestRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("-Dcom.bar=baz")
			.contains("-Dcom.foo=bar")
			.contains("-XX:TieredStopAtLevel=1");
	}

	@TestTemplate
	void jarTypeFilteringIsAppliedToTheClasspath() throws IOException {
		copyClasspathApplication();
		File flatDirRepository = new File(this.gradleBuild.getProjectDir(), "repository");
		createDependenciesStarterJar(new File(flatDirRepository, "starter.jar"));
		createStandardJar(new File(flatDirRepository, "standard.jar"));
		BuildResult result = this.gradleBuild.build("bootTestRun");
		assertThat(result.task(":bootTestRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("standard.jar").doesNotContain("starter.jar");
	}

	@TestTemplate
	void failsGracefullyWhenNoTestMainMethodIsFound() throws IOException {
		copyApplication("nomain");
		BuildResult result = this.gradleBuild.buildAndFail("bootTestRun");
		assertThat(result.task(":bootTestRun").getOutcome()).isEqualTo(TaskOutcome.FAILED);
		if (this.gradleBuild.isConfigurationCache() && this.gradleBuild.gradleVersionIsAtLeast("8.0")) {
			assertThat(result.getOutput())
				.contains("Main class name has not been configured and it could not be resolved from classpath");
		}
		else {
			assertThat(result.getOutput())
				.contains("Main class name has not been configured and it could not be resolved from classpath "
						+ String.join(File.separator, "build", "classes", "java", "test"));
		}
	}

	@TestTemplate
	void developmentOnlyDependenciesAreNotOnTheClasspath() throws IOException {
		copyClasspathApplication();
		BuildResult result = this.gradleBuild.build("bootTestRun");
		assertThat(result.task(":bootTestRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).doesNotContain("commons-lang3-3.12.0.jar");
	}

	@TestTemplate
	void testAndDevelopmentOnlyDependenciesAreOnTheClasspath() throws IOException {
		copyClasspathApplication();
		BuildResult result = this.gradleBuild.build("bootTestRun");
		assertThat(result.task(":bootTestRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("commons-lang3-3.12.0.jar");
	}

	private void copyClasspathApplication() throws IOException {
		copyApplication("classpath");
	}

	private void copyJvmArgsApplication() throws IOException {
		copyApplication("jvmargs");
	}

	private void copyApplication(String name) throws IOException {
		File output = new File(this.gradleBuild.getProjectDir(), "src/test/java/com/example/boottestrun/" + name);
		output.mkdirs();
		FileSystemUtils.copyRecursively(new File("src/test/java/com/example/boottestrun/" + name), output);
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
