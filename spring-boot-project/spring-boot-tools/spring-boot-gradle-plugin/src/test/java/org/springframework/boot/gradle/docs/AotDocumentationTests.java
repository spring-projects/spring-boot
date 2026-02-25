/*
 * Copyright 2012-present the original author or authors.
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
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AOT documentation.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(GradleMultiDslExtension.class)
class AotDocumentationTests {

	GradleBuild gradleBuild;

	@TestTemplate
	void applyNativeImagePlugin() {
		assertThat(this.gradleBuild.script(Examples.DIR + "aot/apply-native-image-plugin").build("tasks").getOutput())
			.contains("nativeCompile")
			.contains("aotClasses");
	}

	@TestTemplate
	void applyAotPlugin() {
		assertThat(this.gradleBuild.script(Examples.DIR + "aot/apply-aot-plugin").build("tasks").getOutput())
			.contains("aotClasses")
			.doesNotContain("nativeCompile");
	}

}
