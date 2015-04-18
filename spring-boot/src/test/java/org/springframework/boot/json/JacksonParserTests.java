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

import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.json.jackson.ObjectMapperProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link JsonParser}.
 *
 * @author Dave Syer
 * @author Dan Paquette
 */
@RunWith(MockitoJUnitRunner.class)
public class JacksonParserTests extends AbstractJsonParserTests {

	private static final String TEST_LIST_JSON = "[\"value\"]";

	@Mock
	private ObjectMapperProvider mockObjectMapperProvider;

	@Mock
	private ObjectMapper mockObjectMapper;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Override
	protected JsonParser getParser() {
		return new JacksonJsonParser();
	}

	@Test
	public void testNullObjectMapperProviderParameter() {
		this.exception.expect(IllegalArgumentException.class);
		new JacksonJsonParser((ObjectMapperProvider) null);
	}

	@Test
	public void testNullObjectMapperParameter() {
		this.exception.expect(IllegalArgumentException.class);
		new JacksonJsonParser((ObjectMapper) null);
	}

	@Test
	public void testCustomObjectMapper() throws Exception {
		when(this.mockObjectMapper.readValue(TEST_LIST_JSON, List.class)).thenReturn(
				Collections.singletonList("value"));
		JacksonJsonParser parser = new JacksonJsonParser(this.mockObjectMapper);
		List<Object> parsedList = parser.parseList(TEST_LIST_JSON);
		assertEquals(1, parsedList.size());
		assertEquals("value", parsedList.get(0));
		verify(this.mockObjectMapper, times(1)).readValue(TEST_LIST_JSON, List.class);
	}

	@Test
	public void testParseListWithSuppliedObjectMapperProvider() throws Exception {
		when(this.mockObjectMapperProvider.getObjectMapper()).thenReturn(
				this.mockObjectMapper);
		when(this.mockObjectMapper.readValue(TEST_LIST_JSON, List.class)).thenReturn(
				Collections.singletonList("value"));
		JacksonJsonParser parser = new JacksonJsonParser(this.mockObjectMapperProvider);
		List<Object> parsedList = parser.parseList(TEST_LIST_JSON);
		assertEquals(1, parsedList.size());
		assertEquals("value", parsedList.get(0));
		verify(this.mockObjectMapperProvider, times(1)).getObjectMapper();
		verify(this.mockObjectMapper, times(1)).readValue(TEST_LIST_JSON, List.class);
	}
}
