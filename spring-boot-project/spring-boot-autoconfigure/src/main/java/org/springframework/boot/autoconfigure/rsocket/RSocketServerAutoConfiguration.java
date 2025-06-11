/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.function.Consumer;

import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.WebsocketServerSpec.Builder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.reactor.netty.ReactorNettyConfigurations;
import org.springframework.boot.autoconfigure.rsocket.RSocketProperties.Server.Spec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.rsocket.context.RSocketServerBootstrap;
import org.springframework.boot.rsocket.netty.NettyRSocketServerFactory;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.boot.rsocket.server.RSocketServerFactory;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.util.unit.DataSize;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for RSocket servers. In the case of
 * {@link org.springframework.boot.WebApplicationType#REACTIVE}, the RSocket server is
 * added as a WebSocket endpoint on the existing
 * {@link org.springframework.boot.web.embedded.netty.NettyWebServer}. If a specific
 * server port is configured, a new standalone RSocket server is created.
 *
 * @author Brian Clozel
 * @author Scott Frederick
 * @since 2.2.0
 */
@AutoConfiguration(after = RSocketStrategiesAutoConfiguration.class)
@ConditionalOnClass({ RSocketServer.class, RSocketStrategies.class, HttpServer.class, TcpServerTransport.class })
@ConditionalOnBean(RSocketMessageHandler.class)
@EnableConfigurationProperties(RSocketProperties.class)
public class RSocketServerAutoConfiguration {

	@Conditional(OnRSocketWebServerCondition.class)
	@Configuration(proxyBeanMethods = false)
	static class WebFluxServerConfiguration {

		@Bean
		@ConditionalOnMissingBean
		RSocketWebSocketNettyRouteProvider rSocketWebsocketRouteProvider(RSocketProperties properties,
				RSocketMessageHandler messageHandler, ObjectProvider<RSocketServerCustomizer> customizers) {
			return new RSocketWebSocketNettyRouteProvider(properties.getServer().getMappingPath(),
					messageHandler.responder(), customizeWebsocketServerSpec(properties.getServer().getSpec()),
					customizers.orderedStream());
		}

		private Consumer<Builder> customizeWebsocketServerSpec(Spec spec) {
			return (builder) -> {
				PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
				map.from(spec.getProtocols()).to(builder::protocols);
				map.from(spec.getMaxFramePayloadLength()).asInt(DataSize::toBytes).to(builder::maxFramePayloadLength);
				map.from(spec.isHandlePing()).to(builder::handlePing);
				map.from(spec.isCompress()).to(builder::compress);
			};
		}

	}

	@ConditionalOnProperty("spring.rsocket.server.port")
	@ConditionalOnClass(ReactorResourceFactory.class)
	@Configuration(proxyBeanMethods = false)
	@Import(ReactorNettyConfigurations.ReactorResourceFactoryConfiguration.class)
	static class EmbeddedServerConfiguration {

		@Bean
		@ConditionalOnMissingBean
		RSocketServerFactory rSocketServerFactory(RSocketProperties properties, ReactorResourceFactory resourceFactory,
				ObjectProvider<RSocketServerCustomizer> customizers, ObjectProvider<SslBundles> sslBundles) {
			NettyRSocketServerFactory factory = new NettyRSocketServerFactory();
			factory.setResourceFactory(resourceFactory);
			factory.setTransport(properties.getServer().getTransport());
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(properties.getServer().getAddress()).to(factory::setAddress);
			map.from(properties.getServer().getPort()).to(factory::setPort);
			map.from(properties.getServer().getFragmentSize()).to(factory::setFragmentSize);
			map.from(properties.getServer().getSsl()).to(factory::setSsl);
			factory.setSslBundles(sslBundles.getIfAvailable());
			factory.setRSocketServerCustomizers(customizers.orderedStream().toList());
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

		@ConditionalOnWebApplication(type = Type.REACTIVE)
		static class IsReactiveWebApplication {

		}

		@ConditionalOnProperty(name = "spring.rsocket.server.port", matchIfMissing = true)
		static class HasNoPortConfigured {

		}

		@ConditionalOnProperty("spring.rsocket.server.mapping-path")
		static class HasMappingPathConfigured {

		}

		@ConditionalOnProperty(name = "spring.rsocket.server.transport", havingValue = "websocket")
		static class HasWebsocketTransportConfigured {

		}

	}

}
