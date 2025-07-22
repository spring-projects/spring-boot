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

import io.netty.buffer.PooledByteBufAllocator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.cbor.CBORFactory;
import tools.jackson.dataformat.cbor.CBORMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.context.annotation.Bean;
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
@AutoConfiguration(afterName = "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration")
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
	@ConditionalOnBean(JsonMapper.Builder.class)
	@ConditionalOnClass({ ObjectMapper.class, CBORFactory.class })
	protected static class JacksonCborStrategyConfiguration {

		private static final MediaType[] SUPPORTED_TYPES = { MediaType.APPLICATION_CBOR };

		@Bean
		@Order(0)
		RSocketStrategiesCustomizer jacksonCborRSocketStrategyCustomizer(JsonMapper.Builder builder) {
			return (strategy) -> {
				CBORMapper cborMapper = CBORMapper.builder().baseSettings(builder.baseSettings()).build();
				strategy.decoder(new JacksonCborDecoder(cborMapper, SUPPORTED_TYPES));
				strategy.encoder(new JacksonCborEncoder(cborMapper, SUPPORTED_TYPES));
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ObjectMapper.class)
	protected static class JacksonJsonStrategyConfiguration {

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
