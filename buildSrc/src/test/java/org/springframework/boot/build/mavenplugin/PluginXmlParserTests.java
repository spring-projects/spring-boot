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

package org.springframework.boot.build.mavenplugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.boot.build.mavenplugin.PluginXmlParser.Plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
		assertThat(plugin.getMojos().stream().map(PluginXmlParser.Mojo::getGoal).collect(Collectors.toList()))
				.containsExactly("build-info", "help", "repackage", "run", "start", "stop");
	}

	@Test
	void parseNonExistingFileThrowException() {
		assertThatThrownBy(() -> this.parser.parse(new File("src/test/resources/nonexistent.xml")))
				.isInstanceOf(RuntimeException.class).hasCauseInstanceOf(FileNotFoundException.class);
	}

}
