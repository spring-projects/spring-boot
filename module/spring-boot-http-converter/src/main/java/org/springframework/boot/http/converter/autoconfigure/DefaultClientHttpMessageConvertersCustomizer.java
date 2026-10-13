/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.converter.autoconfigure;

import java.util.Collection;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters.ClientBuilder;
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;

class DefaultClientHttpMessageConvertersCustomizer implements ClientHttpMessageConvertersCustomizer {

	private final Collection<HttpMessageConverter<?>> converters;

	DefaultClientHttpMessageConvertersCustomizer(Collection<HttpMessageConverter<?>> converters) {
		this.converters = converters;
	}

	@Override
	public void customize(ClientBuilder builder) {
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
