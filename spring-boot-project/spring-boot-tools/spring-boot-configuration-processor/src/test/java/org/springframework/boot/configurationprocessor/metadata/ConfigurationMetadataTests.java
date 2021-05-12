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

package org.springframework.boot.configurationprocessor.metadata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationMetadata}.
 *
 * @author Stephane Nicoll
 */
class ConfigurationMetadataTests {

	@Test
	void toDashedCaseCamelCase() {
		assertThat(toDashedCase("simpleCamelCase")).isEqualTo("simple-camel-case");
	}

	@Test
	void toDashedCaseUpperCamelCaseSuffix() {
		assertThat(toDashedCase("myDLQ")).isEqualTo("my-d-l-q");
	}

	@Test
	void toDashedCaseUpperCamelCaseMiddle() {
		assertThat(toDashedCase("someDLQKey")).isEqualTo("some-d-l-q-key");
	}

	@Test
	void toDashedCaseWordsUnderscore() {
		assertThat(toDashedCase("Word_With_underscore")).isEqualTo("word-with-underscore");
	}

	@Test
	void toDashedCaseWordsSeveralUnderscores() {
		assertThat(toDashedCase("Word___With__underscore")).isEqualTo("word---with--underscore");
	}

	@Test
	void toDashedCaseLowerCaseUnderscore() {
		assertThat(toDashedCase("lower_underscore")).isEqualTo("lower-underscore");
	}

	@Test
	void toDashedCaseUpperUnderscoreSuffix() {
		assertThat(toDashedCase("my_DLQ")).isEqualTo("my-d-l-q");
	}

	@Test
	void toDashedCaseUpperUnderscoreMiddle() {
		assertThat(toDashedCase("some_DLQ_key")).isEqualTo("some-d-l-q-key");
	}

	@Test
	void toDashedCaseMultipleUnderscores() {
		assertThat(toDashedCase("super___crazy")).isEqualTo("super---crazy");
	}

	@Test
	void toDashedCaseLowercase() {
		assertThat(toDashedCase("lowercase")).isEqualTo("lowercase");
	}

	private String toDashedCase(String name) {
		return ConfigurationMetadata.toDashedCase(name);
	}

}
