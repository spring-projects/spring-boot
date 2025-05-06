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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.http.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.util.MimeType;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link org.springframework.core.codec.Encoder Encoders} and
 * {@link org.springframework.core.codec.Decoder Decoders}.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
@AutoConfiguration(afterName = "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration")
@ConditionalOnClass({ CodecConfigurer.class, WebClient.class })
public class CodecsAutoConfiguration {

	private static final MimeType[] EMPTY_MIME_TYPES = {};

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ObjectMapper.class)
	@SuppressWarnings({ "removal", "deprecation" })
	static class JacksonCodecConfiguration {

		@Bean
		@Order(0)
		@ConditionalOnBean(ObjectMapper.class)
		CodecCustomizer jacksonCodecCustomizer(ObjectMapper objectMapper) {
			return (configurer) -> {
				CodecConfigurer.DefaultCodecs defaults = configurer.defaultCodecs();
				defaults.jackson2JsonDecoder(
						new org.springframework.http.codec.json.Jackson2JsonDecoder(objectMapper, EMPTY_MIME_TYPES));
				defaults.jackson2JsonEncoder(
						new org.springframework.http.codec.json.Jackson2JsonEncoder(objectMapper, EMPTY_MIME_TYPES));
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(HttpCodecsProperties.class)
	static class DefaultCodecsConfiguration {

		@Bean
		DefaultCodecCustomizer defaultCodecCustomizer(HttpCodecsProperties httpCodecProperties,
				Environment environment) {
			return new DefaultCodecCustomizer(httpCodecProperties.isLogRequestDetails(),
					httpCodecProperties.getMaxInMemorySize());
		}

		static final class DefaultCodecCustomizer implements CodecCustomizer, Ordered {

			private final boolean logRequestDetails;

			private final DataSize maxInMemorySize;

			DefaultCodecCustomizer(boolean logRequestDetails, DataSize maxInMemorySize) {
				this.logRequestDetails = logRequestDetails;
				this.maxInMemorySize = maxInMemorySize;
			}

			@Override
			public void customize(CodecConfigurer configurer) {
				PropertyMapper map = PropertyMapper.get();
				CodecConfigurer.DefaultCodecs defaultCodecs = configurer.defaultCodecs();
				defaultCodecs.enableLoggingRequestDetails(this.logRequestDetails);
				map.from(this.maxInMemorySize)
					.whenNonNull()
					.asInt(DataSize::toBytes)
					.to(defaultCodecs::maxInMemorySize);
			}

			@Override
			public int getOrder() {
				return 0;
			}

		}

	}

}
