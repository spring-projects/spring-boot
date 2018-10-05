/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle.docs;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.gradle.junit.GradleMultiDslSuite;
import org.springframework.boot.gradle.testkit.GradleBuild;

/**
 * Tests for the getting started documentation.
 *
 * @author Andy Wilkinson
 * @author Jean-Baptiste Nizet
 */
@RunWith(GradleMultiDslSuite.class)
public class GettingStartedDocumentationTests {

	@Rule
	public GradleBuild gradleBuild;

	// NOTE: We can't run any `apply-plugin` tests because during a release the
	// jar won't be there

	@Test
	public void typicalPluginsAppliesExceptedPlugins() {
		this.gradleBuild.script("src/main/gradle/getting-started/typical-plugins")
				.build("verify");
	}

}
