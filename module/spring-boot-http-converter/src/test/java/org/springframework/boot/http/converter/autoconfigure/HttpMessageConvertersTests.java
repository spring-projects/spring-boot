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

package org.springframework.boot.http.converter.autoconfigure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.cbor.JacksonCborHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpMessageConverters}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
@SuppressWarnings("deprecation")
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
				ResourceRegionHttpMessageConverter.class, AllEncompassingFormHttpMessageConverter.class,
				JacksonJsonHttpMessageConverter.class, JacksonCborHttpMessageConverter.class,
				JacksonXmlHttpMessageConverter.class);
	}

	@Test
	void addBeforeExistingConverter() {
		JacksonJsonHttpMessageConverter converter1 = new JacksonJsonHttpMessageConverter();
		JacksonJsonHttpMessageConverter converter2 = new JacksonJsonHttpMessageConverter();
		HttpMessageConverters converters = new HttpMessageConverters(converter1, converter2);
		assertThat(converters.getConverters()).contains(converter1);
		assertThat(converters.getConverters()).contains(converter2);
		List<JacksonJsonHttpMessageConverter> httpConverters = new ArrayList<>();
		for (HttpMessageConverter<?> candidate : converters) {
			if (candidate instanceof JacksonJsonHttpMessageConverter jsonConverter) {
				httpConverters.add(jsonConverter);
			}
		}
		// The existing converter is still there, but with a lower priority
		assertThat(httpConverters).hasSize(3);
		assertThat(httpConverters.indexOf(converter1)).isZero();
		assertThat(httpConverters.indexOf(converter2)).isOne();
		assertThat(converters.getConverters().indexOf(converter1)).isNotZero();
	}

	@Test
	void addBeforeExistingEquivalentConverter() {
		GsonHttpMessageConverter converter1 = new GsonHttpMessageConverter();
		HttpMessageConverters converters = new HttpMessageConverters(converter1);
		Stream<Class<?>> converterClasses = converters.getConverters().stream().map(HttpMessageConverter::getClass);
		assertThat(converterClasses).containsSequence(GsonHttpMessageConverter.class,
				JacksonJsonHttpMessageConverter.class);
	}

	@Test
	void addBeforeExistingAnotherEquivalentConverter() {
		KotlinSerializationJsonHttpMessageConverter converter1 = new KotlinSerializationJsonHttpMessageConverter();
		HttpMessageConverters converters = new HttpMessageConverters(converter1);
		Stream<Class<?>> converterClasses = converters.getConverters().stream().map(HttpMessageConverter::getClass);
		assertThat(converterClasses).containsSequence(KotlinSerializationJsonHttpMessageConverter.class,
				JacksonJsonHttpMessageConverter.class);
	}

	@Test
	void addBeforeExistingMultipleEquivalentConverters() {
		GsonHttpMessageConverter converter1 = new GsonHttpMessageConverter();
		KotlinSerializationJsonHttpMessageConverter converter2 = new KotlinSerializationJsonHttpMessageConverter();
		HttpMessageConverters converters = new HttpMessageConverters(converter1, converter2);
		Stream<Class<?>> converterClasses = converters.getConverters().stream().map(HttpMessageConverter::getClass);
		assertThat(converterClasses).containsSequence(GsonHttpMessageConverter.class,
				KotlinSerializationJsonHttpMessageConverter.class, JacksonJsonHttpMessageConverter.class);
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
				converters.removeIf(JacksonXmlHttpMessageConverter.class::isInstance);
				return converters;
			}

		};
		List<Class<?>> converterClasses = new ArrayList<>();
		for (HttpMessageConverter<?> converter : converters) {
			converterClasses.add(converter.getClass());
		}
		assertThat(converterClasses).containsExactly(ByteArrayHttpMessageConverter.class,
				StringHttpMessageConverter.class, ResourceHttpMessageConverter.class,
				ResourceRegionHttpMessageConverter.class, AllEncompassingFormHttpMessageConverter.class,
				JacksonJsonHttpMessageConverter.class, JacksonCborHttpMessageConverter.class);
	}

	@Test
	void postProcessPartConverters() {
		HttpMessageConverters converters = new HttpMessageConverters() {

			@Override
			protected List<HttpMessageConverter<?>> postProcessPartConverters(
					List<HttpMessageConverter<?>> converters) {
				converters.removeIf(JacksonXmlHttpMessageConverter.class::isInstance);
				return converters;
			}

		};
		List<Class<?>> converterClasses = new ArrayList<>();
		for (HttpMessageConverter<?> converter : extractFormPartConverters(converters.getConverters())) {
			converterClasses.add(converter.getClass());
		}
		assertThat(converterClasses).containsExactly(ByteArrayHttpMessageConverter.class,
				StringHttpMessageConverter.class, ResourceHttpMessageConverter.class,
				JacksonJsonHttpMessageConverter.class, JacksonCborHttpMessageConverter.class);
	}

	private List<HttpMessageConverter<?>> extractFormPartConverters(List<HttpMessageConverter<?>> converters) {
		AllEncompassingFormHttpMessageConverter formConverter = findFormConverter(converters);
		assertThat(formConverter).isNotNull();
		return formConverter.getPartConverters();
	}

	private @Nullable AllEncompassingFormHttpMessageConverter findFormConverter(
			Collection<HttpMessageConverter<?>> converters) {
		for (HttpMessageConverter<?> converter : converters) {
			if (converter instanceof AllEncompassingFormHttpMessageConverter allEncompassingConverter) {
				return allEncompassingConverter;
			}
		}
		return null;
	}

}
