/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.logging.log4j2;

import org.junit.jupiter.api.Test;

import org.springframework.boot.logging.log4j2.ColorConverterTests.TestLogEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnclosedInSquareBracketsConverter}.
 *
 * @author Phillip Webb
 */
class EnclosedInSquareBracketsConverterTests {

	private TestLogEvent event;

	@Test
	void transformWhenEmpty() {
		StringBuilder output = new StringBuilder();
		newConverter("").format(this.event, output);
		assertThat(output).hasToString("");
	}

	@Test
	void transformWhenName() {
		StringBuilder output = new StringBuilder();
		newConverter("My Application").format(this.event, output);
		assertThat(output).hasToString("[My Application] ");
	}

	private EnclosedInSquareBracketsConverter newConverter(String in) {
		return EnclosedInSquareBracketsConverter.newInstance(null, new String[] { in });
	}

}
