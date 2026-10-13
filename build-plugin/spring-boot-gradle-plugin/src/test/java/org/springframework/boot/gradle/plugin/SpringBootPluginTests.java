/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.plugin;

import java.io.File;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.gradle.junit.GradleProjectBuilder;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootPlugin}.
 *
 * @author Martin Chalupa
 * @author Andy Wilkinson
 */
@ClassPathExclusions("kotlin-daemon-client-*.jar")
class SpringBootPluginTests {

	@TempDir
	File temp;

	@Test
	void bootArchivesConfigurationsCannotBeResolved() {
		Project project = GradleProjectBuilder.builder().withProjectDir(this.temp).build();
		project.getPlugins().apply(SpringBootPlugin.class);
		Configuration bootArchives = project.getConfigurations()
			.getByName(SpringBootPlugin.BOOT_ARCHIVES_CONFIGURATION_NAME);
		assertThat(bootArchives.isCanBeResolved()).isFalse();
	}

}
