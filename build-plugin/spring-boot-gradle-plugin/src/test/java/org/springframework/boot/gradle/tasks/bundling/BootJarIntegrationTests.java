/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.tasks.bundling;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.gradle.testkit.runner.BuildResult;
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
