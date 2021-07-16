/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpMessageConverters}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
class HttpMessageConvertersTests {

	@Test
	void containsDefaults() {
		HttpMessageConverters converters = new HttpMessageConverters();
		List<Class<?>> converterClasses = new ArrayList<>();
		for (HttpMessageConverter<?> converter : converters) {
			converterClasses.add(converter.getClass());
		}
		assertThat(converterClasses).containsExactly(ByteArrayHttpMessageConverter.class,
				StringHttpMessageConverter.class, ResourceHttpMessageConverter.class,
				ResourceRegionHttpMessageConverter.class, SourceHttpMessageConverter.class,
				AllEncompassingFormHttpMessageConverter.class, MappingJackson2HttpMessageConverter.class,
				MappingJackson2SmileHttpMessageConverter.class, MappingJackson2CborHttpMessageConverter.class,
				MappingJackson2XmlHttpMessageConverter.class);
	}

	@Test
	void addBeforeExistingConverter() {
		MappingJackson2HttpMessageConverter converter1 = new MappingJackson2HttpMessageConverter();
		MappingJackson2HttpMessageConverter converter2 = new MappingJackson2HttpMessageConverter();
		HttpMessageConverters converters = new HttpMessageConverters(converter1, converter2);
		assertThat(converters.getConverters().contains(converter1)).isTrue();
		assertThat(converters.getConverters().contains(converter2)).isTrue();
		List<MappingJackson2HttpMessageConverter> httpConverters = new ArrayList<>();
		for (HttpMessageConverter<?> candidate : converters) {
			if (candidate instanceof MappingJackson2HttpMessageConverter) {
				httpConverters.add((MappingJackson2HttpMessageConverter) candidate);
			}
		}
		// The existing converter is still there, but with a lower priority
		assertThat(httpConverters).hasSize(3);
		assertThat(httpConverters.indexOf(converter1)).isEqualTo(0);
		assertThat(httpConverters.indexOf(converter2)).isEqualTo(1);
		assertThat(converters.getConverters().indexOf(converter1)).isNotEqualTo(0);
	}

	@Test
	void addBeforeExistingEquivalentConverter() {
		GsonHttpMessageConverter converter1 = new GsonHttpMessageConverter();
		HttpMessageConverters converters = new HttpMessageConverters(converter1);
		List<Class<?>> converterClasses = converters.getConverters().stream().map(HttpMessageConverter::getClass)
				.collect(Collectors.toList());
		assertThat(converterClasses).containsSequence(GsonHttpMessageConverter.class,
				MappingJackson2HttpMessageConverter.class);
	}

	@Test
	void addNewConverters() {
		HttpMessageConverter<?> converter1 = mock(HttpMessageConverter.class);
		HttpMessageConverter<?> converter2 = mock(HttpMessageConverter.class);
		HttpMessageConverters converters = new HttpMessageConverters(converter1, converter2);
		assertThat(converters.getConverters().get(0)).isEqualTo(converter1);
		assertThat(converters.getConverters().get(1)).isEqualTo(converter2);
	}

	@Test
	void convertersAreAddedToFormPartConverter() {
		HttpMessageConverter<?> converter1 = mock(HttpMessageConverter.class);
		HttpMessageConverter<?> converter2 = mock(HttpMessageConverter.class);
		List<HttpMessageConverter<?>> converters = new HttpMessageConverters(converter1, converter2).getConverters();
		List<HttpMessageConverter<?>> partConverters = extractFormPartConverters(converters);
		assertThat(partConverters.get(0)).isEqualTo(converter1);
		assertThat(partConverters.get(1)).isEqualTo(converter2);
	}

	@Test
	void postProcessConverters() {
		HttpMessageConverters converters = new HttpMessageConverters() {

			@Override
			protected List<HttpMessageConverter<?>> postProcessConverters(List<HttpMessageConverter<?>> converters) {
				converters.removeIf(MappingJackson2XmlHttpMessageConverter.class::isInstance);
				return converters;
			}

		};
		List<Class<?>> converterClasses = new ArrayList<>();
		for (HttpMessageConverter<?> converter : converters) {
			converterClasses.add(converter.getClass());
		}
		assertThat(converterClasses).containsExactly(ByteArrayHttpMessageConverter.class,
				StringHttpMessageConverter.class, ResourceHttpMessageConverter.class,
				ResourceRegionHttpMessageConverter.class, SourceHttpMessageConverter.class,
				AllEncompassingFormHttpMessageConverter.class, MappingJackson2HttpMessageConverter.class,
				MappingJackson2SmileHttpMessageConverter.class, MappingJackson2CborHttpMessageConverter.class);
	}

	@Test
	void postProcessPartConverters() {
		HttpMessageConverters converters = new HttpMessageConverters() {

			@Override
			protected List<HttpMessageConverter<?>> postProcessPartConverters(
					List<HttpMessageConverter<?>> converters) {
				converters.removeIf(MappingJackson2XmlHttpMessageConverter.class::isInstance);
				return converters;
			}

		};
		List<Class<?>> converterClasses = new ArrayList<>();
		for (HttpMessageConverter<?> converter : extractFormPartConverters(converters.getConverters())) {
			converterClasses.add(converter.getClass());
		}
		assertThat(converterClasses).containsExactly(ByteArrayHttpMessageConverter.class,
				StringHttpMessageConverter.class, ResourceHttpMessageConverter.class, SourceHttpMessageConverter.class,
				MappingJackson2HttpMessageConverter.class, MappingJackson2SmileHttpMessageConverter.class);
	}

	private List<HttpMessageConverter<?>> extractFormPartConverters(List<HttpMessageConverter<?>> converters) {
		AllEncompassingFormHttpMessageConverter formConverter = findFormConverter(converters);
		return formConverter.getPartConverters();
	}

	private AllEncompassingFormHttpMessageConverter findFormConverter(Collection<HttpMessageConverter<?>> converters) {
		for (HttpMessageConverter<?> converter : converters) {
			if (converter instanceof AllEncompassingFormHttpMessageConverter) {
				return (AllEncompassingFormHttpMessageConverter) converter;
			}
		}
		return null;
	}

}
