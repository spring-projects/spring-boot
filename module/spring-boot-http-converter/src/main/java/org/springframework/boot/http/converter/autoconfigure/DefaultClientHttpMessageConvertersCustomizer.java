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
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;

@SuppressWarnings("deprecation")
class DefaultClientHttpMessageConvertersCustomizer implements ClientHttpMessageConvertersCustomizer {

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
			this.legacyConverters.forEach(builder::customMessageConverter);
		}
		else {
			builder.registerDefaults();
			this.converters.forEach((converter) -> {
				if (converter instanceof StringHttpMessageConverter) {
					builder.stringMessageConverter(converter);
				}
				else if (converter instanceof KotlinSerializationJsonHttpMessageConverter) {
					builder.customMessageConverter(converter);
				}
				else if (converter.getSupportedMediaTypes().contains(MediaType.APPLICATION_JSON)) {
					builder.jsonMessageConverter(converter);
				}
				else {
					builder.customMessageConverter(converter);
				}
			});
		}
	}

}
