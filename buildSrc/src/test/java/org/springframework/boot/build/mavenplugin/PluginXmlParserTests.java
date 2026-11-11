/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.mavenplugin;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.jupiter.api.Test;

import org.springframework.boot.build.mavenplugin.PluginXmlParser.Plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link PluginXmlParser}.
 *
 * @author Andy Wilkinson
 * @author Mike Smithson
 */
class PluginXmlParserTests {

	private final PluginXmlParser parser = new PluginXmlParser();

	@Test
	void parseExistingDescriptorReturnPluginDescriptor() {
		Plugin plugin = this.parser.parse(new File("src/test/resources/plugin.xml"));
		assertThat(plugin.getGroupId()).isEqualTo("org.springframework.boot");
		assertThat(plugin.getArtifactId()).isEqualTo("spring-boot-maven-plugin");
		assertThat(plugin.getVersion()).isEqualTo("2.2.0.GRADLE-SNAPSHOT");
		assertThat(plugin.getGoalPrefix()).isEqualTo("spring-boot");
		assertThat(plugin.getMojos().stream().map(PluginXmlParser.Mojo::getGoal)).containsExactly("build-info", "help",
				"repackage", "run", "start", "stop");
	}

	@Test
	void parseNonExistingFileThrowException() {
		assertThatExceptionOfType(RuntimeException.class)
			.isThrownBy(() -> this.parser.parse(new File("src/test/resources/nonexistent.xml")))
			.withCauseInstanceOf(FileNotFoundException.class);
	}

}
