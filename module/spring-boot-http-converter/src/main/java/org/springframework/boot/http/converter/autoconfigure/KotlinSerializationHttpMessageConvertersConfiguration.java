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
