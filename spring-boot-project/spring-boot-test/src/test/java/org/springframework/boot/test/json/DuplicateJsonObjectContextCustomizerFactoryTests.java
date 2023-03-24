/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.test.json;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.testsupport.classpath.ClassPathOverrides;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DuplicateJsonObjectContextCustomizerFactory}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
@ClassPathOverrides("org.json:json:20140107")
class DuplicateJsonObjectContextCustomizerFactoryTests {

	private CapturedOutput output;

	@BeforeEach
	void setup(CapturedOutput output) {
		this.output = output;
	}

	@Test
	void warningForMultipleVersions() {
		new DuplicateJsonObjectContextCustomizerFactory().createContextCustomizer(null, null)
			.customizeContext(null, null);
		assertThat(this.output).contains("Found multiple occurrences of org.json.JSONObject on the class path:");
	}

}
