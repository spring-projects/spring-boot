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

package org.springframework.boot.rsocket.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import io.netty.buffer.PooledByteBufAllocator;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.cbor.CBORMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.codec.cbor.JacksonCborDecoder;
import org.springframework.http.codec.cbor.JacksonCborEncoder;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.ClassUtils;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RSocketStrategies}.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration(afterName = { "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration",
		"org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration" })
@ConditionalOnClass({ io.rsocket.RSocket.class, RSocketStrategies.class, PooledByteBufAllocator.class })
public final class RSocketStrategiesAutoConfiguration {

	private static final String PATHPATTERN_ROUTEMATCHER_CLASS = "org.springframework.web.util.pattern.PathPatternRouteMatcher";

	@Bean
	@ConditionalOnMissingBean
	RSocketStrategies rSocketStrategies(ObjectProvider<RSocketStrategiesCustomizer> customizers) {
		RSocketStrategies.Builder builder = RSocketStrategies.builder();
		if (ClassUtils.isPresent(PATHPATTERN_ROUTEMATCHER_CLASS, null)) {
			builder.routeMatcher(new PathPatternRouteMatcher());
		}
		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(name = "spring.rsocket.preferred-mapper", havingValue = "jackson", matchIfMissing = true)
	static class JacksonStrategyConfiguration {

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass(CBORMapper.class)
		@ConditionalOnBean(CBORMapper.class)
		static class JacksonCborStrategyConfiguration {

			private static final MediaType[] SUPPORTED_TYPES = { MediaType.APPLICATION_CBOR };

			@Bean
			@Order(0)
			RSocketStrategiesCustomizer jacksonCborRSocketStrategyCustomizer(CBORMapper cborMapper) {
				return (strategy) -> {
					strategy.decoder(new JacksonCborDecoder(cborMapper, SUPPORTED_TYPES));
					strategy.encoder(new JacksonCborEncoder(cborMapper, SUPPORTED_TYPES));
				};
			}

		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass(JsonMapper.class)
		static class JacksonJsonStrategyConfiguration {

			private static final MediaType[] SUPPORTED_TYPES = { MediaType.APPLICATION_JSON,
					new MediaType("application", "*+json") };

			@Bean
			@Order(1)
			@ConditionalOnBean(JsonMapper.class)
			RSocketStrategiesCustomizer jacksonJsonRSocketStrategyCustomizer(JsonMapper jsonMapper) {
				return (strategy) -> {
					strategy.decoder(new JacksonJsonDecoder(jsonMapper, SUPPORTED_TYPES));
					strategy.encoder(new JacksonJsonEncoder(jsonMapper, SUPPORTED_TYPES));
				};
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(NoJacksonOrJackson2Preferred.class)
	@SuppressWarnings("removal")
	@Deprecated(since = "4.0.0", forRemoval = true)
	static class Jackson2StrategyConfiguration {

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass({ ObjectMapper.class, CBORFactory.class })
		static class Jackson2CborStrategyConfiguration {

			private static final MediaType[] SUPPORTED_TYPES = { MediaType.APPLICATION_CBOR };

			@Bean
			@Order(0)
			@ConditionalOnBean(org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.class)
			RSocketStrategiesCustomizer jackson2CborRSocketStrategyCustomizer(
					org.springframework.http.converter.json.Jackson2ObjectMapperBuilder builder) {
				return (strategy) -> {
					ObjectMapper objectMapper = builder.createXmlMapper(false)
						.factory(new com.fasterxml.jackson.dataformat.cbor.CBORFactory())
						.build();
					strategy.decoder(
							new org.springframework.http.codec.cbor.Jackson2CborDecoder(objectMapper, SUPPORTED_TYPES));
					strategy.encoder(
							new org.springframework.http.codec.cbor.Jackson2CborEncoder(objectMapper, SUPPORTED_TYPES));
				};
			}

		}

		@ConditionalOnClass(ObjectMapper.class)
		static class Jackson2JsonStrategyConfiguration {

			private static final MediaType[] SUPPORTED_TYPES = { MediaType.APPLICATION_JSON,
					new MediaType("application", "*+json") };

			@Bean
			@Order(1)
			@ConditionalOnBean(ObjectMapper.class)
			RSocketStrategiesCustomizer jackson2JsonRSocketStrategyCustomizer(ObjectMapper objectMapper) {
				return (strategy) -> {
					strategy.decoder(
							new org.springframework.http.codec.json.Jackson2JsonDecoder(objectMapper, SUPPORTED_TYPES));
					strategy.encoder(
							new org.springframework.http.codec.json.Jackson2JsonEncoder(objectMapper, SUPPORTED_TYPES));
				};
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

		@ConditionalOnProperty(name = "spring.rsocket.preferred-mapper", havingValue = "jackson2")
		static class Jackson2Preferred {

		}

	}

}
