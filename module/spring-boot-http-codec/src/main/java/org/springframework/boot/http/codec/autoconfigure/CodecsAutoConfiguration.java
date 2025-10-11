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

package org.springframework.boot.http.codec.autoconfigure;

import com.google.gson.Gson;
import kotlinx.serialization.Serializable;
import kotlinx.serialization.json.Json;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.http.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.CodecConfigurer.CustomCodecs;
import org.springframework.http.codec.json.GsonDecoder;
import org.springframework.http.codec.json.GsonEncoder;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.http.codec.json.KotlinSerializationJsonDecoder;
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link org.springframework.core.codec.Encoder Encoders} and
 * {@link org.springframework.core.codec.Decoder Decoders}.
 *
 * @author Brian Clozel
 * @author Vasily Pelikh
 * @since 2.0.0
 */
@AutoConfiguration(afterName = { "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration",
		"org.springframework.boot.gson.autoconfigure.GsonAutoConfiguration",
		"org.springframework.boot.kotlin.serialization.autoconfigure.KotlinSerializationAutoConfiguration" })
@ConditionalOnClass({ CodecConfigurer.class, WebClient.class })
public final class CodecsAutoConfiguration {

	private static final String PREFERRED_MAPPER_PROPERTY = "spring.http.codecs.preferred-json-mapper";

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JsonMapper.class)
	@ConditionalOnBean(JsonMapper.class)
	@ConditionalOnProperty(name = PREFERRED_MAPPER_PROPERTY, havingValue = "jackson", matchIfMissing = true)
	static class JacksonJsonCodecConfiguration {

		@Bean
		@Order(0)
		CodecCustomizer jacksonCodecCustomizer(JsonMapper jsonMapper) {
			return (configurer) -> {
				CodecConfigurer.DefaultCodecs defaults = configurer.defaultCodecs();
				defaults.jacksonJsonDecoder(new JacksonJsonDecoder(jsonMapper));
				defaults.jacksonJsonEncoder(new JacksonJsonEncoder(jsonMapper));
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Gson.class)
	@ConditionalOnBean(Gson.class)
	@ConditionalOnProperty(name = PREFERRED_MAPPER_PROPERTY, havingValue = "gson")
	static class GsonJsonCodecConfiguration {

		@Bean
		CodecCustomizer gsonCodecCustomizer(Gson gson) {
			return (configurer) -> {
				CustomCodecs customCodecs = configurer.customCodecs();
				customCodecs.registerWithDefaultConfig(new GsonDecoder(gson));
				customCodecs.registerWithDefaultConfig(new GsonEncoder(gson));
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ Serializable.class, Json.class })
	@ConditionalOnBean(Json.class)
	@ConditionalOnProperty(name = PREFERRED_MAPPER_PROPERTY, havingValue = "kotlin-serialization")
	static class KotlinSerializationJsonCodecConfiguration {

		@Bean
		@Order(-10) // configured ahead of JSON mappers
		CodecCustomizer kotlinSerializationCodecCustomizer(Json json) {
			return (configurer) -> {
				CustomCodecs customCodecs = configurer.customCodecs();
				customCodecs.registerWithDefaultConfig(new KotlinSerializationJsonDecoder(json));
				customCodecs.registerWithDefaultConfig(new KotlinSerializationJsonEncoder(json));
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(HttpCodecsProperties.class)
	static class DefaultCodecsConfiguration {

		@Bean
		DefaultCodecCustomizer defaultCodecCustomizer(HttpCodecsProperties httpCodecProperties) {
			return new DefaultCodecCustomizer(httpCodecProperties.isLogRequestDetails(),
					httpCodecProperties.getMaxInMemorySize());
		}

		static final class DefaultCodecCustomizer implements CodecCustomizer, Ordered {

			private final boolean logRequestDetails;

			private final @Nullable DataSize maxInMemorySize;

			DefaultCodecCustomizer(boolean logRequestDetails, @Nullable DataSize maxInMemorySize) {
				this.logRequestDetails = logRequestDetails;
				this.maxInMemorySize = maxInMemorySize;
			}

			@Override
			public void customize(CodecConfigurer configurer) {
				PropertyMapper map = PropertyMapper.get();
				CodecConfigurer.DefaultCodecs defaultCodecs = configurer.defaultCodecs();
				defaultCodecs.enableLoggingRequestDetails(this.logRequestDetails);
				map.from(this.maxInMemorySize).asInt(DataSize::toBytes).to(defaultCodecs::maxInMemorySize);
			}

			@Override
			public int getOrder() {
				return 0;
			}

		}

	}

}
