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

package org.springframework.boot.json;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.resources.ResourceContent;
import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Base for {@link JsonParser} tests.
 *
 * @author Dave Syer
 * @author Jean de Klerk
 * @author Stephane Nicoll
 */
abstract class AbstractJsonParserTests {

	private final JsonParser parser = getParser();

	protected abstract JsonParser getParser();

	@Test
	void simpleMap() {
		Map<String, Object> map = this.parser.parseMap("{\"foo\":\"bar\",\"spam\":1}");
		assertThat(map).hasSize(2);
		assertThat(map).containsEntry("foo", "bar");
		Object spam = map.get("spam");
		assertThat(spam).isNotNull();
		assertThat(((Number) spam).longValue()).isOne();
	}

	@Test
	void doubleValue() {
		Map<String, Object> map = this.parser.parseMap("{\"foo\":\"bar\",\"spam\":1.23}");
		assertThat(map).hasSize(2);
		assertThat(map).containsEntry("foo", "bar");
		assertThat(map).containsEntry("spam", 1.23d);
	}

	@Test
	void stringContainingNumber() {
		Map<String, Object> map = this.parser.parseMap("{\"foo\":\"123\"}");
		assertThat(map).hasSize(1);
		assertThat(map).containsEntry("foo", "123");
	}

	@Test
	void stringContainingComma() {
		Map<String, Object> map = this.parser.parseMap("{\"foo\":\"bar1,bar2\"}");
		assertThat(map).hasSize(1);
		assertThat(map).containsEntry("foo", "bar1,bar2");
	}

	@Test
	void emptyMap() {
		Map<String, Object> map = this.parser.parseMap("{}");
		assertThat(map).isEmpty();
	}

	@Test
	void simpleList() {
		List<Object> list = this.parser.parseList("[\"foo\",\"bar\",1]");
		assertThat(list).hasSize(3);
		assertThat(list.get(1)).isEqualTo("bar");
	}

	@Test
	void emptyList() {
		List<Object> list = this.parser.parseList("[]");
		assertThat(list).isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Test
	void listOfMaps() {
		List<Object> list = this.parser.parseList("[{\"foo\":\"bar\",\"spam\":1},{\"foo\":\"baz\",\"spam\":2}]");
		assertThat(list).hasSize(2);
		assertThat(((Map<String, Object>) list.get(1))).hasSize(2);
	}

	@SuppressWarnings("unchecked")
	@Test
	void mapOfLists() {
		Map<String, Object> map = this.parser
			.parseMap("{\"foo\":[{\"foo\":\"bar\",\"spam\":1},{\"foo\":\"baz\",\"spam\":2}]}");
		assertThat(map).hasSize(1);
		assertThat(((List<Object>) map.get("foo"))).hasSize(2);
		assertThat(map.get("foo")).asInstanceOf(InstanceOfAssertFactories.LIST).allMatch(Map.class::isInstance);
	}

	@SuppressWarnings("unchecked")
	@Test
	void nestedLeadingAndTrailingWhitespace() {
		Map<String, Object> map = this.parser
			.parseMap(" {\"foo\": [ { \"foo\" : \"bar\" , \"spam\" : 1 } , { \"foo\" : \"baz\" , \"spam\" : 2 } ] } ");
		assertThat(map).hasSize(1);
		assertThat(((List<Object>) map.get("foo"))).hasSize(2);
		assertThat(map.get("foo")).asInstanceOf(InstanceOfAssertFactories.LIST).allMatch(Map.class::isInstance);
	}

	@Test
	void mapWithNullThrowsARuntimeException() {
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> this.parser.parseMap(null));
	}

	@Test
	void listWithNullThrowsARuntimeException() {
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> this.parser.parseList(null));
	}

	@Test
	void mapWithEmptyStringThrowsARuntimeException() {
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> this.parser.parseMap(""));
	}

	@Test
	void listWithEmptyStringThrowsARuntimeException() {
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> this.parser.parseList(""));
	}

	@Test
	void mapWithListThrowsARuntimeException() {
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> this.parser.parseMap("[]"));
	}

	@Test
	void listWithMapThrowsARuntimeException() {
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> this.parser.parseList("{}"));
	}

	@Test
	void listWithLeadingWhitespace() {
		List<Object> list = this.parser.parseList("\n\t[\"foo\"]");
		assertThat(list).hasSize(1);
		assertThat(list.get(0)).isEqualTo("foo");
	}

	@Test
	void mapWithLeadingWhitespace() {
		Map<String, Object> map = this.parser.parseMap("\n\t{\"foo\":\"bar\"}");
		assertThat(map).hasSize(1);
		assertThat(map).containsEntry("foo", "bar");
	}

	@Test
	void mapWithLeadingWhitespaceListThrowsARuntimeException() {
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> this.parser.parseMap("\n\t[]"));
	}

	@Test
	void listWithLeadingWhitespaceMapThrowsARuntimeException() {
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> this.parser.parseList("\n\t{}"));
	}

	@Test
	void escapeDoubleQuote() {
		String input = "{\"foo\": \"\\\"bar\\\"\"}";
		Map<String, Object> map = this.parser.parseMap(input);
		assertThat(map).containsEntry("foo", "\"bar\"");
	}

	@Test
	void listWithMalformedMap() {
		assertThatExceptionOfType(JsonParseException.class)
			.isThrownBy(() -> this.parser.parseList("[tru,erqett,{\"foo\":fatrue,true,true,true,tr''ue}]"));
	}

	@Test
	void mapWithKeyAndNoValue() {
		assertThatExceptionOfType(JsonParseException.class).isThrownBy(() -> this.parser.parseMap("{\"foo\"}"));
	}

	@Test // gh-31868
	@WithPackageResources("repeated-open-array.txt")
	void listWithRepeatedOpenArray(@ResourceContent("repeated-open-array.txt") String input) {
		assertThatExceptionOfType(JsonParseException.class).isThrownBy(() -> this.parser.parseList(input))
			.havingCause()
			.withMessageContaining("too deeply nested");
	}

	@Test // gh-31869
	@WithPackageResources("large-malformed-json.txt")
	void largeMalformed(@ResourceContent("large-malformed-json.txt") String input) {
		assertThatExceptionOfType(JsonParseException.class).isThrownBy(() -> this.parser.parseList(input));
	}

	@Test // gh-32029
	@WithPackageResources("deeply-nested-map-json.txt")
	void deeplyNestedMap(@ResourceContent("deeply-nested-map-json.txt") String input) {
		assertThatExceptionOfType(JsonParseException.class).isThrownBy(() -> this.parser.parseList(input));
	}

}
