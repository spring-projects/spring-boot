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

package org.springframework.boot.opentelemetry.autoconfigure;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link W3CHeaderParser}.
 *
 * @author Moritz Halbritter
 */
class W3CHeaderParserTests {

	@Test
	void shouldParseEmptyString() {
		assertThat(W3CHeaderParser.parse("")).isEmpty();
	}

	@Test
	void shouldParseSingleEntry() {
		Map<String, String> result = W3CHeaderParser.parse("key1=value1");
		assertThat(result).containsExactly(Map.entry("key1", "value1"));
	}

	@Test
	void shouldParseMultipleEntries() {
		Map<String, String> result = W3CHeaderParser.parse("key1=value1,key2=value2,key3=value3");
		assertThat(result).containsExactly(Map.entry("key1", "value1"), Map.entry("key2", "value2"),
				Map.entry("key3", "value3"));
	}

	@Test
	void shouldHandleWhitespaceAroundDelimiters() {
		Map<String, String> result = W3CHeaderParser.parse("key1 = value1 , key2 = value2");
		assertThat(result).containsExactly(Map.entry("key1", "value1"), Map.entry("key2", "value2"));
	}

	@Test
	void shouldPercentDecodeValues() {
		Map<String, String> result = W3CHeaderParser.parse("serverNode=DF%2028,userId=Am%C3%A9lie");
		assertThat(result).containsExactly(Map.entry("serverNode", "DF 28"), Map.entry("userId", "Amélie"));
	}

	@Test
	void shouldIgnoreProperties() {
		Map<String, String> result = W3CHeaderParser
			.parse("key1=value1;property1;property2,key2=value2;propKey=propVal");
		assertThat(result).containsExactly(Map.entry("key1", "value1"), Map.entry("key2", "value2"));
	}

	@Test
	void shouldHandleEqualsSignInValue() {
		Map<String, String> result = W3CHeaderParser.parse("key1=val=ue");
		assertThat(result).containsExactly(Map.entry("key1", "val=ue"));
	}

	@Test
	void shouldSkipEntriesWithoutEqualsSign() {
		Map<String, String> result = W3CHeaderParser.parse("key1=value1,malformed,key2=value2");
		assertThat(result).containsExactly(Map.entry("key1", "value1"), Map.entry("key2", "value2"));
	}

	@Test
	void shouldSkipEntriesWithEmptyKey() {
		Map<String, String> result = W3CHeaderParser.parse("=value1,key2=value2");
		assertThat(result).containsExactly(Map.entry("key2", "value2"));
	}

	@Test
	void shouldAllowEmptyValue() {
		Map<String, String> result = W3CHeaderParser.parse("key1=");
		assertThat(result).containsExactly(Map.entry("key1", ""));
	}

	@Test
	void shouldParseSpecExample() {
		Map<String, String> result = W3CHeaderParser
			.parse("key1=value1;property1;property2, key2 = value2, key3=value3; propertyKey=propertyValue");
		assertThat(result).containsExactly(Map.entry("key1", "value1"), Map.entry("key2", "value2"),
				Map.entry("key3", "value3"));
	}

	@Test
	void shouldKeepLastValueForDuplicateKeys() {
		Map<String, String> result = W3CHeaderParser.parse("key1=first,key1=second");
		assertThat(result).containsExactly(Map.entry("key1", "second"));
	}

}
