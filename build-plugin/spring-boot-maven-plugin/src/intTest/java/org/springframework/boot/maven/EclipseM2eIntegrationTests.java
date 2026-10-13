/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.maven;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

import org.junit.jupiter.api.Test;

import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to check that our plugin works well with Eclipse m2e.
 *
 * @author Phillip Webb
 */
class EclipseM2eIntegrationTests {

	@Test // gh-21992
	void pluginPomIncludesOptionalShadeDependency() throws Exception {
		String version = new Versions().get("project.version");
		File repository = new File("build/test-maven-repository");
		File pluginDirectory = new File(repository, "org/springframework/boot/spring-boot-maven-plugin/" + version);
		File[] pomFiles = pluginDirectory.listFiles(this::isPomFile);
		Arrays.sort(pomFiles, Comparator.comparing(File::getName));
		File pomFile = pomFiles[pomFiles.length - 1];
		String pomContent = new String(FileCopyUtils.copyToByteArray(pomFile), StandardCharsets.UTF_8);
		assertThat(pomContent).contains("maven-shade-plugin");
	}

	private boolean isPomFile(File file) {
		return file.getName().endsWith(".pom");
	}

}
