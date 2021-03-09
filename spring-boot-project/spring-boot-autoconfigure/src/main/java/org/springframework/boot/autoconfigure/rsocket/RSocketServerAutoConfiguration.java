/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.stream.Collectors;

import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.netty.http.server.HttpServer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.rsocket.context.RSocketServerBootstrap;
import org.springframework.boot.rsocket.netty.NettyRSocketServerFactory;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.boot.rsocket.server.RSocketServerFactory;
import org.springframework.boot.rsocket.server.ServerRSocketFactoryProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for RSocket servers. In the case of
 * {@link org.springframework.boot.WebApplicationType#REACTIVE}, the RSocket server is
 * added as a WebSocket endpoint on the existing
 * {@link org.springframework.boot.web.embedded.netty.NettyWebServer}. If a specific
 * server port is configured, a new standalone RSocket server is created.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ RSocketServer.class, RSocketStrategies.class, HttpServer.class, TcpServerTransport.class })
@ConditionalOnBean(RSocketMessageHandler.class)
@AutoConfigureAfter(RSocketStrategiesAutoConfiguration.class)
@EnableConfigurationProperties(RSocketProperties.class)
public class RSocketServerAutoConfiguration {

	@Conditional(OnRSocketWebServerCondition.class)
	@Configuration(proxyBeanMethods = false)
	static class WebFluxServerConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@SuppressWarnings("deprecation")
		RSocketWebSocketNettyRouteProvider rSocketWebsocketRouteProvider(RSocketProperties properties,
				RSocketMessageHandler messageHandler, ObjectProvider<ServerRSocketFactoryProcessor> processors,
				ObjectProvider<RSocketServerCustomizer> customizers) {
			return new RSocketWebSocketNettyRouteProvider(properties.getServer().getMappingPath(),
					messageHandler.responder(), processors.orderedStream(), customizers.orderedStream());
		}

	}

	@ConditionalOnProperty(prefix = "spring.rsocket.server", name = "port")
	@ConditionalOnClass(ReactorResourceFactory.class)
	@Configuration(proxyBeanMethods = false)
	static class EmbeddedServerConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ReactorResourceFactory reactorResourceFactory() {
			return new ReactorResourceFactory();
		}

		@Bean
		@ConditionalOnMissingBean
		@SuppressWarnings("deprecation")
		RSocketServerFactory rSocketServerFactory(RSocketProperties properties, ReactorResourceFactory resourceFactory,
				ObjectProvider<ServerRSocketFactoryProcessor> processors,
				ObjectProvider<RSocketServerCustomizer> customizers) {
			NettyRSocketServerFactory factory = new NettyRSocketServerFactory();
			factory.setResourceFactory(resourceFactory);
			factory.setTransport(properties.getServer().getTransport());
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(properties.getServer().getAddress()).to(factory::setAddress);
			map.from(properties.getServer().getPort()).to(factory::setPort);
			factory.setRSocketServerCustomizers(customizers.orderedStream().collect(Collectors.toList()));
			factory.setSocketFactoryProcessors(processors.orderedStream().collect(Collectors.toList()));
			return factory;
		}

		@Bean
		@ConditionalOnMissingBean
		RSocketServerBootstrap rSocketServerBootstrap(RSocketServerFactory rSocketServerFactory,
				RSocketMessageHandler rSocketMessageHandler) {
			return new RSocketServerBootstrap(rSocketServerFactory, rSocketMessageHandler.responder());
		}

		@Bean
		RSocketServerCustomizer frameDecoderRSocketServerCustomizer(RSocketMessageHandler rSocketMessageHandler) {
			return (server) -> {
				if (rSocketMessageHandler.getRSocketStrategies()
						.dataBufferFactory() instanceof NettyDataBufferFactory) {
					server.payloadDecoder(PayloadDecoder.ZERO_COPY);
				}
			};
		}

	}

	static class OnRSocketWebServerCondition extends AllNestedConditions {

		OnRSocketWebServerCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
		static class IsReactiveWebApplication {

		}

		@ConditionalOnProperty(prefix = "spring.rsocket.server", name = "port", matchIfMissing = true)
		static class HasNoPortConfigured {

		}

		@ConditionalOnProperty(prefix = "spring.rsocket.server", name = "mapping-path")
		static class HasMappingPathConfigured {

		}

		@ConditionalOnProperty(prefix = "spring.rsocket.server", name = "transport", havingValue = "websocket")
		static class HasWebsocketTransportConfigured {

		}

	}

}
