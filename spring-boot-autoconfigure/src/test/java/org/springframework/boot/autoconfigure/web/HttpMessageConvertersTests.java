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

package org.springframework.boot.autoconfigure.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpMessageConverters}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public class HttpMessageConvertersTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void containsDefaults() throws Exception {
		HttpMessageConverters converters = new HttpMessageConverters();
		List<Class<?>> converterClasses = new ArrayList<Class<?>>();
		for (HttpMessageConverter<?> converter : converters) {
			converterClasses.add(converter.getClass());
		}
		assertThat(converterClasses, equalTo(Arrays.<Class<?>> asList(
				ByteArrayHttpMessageConverter.class, StringHttpMessageConverter.class,
				ResourceHttpMessageConverter.class, SourceHttpMessageConverter.class,
				AllEncompassingFormHttpMessageConverter.class,
				MappingJackson2HttpMessageConverter.class,
				Jaxb2RootElementHttpMessageConverter.class)));
	}

	@Test
	public void addBeforeExistingConverter() {
		MappingJackson2HttpMessageConverter converter1 = new MappingJackson2HttpMessageConverter();
		MappingJackson2HttpMessageConverter converter2 = new MappingJackson2HttpMessageConverter();
		HttpMessageConverters converters = new HttpMessageConverters(converter1,
				converter2);
		assertTrue(converters.getConverters().contains(converter1));
		assertTrue(converters.getConverters().contains(converter2));
		List<MappingJackson2HttpMessageConverter> httpConverters = new ArrayList<MappingJackson2HttpMessageConverter>();
		for (HttpMessageConverter<?> candidate : converters) {
			if (candidate instanceof MappingJackson2HttpMessageConverter) {
				httpConverters.add((MappingJackson2HttpMessageConverter) candidate);
			}
		}
		// The existing converter is still there, but with a lower priority
		assertEquals(3, httpConverters.size());
		assertEquals(0, httpConverters.indexOf(converter1));
		assertEquals(1, httpConverters.indexOf(converter2));
		assertNotEquals(0, converters.getConverters().indexOf(converter1));
	}

	@Test
	public void addNewConverters() {
		HttpMessageConverter<?> converter1 = mock(HttpMessageConverter.class);
		HttpMessageConverter<?> converter2 = mock(HttpMessageConverter.class);
		HttpMessageConverters converters = new HttpMessageConverters(converter1,
				converter2);
		assertTrue(converters.getConverters().contains(converter1));
		assertEquals(converter1, converters.getConverters().get(0));
		assertEquals(converter2, converters.getConverters().get(1));
	}

}
