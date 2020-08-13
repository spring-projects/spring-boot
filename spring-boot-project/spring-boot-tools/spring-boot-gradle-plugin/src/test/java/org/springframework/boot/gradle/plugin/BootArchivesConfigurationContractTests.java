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

package org.springframework.boot.gradle.plugin;

import java.io.File;
import java.io.IOException;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for expectations about {code bootArchives} Gradle configuration about its
 * purpose.
 *
 * @author Martin Chalupa
 */
public class BootArchivesConfigurationContractTests {

	@TempDir
	File temp;

	private Project project;

	@BeforeEach
	void createConvention() throws IOException {
		this.project = ProjectBuilder.builder().withProjectDir(this.temp).build();
	}

	@Test
	void bootArchivesConfigurationsCannotBeResolved() {
		this.project.getPlugins().apply(SpringBootPlugin.class);
		Configuration bootArchives = this.project.getConfigurations()
				.getByName(SpringBootPlugin.BOOT_ARCHIVES_CONFIGURATION_NAME);
		assertThat(bootArchives.isCanBeResolved()).isFalse();
	}

}
