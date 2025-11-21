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

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link DefaultServerHttpMessageConvertersCustomizer}.
 *
 * @author Tommy Karlsson
 */
@SuppressWarnings("deprecation")
class DefaultServerHttpMessageConvertersCustomizerTests {

	@Test
	void customizeWithTwoJsonConvertersConfiguresFirstAsJsonConverterAndSecondAsCustomConverter() {
		// Create two JSON converters
		JacksonJsonHttpMessageConverter jsonConverter1 = new JacksonJsonHttpMessageConverter();
		GsonHttpMessageConverter jsonConverter2 = new GsonHttpMessageConverter();

		// Create the customizer with both converters
		List<HttpMessageConverter<?>> converters = List.of(jsonConverter1, jsonConverter2);
		DefaultServerHttpMessageConvertersCustomizer customizer = new DefaultServerHttpMessageConvertersCustomizer(null,
				converters);

		// Mock the ServerBuilder
		ServerBuilder builder = mock(ServerBuilder.class);

		// Execute the customize method
		customizer.customize(builder);

		// Verify that registerDefaults was called
		then(builder).should().registerDefaults();

		// Verify that the first JSON converter is configured as the JSON converter
		then(builder).should(times(1)).withJsonConverter(jsonConverter1);

		// Verify that the second JSON converter is configured as a custom converter
		// (since there should only be one JSON converter registered via withJsonConverter)
		then(builder).should(times(1)).addCustomConverter(jsonConverter2);

		// Verify that withJsonConverter was only called once (for the first converter)
		then(builder).should(times(1)).withJsonConverter(any());
	}

	@Test
	void customizeWithJsonConverterVerifiesJsonConverterConfiguration() {
		// Create a single JSON converter
		JacksonJsonHttpMessageConverter jsonConverter = new JacksonJsonHttpMessageConverter();

		// Verify it supports JSON
		boolean supportsJson = jsonConverter.getSupportedMediaTypes()
			.stream()
			.anyMatch(mediaType -> mediaType.equalsTypeAndSubtype(MediaType.APPLICATION_JSON));
		assert supportsJson : "Converter should support APPLICATION_JSON";

		// Create the customizer
		List<HttpMessageConverter<?>> converters = List.of(jsonConverter);
		DefaultServerHttpMessageConvertersCustomizer customizer = new DefaultServerHttpMessageConvertersCustomizer(null,
				converters);

		// Mock the ServerBuilder
		ServerBuilder builder = mock(ServerBuilder.class);

		// Execute the customize method
		customizer.customize(builder);

		// Verify that registerDefaults was called
		then(builder).should().registerDefaults();

		// Verify that withJsonConverter was called with the JSON converter
		then(builder).should().withJsonConverter(jsonConverter);

		// Verify that addCustomConverter was not called for the JSON converter
		then(builder).should(times(0)).addCustomConverter(any());
	}

	@Test
	void customizeWithNonJsonConverterConfiguresAsCustomConverter() {
		// Create a custom converter that doesn't support JSON
		HttpMessageConverter<?> customConverter = mock(HttpMessageConverter.class);

		// Create the customizer
		List<HttpMessageConverter<?>> converters = List.of(customConverter);
		DefaultServerHttpMessageConvertersCustomizer customizer = new DefaultServerHttpMessageConvertersCustomizer(null,
				converters);

		// Mock the ServerBuilder
		ServerBuilder builder = mock(ServerBuilder.class);

		// Execute the customize method
		customizer.customize(builder);

		// Verify that registerDefaults was called
		then(builder).should().registerDefaults();

		// Verify that addCustomConverter was called with the custom converter
		then(builder).should().addCustomConverter(customConverter);

		// Verify that withJsonConverter was not called
		then(builder).should(times(0)).withJsonConverter(any());
	}

}