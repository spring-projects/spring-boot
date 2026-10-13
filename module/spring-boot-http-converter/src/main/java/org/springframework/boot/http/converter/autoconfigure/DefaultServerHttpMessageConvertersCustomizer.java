/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.converter.autoconfigure;

import java.util.Collection;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;

class DefaultServerHttpMessageConvertersCustomizer implements ServerHttpMessageConvertersCustomizer {

	private final Collection<HttpMessageConverter<?>> converters;

	DefaultServerHttpMessageConvertersCustomizer(Collection<HttpMessageConverter<?>> converters) {
		this.converters = converters;
	}

	@Override
	public void customize(ServerBuilder builder) {
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
