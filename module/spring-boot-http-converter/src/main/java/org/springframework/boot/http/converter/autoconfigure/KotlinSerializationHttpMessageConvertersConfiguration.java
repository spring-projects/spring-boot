/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.converter.autoconfigure;

import kotlinx.serialization.Serializable;
import kotlinx.serialization.json.Json;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.converter.HttpMessageConverters.ClientBuilder;
import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;
import org.springframework.util.ClassUtils;

/**
 * Configuration for HTTP message converters that use Kotlin Serialization.
 *
 * @author Brian Clozel
 * @author Dmitry Sulman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Serializable.class, Json.class })
@ConditionalOnBean(Json.class)
class KotlinSerializationHttpMessageConvertersConfiguration {

	@Bean
	@Order(0)
	@ConditionalOnMissingBean(KotlinSerializationJsonHttpMessageConverter.class)
	KotlinSerializationJsonConvertersCustomizer kotlinSerializationJsonConvertersCustomizer(Json json,
			ResourceLoader resourceLoader) {
		return new KotlinSerializationJsonConvertersCustomizer(json, resourceLoader);
	}

	static class KotlinSerializationJsonConvertersCustomizer
			implements ClientHttpMessageConvertersCustomizer, ServerHttpMessageConvertersCustomizer {

		private final KotlinSerializationJsonHttpMessageConverter converter;

		KotlinSerializationJsonConvertersCustomizer(Json json, ResourceLoader resourceLoader) {
			ClassLoader classLoader = resourceLoader.getClassLoader();
			boolean hasAnyJsonSupport = ClassUtils.isPresent("tools.jackson.databind.json.JsonMapper", classLoader)
					|| ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader)
					|| ClassUtils.isPresent("com.google.gson.Gson", classLoader);
			this.converter = hasAnyJsonSupport ? new KotlinSerializationJsonHttpMessageConverter(json)
					: new KotlinSerializationJsonHttpMessageConverter(json, (type) -> true);
		}

		@Override
		public void customize(ClientBuilder builder) {
			builder.withKotlinSerializationJsonConverter(this.converter);
		}

		@Override
		public void customize(ServerBuilder builder) {
			builder.withKotlinSerializationJsonConverter(this.converter);
		}

	}

}
