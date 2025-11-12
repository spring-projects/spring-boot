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

import com.fasterxml.jackson.databind.ObjectMapper;
import kotlinx.serialization.json.Json;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.http.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.http.codec.json.KotlinSerializationJsonDecoder;
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder;
import org.springframework.util.ClassUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link org.springframework.core.codec.Encoder Encoders} and
 * {@link org.springframework.core.codec.Decoder Decoders}.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration(afterName = { "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration",
		"org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration" })
@ConditionalOnClass({ CodecConfigurer.class, WebClient.class })
public final class CodecsAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JsonMapper.class)
	@ConditionalOnProperty(name = "spring.http.codecs.preferred-json-mapper", havingValue = "jackson",
			matchIfMissing = true)
	static class JacksonJsonCodecConfiguration {

		@Bean
		@Order(0)
		@ConditionalOnBean(JsonMapper.class)
		CodecCustomizer jacksonCodecCustomizer(JsonMapper jsonMapper) {
			return (configurer) -> {
				CodecConfigurer.DefaultCodecs defaults = configurer.defaultCodecs();
				defaults.jacksonJsonDecoder(new JacksonJsonDecoder(jsonMapper));
				defaults.jacksonJsonEncoder(new JacksonJsonEncoder(jsonMapper));
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ObjectMapper.class)
	@Conditional(NoJacksonOrJackson2Preferred.class)
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	static class Jackson2JsonCodecConfiguration {

		@Bean
		@Order(0)
		@ConditionalOnBean(ObjectMapper.class)
		CodecCustomizer jackson2CodecCustomizer(ObjectMapper objectMapper) {
			return (configurer) -> {
				CodecConfigurer.DefaultCodecs defaults = configurer.defaultCodecs();
				defaults.jacksonJsonDecoder(new org.springframework.http.codec.json.Jackson2JsonDecoder(objectMapper));
				defaults.jacksonJsonEncoder(new org.springframework.http.codec.json.Jackson2JsonEncoder(objectMapper));
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Json.class)
	static class KotlinxSerializationJsonCodecConfiguration {

		@Bean
		@ConditionalOnBean(Json.class)
		CodecCustomizer kotlinxJsonCodecCustomizer(Json json, ResourceLoader resourceLoader) {
			ClassLoader classLoader = resourceLoader.getClassLoader();
			boolean hasAnyJsonSupport = ClassUtils.isPresent("tools.jackson.databind.json.JsonMapper", classLoader)
					|| ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader)
					|| ClassUtils.isPresent("com.google.gson.Gson", classLoader);

			return (configurer) -> {
				CodecConfigurer.DefaultCodecs defaults = configurer.defaultCodecs();
				defaults.kotlinSerializationJsonEncoder(hasAnyJsonSupport ? new KotlinSerializationJsonEncoder(json)
						: new KotlinSerializationJsonEncoder(json, (type) -> true));
				defaults.kotlinSerializationJsonDecoder(hasAnyJsonSupport ? new KotlinSerializationJsonDecoder(json)
						: new KotlinSerializationJsonDecoder(json, (type) -> true));
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

	static class NoJacksonOrJackson2Preferred extends AnyNestedCondition {

		NoJacksonOrJackson2Preferred() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnMissingClass("tools.jackson.databind.json.JsonMapper")
		static class NoJackson {

		}

		@ConditionalOnProperty(name = "spring.http.codecs.preferred-json-mapper", havingValue = "jackson2")
		static class Jackson2Preferred {

		}

	}

}
