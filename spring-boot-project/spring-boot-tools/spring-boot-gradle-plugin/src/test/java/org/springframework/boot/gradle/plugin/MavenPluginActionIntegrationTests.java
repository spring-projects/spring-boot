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

import org.junit.jupiter.api.TestTemplate;

import org.springframework.boot.gradle.junit.GradleCompatibility;
import org.springframework.boot.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MavenPluginAction}.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility
class MavenPluginActionIntegrationTests {

	GradleBuild gradleBuild;

	@TestTemplate
	void clearsConf2ScopeMappingsOfUploadBootArchivesTask() {
		assertThat(this.gradleBuild.expectDeprecationWarningsWithAtLeastVersion("6.0.0").build("conf2ScopeMappings")
				.getOutput()).contains("Conf2ScopeMappings = 0");
	}

}
