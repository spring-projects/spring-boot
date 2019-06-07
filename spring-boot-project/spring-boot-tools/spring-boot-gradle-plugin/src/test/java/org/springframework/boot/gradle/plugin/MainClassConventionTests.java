/*
 * Copyright 2012-2019 the original author or authors.
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

import java.io.IOException;

import org.gradle.api.Project;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.internal.impldep.org.junit.Before;
import org.gradle.internal.impldep.org.junit.Rule;
import org.gradle.internal.impldep.org.junit.Test;
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder;
import org.gradle.testfixtures.ProjectBuilder;

import org.springframework.boot.gradle.dsl.SpringBootExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class MainClassConventionTests {

	@Rule
	public final TemporaryFolder temp = new TemporaryFolder();

	private Project project;

	private MainClassConvention convention;

	@Before
	public void createConvention() throws IOException {
		this.project = ProjectBuilder.builder().withProjectDir(this.temp.newFolder()).build();
		this.convention = new MainClassConvention(this.project, () -> null);
	}

	@Test
	public void mainClassNameProjectPropertyIsUsed() throws Exception {
		this.project.getExtensions().getByType(ExtraPropertiesExtension.class).set("mainClassName",
				"com.example.MainClass");
		assertThat(this.convention.call()).isEqualTo("com.example.MainClass");
	}

	@Test
	public void springBootExtensionMainClassNameIsUsed() throws Exception {
		SpringBootExtension extension = this.project.getExtensions().create("springBoot", SpringBootExtension.class,
				this.project);
		extension.setMainClassName("com.example.MainClass");
		assertThat(this.convention.call()).isEqualTo("com.example.MainClass");
	}

	@Test
	public void springBootExtensionMainClassNameIsUsedInPreferenceToMainClassNameProjectProperty() throws Exception {
		this.project.getExtensions().getByType(ExtraPropertiesExtension.class).set("mainClassName",
				"com.example.ProjectPropertyMainClass");
		SpringBootExtension extension = this.project.getExtensions().create("springBoot", SpringBootExtension.class,
				this.project);
		extension.setMainClassName("com.example.SpringBootExtensionMainClass");
		assertThat(this.convention.call()).isEqualTo("com.example.SpringBootExtensionMainClass");
	}

}
