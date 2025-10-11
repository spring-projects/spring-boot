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
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.TestTemplate;

import org.springframework.boot.gradle.junit.GradleCompatibility;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BootJar}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Paddy Drury
 */
@GradleCompatibility(configurationCache = true)
class BootJarIntegrationTests extends AbstractBootArchiveIntegrationTests {

	BootJarIntegrationTests() {
		super("bootJar", "BOOT-INF/lib/", "BOOT-INF/classes/", "BOOT-INF/");
	}

	@TestTemplate
	void signed() throws Exception {
		assertThat(this.gradleBuild.build("bootJar").task(":bootJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		File jar = new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0];
		try (JarFile jarFile = new JarFile(jar)) {
			assertThat(jarFile.getEntry("META-INF/BOOT.SF")).isNotNull();
		}
	}

	@TestTemplate
	void whenAResolvableCopyOfAnUnresolvableConfigurationIsResolvedThenResolutionSucceeds() {
		Assumptions.assumeTrue(this.gradleBuild.gradleVersionIsLessThan("9.0-milestone-1"));
		this.gradleBuild.expectDeprecationWarningsWithAtLeastVersion("8.0").build("build");
	}

	@TestTemplate
	void packagedApplicationClasspath() throws IOException {
		copyClasspathApplication();
		BuildResult result = this.gradleBuild.build("launch");
		String output = result.getOutput();
		assertThat(output).containsPattern("1\\. .*classes");
		assertThat(output).containsPattern("2\\. .*library-1.0-SNAPSHOT.jar");
		assertThat(output).containsPattern("3\\. .*commons-lang3-3.9.jar");
		assertThat(output).containsPattern("4\\. .*spring-boot-jarmode-tools.*.jar");
		assertThat(output).doesNotContain("5. ");
	}

	@TestTemplate
	void explodedApplicationClasspath() throws IOException {
		copyClasspathApplication();
		BuildResult result = this.gradleBuild.build("launch");
		String output = result.getOutput();
		assertThat(output).containsPattern("1\\. .*classes");
		assertThat(output).containsPattern("2\\. .*spring-boot-jarmode-tools.*.jar");
		assertThat(output).containsPattern("3\\. .*library-1.0-SNAPSHOT.jar");
		assertThat(output).containsPattern("4\\. .*commons-lang3-3.9.jar");
		assertThat(output).doesNotContain("5. ");
	}

	private void copyClasspathApplication() throws IOException {
		copyApplication("classpath");
	}

	@Override
	String[] getExpectedApplicationLayerContents(String... additionalFiles) {
		Set<String> contents = new TreeSet<>(Arrays.asList(additionalFiles));
		contents.addAll(Arrays.asList("BOOT-INF/classpath.idx", "BOOT-INF/layers.idx", "META-INF/"));
		return contents.toArray(new String[0]);
	}

}
