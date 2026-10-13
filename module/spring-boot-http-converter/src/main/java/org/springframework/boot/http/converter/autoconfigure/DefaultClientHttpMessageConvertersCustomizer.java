/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.converter.autoconfigure;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters.ClientBuilder;
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
			this.legacyConverters.forEach(builder::addCustomConverter);
		}
		else {
			builder.registerDefaults();
			this.converters.forEach((converter) -> {
				if (converter instanceof KotlinSerializationJsonHttpMessageConverter) {
					builder.withKotlinSerializationJsonConverter(converter);
				}
				else {
					builder.addCustomConverter(converter);
				}
			});
		}
	}

}
