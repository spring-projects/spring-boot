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

package org.springframework.boot.test.system;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OutputCaptureExtension} when used via {@link ExtendWith @ExtendWith}.
 *
 * @author Madhura Bhave
 */
@ExtendWith(OutputCaptureExtension.class)
@ExtendWith(OutputExtensionExtendWithTests.BeforeAllExtension.class)
class OutputExtensionExtendWithTests {

	@Test
	void captureShouldReturnOutputCapturedBeforeTestMethod(CapturedOutput output) {
		assertThat(output).contains("Before all").doesNotContain("Hello");
	}

	@Test
	void captureShouldReturnAllCapturedOutput(CapturedOutput output) {
		System.out.println("Hello World");
		System.err.println("Error!!!");
		assertThat(output).contains("Before all").contains("Hello World").contains("Error!!!");
	}

	static class BeforeAllExtension implements BeforeAllCallback {

		@Override
		public void beforeAll(ExtensionContext context) {
			System.out.println("Before all");
		}

	}

}
