/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.json;

import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

/**
 * Base for {@link JsonParser} tests.
 *
 * @author Dave Syer
 * @author Jean de Klerk
 */
public abstract class AbstractJsonParserTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final JsonParser parser = getParser();

	protected abstract JsonParser getParser();

	@Test
	public void simpleMap() {
		Map<String, Object> map = this.parser.parseMap("{\"foo\":\"bar\",\"spam\":1}");
		assertEquals(2, map.size());
		assertEquals("bar", map.get("foo"));
		assertEquals(1L, ((Number) map.get("spam")).longValue());
	}

	@Test
	public void doubleValue() {
		Map<String, Object> map = this.parser.parseMap("{\"foo\":\"bar\",\"spam\":1.23}");
		assertEquals(2, map.size());
		assertEquals("bar", map.get("foo"));
		assertEquals(1.23d, map.get("spam"));
	}

	@Test
	public void emptyMap() {
		Map<String, Object> map = this.parser.parseMap("{}");
		assertEquals(0, map.size());
	}

	@Test
	public void simpleList() {
		List<Object> list = this.parser.parseList("[\"foo\",\"bar\",1]");
		assertEquals(3, list.size());
		assertEquals("bar", list.get(1));
	}

	@Test
	public void emptyList() {
		List<Object> list = this.parser.parseList("[]");
		assertEquals(0, list.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void listOfMaps() {
		List<Object> list = this.parser
				.parseList("[{\"foo\":\"bar\",\"spam\":1},{\"foo\":\"baz\",\"spam\":2}]");
		assertEquals(2, list.size());
		assertEquals(2, ((Map<String, Object>) list.get(1)).size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void mapOfLists() {
		Map<String, Object> map = this.parser.parseMap(
				"{\"foo\":[{\"foo\":\"bar\",\"spam\":1},{\"foo\":\"baz\",\"spam\":2}]}");
		assertEquals(1, map.size());
		assertEquals(2, ((List<Object>) map.get("foo")).size());
	}

	@Test
	public void mapWithNullThrowsARuntimeException() {
		this.thrown.expect(RuntimeException.class);
		this.parser.parseMap(null);
	}

	@Test
	public void listWithNullThrowsARuntimeException() {
		this.thrown.expect(RuntimeException.class);
		this.parser.parseList(null);
	}

	@Test
	public void mapWithEmptyStringThrowsARuntimeException() {
		this.thrown.expect(RuntimeException.class);
		this.parser.parseMap("");
	}

	@Test
	public void listWithEmptyStringThrowsARuntimeException() {
		this.thrown.expect(RuntimeException.class);
		this.parser.parseList("");
	}

	@Test
	public void mapWithListThrowsARuntimeException() {
		this.thrown.expect(RuntimeException.class);
		this.parser.parseMap("[]");
	}

	@Test
	public void listWithMapThrowsARuntimeException() {
		this.thrown.expect(RuntimeException.class);
		this.parser.parseList("{}");
	}

	@Test
	public void listWithLeadingWhitespace() {
		List<Object> list = this.parser.parseList("\n\t[\"foo\"]");
		assertEquals(1, list.size());
		assertEquals("foo", list.get(0));
	}

	@Test
	public void mapWithLeadingWhitespace() {
		Map<String, Object> map = this.parser.parseMap("\n\t{\"foo\":\"bar\"}");
		assertEquals(1, map.size());
		assertEquals("bar", map.get("foo"));
	}

	@Test
	public void mapWithLeadingWhitespaceListThrowsARuntimeException() {
		this.thrown.expect(RuntimeException.class);
		this.parser.parseMap("\n\t[]");
	}

	@Test
	public void listWithLeadingWhitespaceMapThrowsARuntimeException() {
		this.thrown.expect(RuntimeException.class);
		this.parser.parseList("\n\t{}");
	}

}
