/*
 * Copyright 2012-2019 the original author or authors.
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
import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.RSocketFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.messaging.rsocket.RSocketStrategies;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RSocketStrategies}.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ RSocketFactory.class, RSocketStrategies.class,
		PooledByteBufAllocator.class })
@AutoConfigureAfter(JacksonAutoConfiguration.class)
public class RSocketStrategiesAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public RSocketStrategies rSocketStrategies(
			ObjectProvider<RSocketStrategiesCustomizer> customizers) {
		RSocketStrategies.Builder builder = RSocketStrategies.builder();
		builder.reactiveAdapterStrategy(ReactiveAdapterRegistry.getSharedInstance());
		customizers.stream().forEach((customizer) -> customizer.customize(builder));
		builder.dataBufferFactory(
				new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT));
		return builder.build();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ObjectMapper.class)
	protected static class JacksonStrategyConfiguration {

		@Bean
		@Order(0)
		@ConditionalOnBean(ObjectMapper.class)
		public RSocketStrategiesCustomizer jacksonStrategyCustomizer(
				ObjectMapper objectMapper) {
			return (strategy) -> {
				MediaType[] supportedTypes = new MediaType[] { MediaType.APPLICATION_JSON,
						new MediaType("application", "*+json") };
				strategy.decoder(new Jackson2JsonDecoder(objectMapper, supportedTypes));
				strategy.encoder(new Jackson2JsonEncoder(objectMapper, supportedTypes));
			};
		}

	}

}
