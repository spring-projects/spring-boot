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

import java.util.stream.Collectors;

import io.rsocket.RSocketFactory;
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
import org.springframework.boot.rsocket.netty.NettyRSocketServerFactory;
import org.springframework.boot.rsocket.server.RSocketServerBootstrap;
import org.springframework.boot.rsocket.server.RSocketServerFactory;
import org.springframework.boot.rsocket.server.ServerRSocketFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
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
@ConditionalOnClass({ RSocketFactory.class, RSocketStrategies.class, HttpServer.class, TcpServerTransport.class })
@ConditionalOnBean(RSocketMessageHandler.class)
@AutoConfigureAfter(RSocketStrategiesAutoConfiguration.class)
@EnableConfigurationProperties(RSocketProperties.class)
public class RSocketServerAutoConfiguration {

	@Conditional(OnRSocketWebServerCondition.class)
	@Configuration(proxyBeanMethods = false)
	static class WebFluxServerAutoConfiguration {

		@Bean
		RSocketWebSocketNettyRouteProvider rSocketWebsocketRouteProvider(RSocketProperties properties,
				RSocketMessageHandler messageHandler) {
			return new RSocketWebSocketNettyRouteProvider(properties.getServer().getMappingPath(),
					messageHandler.serverAcceptor());
		}

	}

	@ConditionalOnProperty(prefix = "spring.rsocket.server", name = "port")
	@Configuration(proxyBeanMethods = false)
	static class EmbeddedServerAutoConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ReactorResourceFactory reactorResourceFactory() {
			return new ReactorResourceFactory();
		}

		@Bean
		@ConditionalOnMissingBean
		RSocketServerFactory rSocketServerFactory(RSocketProperties properties, ReactorResourceFactory resourceFactory,
				ObjectProvider<ServerRSocketFactoryCustomizer> customizers) {
			NettyRSocketServerFactory factory = new NettyRSocketServerFactory();
			factory.setResourceFactory(resourceFactory);
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(properties.getServer().getAddress()).to(factory::setAddress);
			map.from(properties.getServer().getPort()).to(factory::setPort);
			factory.setServerCustomizers(customizers.orderedStream().collect(Collectors.toList()));
			return factory;
		}

		@Bean
		@ConditionalOnMissingBean
		RSocketServerBootstrap rSocketServerBootstrap(RSocketServerFactory rSocketServerFactory,
				RSocketMessageHandler rSocketMessageHandler) {
			return new RSocketServerBootstrap(rSocketServerFactory, rSocketMessageHandler.serverAcceptor());
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
