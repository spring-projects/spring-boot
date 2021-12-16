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

package org.springframework.boot.gradle.docs;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.gradle.junit.GradleMultiDslExtension;
import org.springframework.boot.gradle.testkit.GradleBuild;

/**
 * Tests for the getting started documentation.
 *
 * @author Andy Wilkinson
 * @author Jean-Baptiste Nizet
 */
@ExtendWith(GradleMultiDslExtension.class)
class GettingStartedDocumentationTests {

	GradleBuild gradleBuild;

	// NOTE: We can't run any 'apply-plugin' tests because during a release the
	// jar won't be there

	@TestTemplate
	void typicalPluginsAppliesExceptedPlugins() {
		this.gradleBuild.script("src/docs/gradle/getting-started/typical-plugins").build("verify");
	}

}
