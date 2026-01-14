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

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters.ClientBuilder;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;

@SuppressWarnings("deprecation")
class DefaultClientHttpMessageConvertersCustomizer implements ClientHttpMessageConvertersCustomizer {

	private static final String JACKSON2_JSON_CONVERTER =
			"org.springframework.http.converter.json.MappingJackson2HttpMessageConverter";

	private static final String JACKSON2_XML_CONVERTER =
			"org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter";

	private final @Nullable HttpMessageConverters legacyConverters;

	private final Collection<HttpMessageConverter<?>> converters;

	DefaultClientHttpMessageConvertersCustomizer(@Nullable HttpMessageConverters legacyConverters,
			Collection<HttpMessageConverter<?>> converters) {

		this.legacyConverters = legacyConverters;
		this.converters = converters;
	}

	@Override
	public void customize(ClientBuilder builder) {
		if (this.legacyConverters != null) {
			this.legacyConverters.forEach(builder::addCustomConverter);
		}
		else {
			builder.registerDefaults();
			this.converters.forEach((converter) -> {
				if (converter instanceof StringHttpMessageConverter) {
					builder.withStringConverter(converter);
				}
				else if (converter instanceof KotlinSerializationJsonHttpMessageConverter) {
					builder.withKotlinSerializationJsonConverter(converter);
				}
				else if (isJsonConverter(converter) && supportsMediaType(converter, MediaType.APPLICATION_JSON)) {
					builder.withJsonConverter(converter);
				}
				else if (isXmlConverter(converter) && supportsMediaType(converter, MediaType.APPLICATION_XML)) {
					builder.withXmlConverter(converter);
				}
				else {
					builder.addCustomConverter(converter);
				}
			});
		}
	}

	private static boolean supportsMediaType(HttpMessageConverter<?> converter, MediaType mediaType) {
		for (MediaType supportedMediaType : converter.getSupportedMediaTypes()) {
			if (supportedMediaType.equalsTypeAndSubtype(mediaType)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isJsonConverter(HttpMessageConverter<?> converter) {
		return converter.getClass().equals(JacksonJsonHttpMessageConverter.class)
				|| converter.getClass().getName().equals(JACKSON2_JSON_CONVERTER)
				|| converter.getClass().equals(GsonHttpMessageConverter.class)
				|| converter.getClass().equals(JsonbHttpMessageConverter.class);
	}

	private static boolean isXmlConverter(HttpMessageConverter<?> converter) {
		return converter.getClass().equals(JacksonXmlHttpMessageConverter.class)
				|| converter.getClass().getName().equals(JACKSON2_XML_CONVERTER)
				|| converter.getClass().equals(Jaxb2RootElementHttpMessageConverter.class);
	}

}
