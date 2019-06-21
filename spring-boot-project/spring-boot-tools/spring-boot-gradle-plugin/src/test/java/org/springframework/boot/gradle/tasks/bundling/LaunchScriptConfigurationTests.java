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

package org.springframework.boot.gradle.tasks.bundling;

import org.gradle.api.Project;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LaunchScriptConfiguration}.
 *
 * @author Andy Wilkinson
 */
class LaunchScriptConfigurationTests {

	private final AbstractArchiveTask task = mock(AbstractArchiveTask.class);

	private final Project project = mock(Project.class);

	@BeforeEach
	void setUp() {
		given(this.task.getProject()).willReturn(this.project);
	}

	@Test
	void initInfoProvidesUsesArchiveBaseNameByDefault() {
		given(this.task.getBaseName()).willReturn("base-name");
		assertThat(new LaunchScriptConfiguration(this.task).getProperties()).containsEntry("initInfoProvides",
				"base-name");
	}

	@Test
	void initInfoShortDescriptionUsesDescriptionByDefault() {
		given(this.project.getDescription()).willReturn("Project description");
		assertThat(new LaunchScriptConfiguration(this.task).getProperties()).containsEntry("initInfoShortDescription",
				"Project description");
	}

	@Test
	void initInfoShortDescriptionUsesArchiveBaseNameWhenDescriptionIsNull() {
		given(this.task.getBaseName()).willReturn("base-name");
		assertThat(new LaunchScriptConfiguration(this.task).getProperties()).containsEntry("initInfoShortDescription",
				"base-name");
	}

	@Test
	void initInfoShortDescriptionUsesSingleLineVersionOfMultiLineProjectDescription() {
		given(this.project.getDescription()).willReturn("Project\ndescription");
		assertThat(new LaunchScriptConfiguration(this.task).getProperties()).containsEntry("initInfoShortDescription",
				"Project description");
	}

	@Test
	void initInfoDescriptionUsesArchiveBaseNameWhenDescriptionIsNull() {
		given(this.task.getBaseName()).willReturn("base-name");
		assertThat(new LaunchScriptConfiguration(this.task).getProperties()).containsEntry("initInfoDescription",
				"base-name");
	}

	@Test
	void initInfoDescriptionUsesProjectDescriptionByDefault() {
		given(this.project.getDescription()).willReturn("Project description");
		assertThat(new LaunchScriptConfiguration(this.task).getProperties()).containsEntry("initInfoDescription",
				"Project description");
	}

	@Test
	void initInfoDescriptionUsesCorrectlyFormattedMultiLineProjectDescription() {
		given(this.project.getDescription()).willReturn("The\nproject\ndescription");
		assertThat(new LaunchScriptConfiguration(this.task).getProperties()).containsEntry("initInfoDescription",
				"The\n#  project\n#  description");
	}

}
