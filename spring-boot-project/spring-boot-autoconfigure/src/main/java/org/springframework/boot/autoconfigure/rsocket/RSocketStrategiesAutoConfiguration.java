/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.rsocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import io.netty.buffer.PooledByteBufAllocator;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.codec.cbor.Jackson2CborDecoder;
import org.springframework.http.codec.cbor.Jackson2CborEncoder;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.ClassUtils;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RSocketStrategies}.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@ConditionalOnClass({ io.rsocket.RSocket.class, RSocketStrategies.class, PooledByteBufAllocator.class })
public class RSocketStrategiesAutoConfiguration {

	private static final String PATHPATTERN_ROUTEMATCHER_CLASS = "org.springframework.web.util.pattern.PathPatternRouteMatcher";

	/**
	 * Creates and configures the RSocketStrategies bean.
	 * @param customizers the object provider for RSocketStrategiesCustomizer instances
	 * @return the configured RSocketStrategies bean
	 */
	@Bean
	@ConditionalOnMissingBean
	public RSocketStrategies rSocketStrategies(ObjectProvider<RSocketStrategiesCustomizer> customizers) {
		RSocketStrategies.Builder builder = RSocketStrategies.builder();
		if (ClassUtils.isPresent(PATHPATTERN_ROUTEMATCHER_CLASS, null)) {
			builder.routeMatcher(new PathPatternRouteMatcher());
		}
		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	/**
	 * JacksonCborStrategyConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ ObjectMapper.class, CBORFactory.class })
	protected static class JacksonCborStrategyConfiguration {

		private static final MediaType[] SUPPORTED_TYPES = { MediaType.APPLICATION_CBOR };

		/**
		 * Customizes the RSocket strategies to use Jackson CBOR encoding and decoding.
		 * This customizer is only applied if a bean of type Jackson2ObjectMapperBuilder
		 * is present. The customizer sets up the RSocket strategies to use a Jackson
		 * ObjectMapper configured with CBOR factory. It configures the RSocket decoder to
		 * use Jackson2CborDecoder and the encoder to use Jackson2CborEncoder.
		 * @param builder the Jackson2ObjectMapperBuilder bean used to create the Jackson
		 * ObjectMapper
		 * @return the RSocketStrategiesCustomizer that applies the customizations to the
		 * RSocket strategies
		 */
		@Bean
		@Order(0)
		@ConditionalOnBean(Jackson2ObjectMapperBuilder.class)
		public RSocketStrategiesCustomizer jacksonCborRSocketStrategyCustomizer(Jackson2ObjectMapperBuilder builder) {
			return (strategy) -> {
				ObjectMapper objectMapper = builder.createXmlMapper(false).factory(new CBORFactory()).build();
				strategy.decoder(new Jackson2CborDecoder(objectMapper, SUPPORTED_TYPES));
				strategy.encoder(new Jackson2CborEncoder(objectMapper, SUPPORTED_TYPES));
			};
		}

	}

	/**
	 * JacksonJsonStrategyConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ObjectMapper.class)
	protected static class JacksonJsonStrategyConfiguration {

		private static final MediaType[] SUPPORTED_TYPES = { MediaType.APPLICATION_JSON,
				new MediaType("application", "*+json") };

		/**
		 * Customizes the RSocket strategies to use Jackson JSON encoding and decoding.
		 * This customizer is only applied if an ObjectMapper bean is present. The
		 * customizer sets the Jackson2JsonDecoder and Jackson2JsonEncoder as the decoder
		 * and encoder respectively, using the provided ObjectMapper and supported types.
		 * @param objectMapper the ObjectMapper bean used for JSON encoding and decoding
		 * @return the RSocketStrategiesCustomizer that applies the Jackson JSON strategy
		 * customization
		 */
		@Bean
		@Order(1)
		@ConditionalOnBean(ObjectMapper.class)
		public RSocketStrategiesCustomizer jacksonJsonRSocketStrategyCustomizer(ObjectMapper objectMapper) {
			return (strategy) -> {
				strategy.decoder(new Jackson2JsonDecoder(objectMapper, SUPPORTED_TYPES));
				strategy.encoder(new Jackson2JsonEncoder(objectMapper, SUPPORTED_TYPES));
			};
		}

	}

}
