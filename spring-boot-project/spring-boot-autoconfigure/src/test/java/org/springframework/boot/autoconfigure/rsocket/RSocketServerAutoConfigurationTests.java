/*
 * Copyright 2012-2020 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.rsocket.context.RSocketPortInfoApplicationContextInitializer;
import org.springframework.boot.rsocket.context.RSocketServerBootstrap;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.boot.rsocket.server.RSocketServerFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RSocketServerAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Verónica Vásquez
 */
class RSocketServerAutoConfigurationTests {

	@Test
	void shouldNotCreateBeansByDefault() {
		contextRunner().run((context) -> assertThat(context).doesNotHaveBean(WebServerFactoryCustomizer.class)
				.doesNotHaveBean(RSocketServerFactory.class).doesNotHaveBean(RSocketServerBootstrap.class));
	}

	@Test
	void shouldNotCreateDefaultBeansForReactiveWebAppWithoutMapping() {
		reactiveWebContextRunner()
				.run((context) -> assertThat(context).doesNotHaveBean(WebServerFactoryCustomizer.class)
						.doesNotHaveBean(RSocketServerFactory.class).doesNotHaveBean(RSocketServerBootstrap.class));
	}

	@Test
	void shouldNotCreateDefaultBeansForReactiveWebAppWithWrongTransport() {
		reactiveWebContextRunner()
				.withPropertyValues("spring.rsocket.server.transport=tcp",
						"spring.rsocket.server.mapping-path=/rsocket")
				.run((context) -> assertThat(context).doesNotHaveBean(WebServerFactoryCustomizer.class)
						.doesNotHaveBean(RSocketServerFactory.class).doesNotHaveBean(RSocketServerBootstrap.class));
	}

	@Test
	void shouldCreateDefaultBeansForReactiveWebApp() {
		reactiveWebContextRunner()
				.withPropertyValues("spring.rsocket.server.transport=websocket",
						"spring.rsocket.server.mapping-path=/rsocket")
				.run((context) -> assertThat(context).hasSingleBean(RSocketWebSocketNettyRouteProvider.class));
	}

	@Test
	void shouldCreateDefaultBeansForRSocketServerWhenPortIsSet() {
		reactiveWebContextRunner().withPropertyValues("spring.rsocket.server.port=0")
				.run((context) -> assertThat(context).hasSingleBean(RSocketServerFactory.class)
						.hasSingleBean(RSocketServerBootstrap.class).hasSingleBean(RSocketServerCustomizer.class));
	}

	@Test
	void shouldSetLocalServerPortWhenRSocketServerPortIsSet() {
		reactiveWebContextRunner().withPropertyValues("spring.rsocket.server.port=0")
				.withInitializer(new RSocketPortInfoApplicationContextInitializer()).run((context) -> {
					assertThat(context).hasSingleBean(RSocketServerFactory.class)
							.hasSingleBean(RSocketServerBootstrap.class).hasSingleBean(RSocketServerCustomizer.class);
					assertThat(context.getEnvironment().getProperty("local.rsocket.server.port")).isNotNull();
				});
	}

	@Test
	void shouldUseCustomServerBootstrap() {
		contextRunner().withUserConfiguration(CustomServerBootstrapConfig.class).run((context) -> assertThat(context)
				.getBeanNames(RSocketServerBootstrap.class).containsExactly("customServerBootstrap"));
	}

	@Test
	void shouldUseCustomNettyRouteProvider() {
		reactiveWebContextRunner().withUserConfiguration(CustomNettyRouteProviderConfig.class)
				.withPropertyValues("spring.rsocket.server.transport=websocket",
						"spring.rsocket.server.mapping-path=/rsocket")
				.run((context) -> assertThat(context).getBeanNames(RSocketWebSocketNettyRouteProvider.class)
						.containsExactly("customNettyRouteProvider"));
	}

	private ApplicationContextRunner contextRunner() {
		return new ApplicationContextRunner().withUserConfiguration(BaseConfiguration.class)
				.withConfiguration(AutoConfigurations.of(RSocketServerAutoConfiguration.class));
	}

	private ReactiveWebApplicationContextRunner reactiveWebContextRunner() {
		return new ReactiveWebApplicationContextRunner().withUserConfiguration(BaseConfiguration.class)
				.withConfiguration(AutoConfigurations.of(RSocketServerAutoConfiguration.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		RSocketMessageHandler messageHandler() {
			RSocketMessageHandler messageHandler = new RSocketMessageHandler();
			messageHandler.setRSocketStrategies(RSocketStrategies.builder().encoder(CharSequenceEncoder.textPlainOnly())
					.decoder(StringDecoder.allMimeTypes()).build());
			return messageHandler;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomServerBootstrapConfig {

		@Bean
		RSocketServerBootstrap customServerBootstrap() {
			return mock(RSocketServerBootstrap.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomNettyRouteProviderConfig {

		@Bean
		RSocketWebSocketNettyRouteProvider customNettyRouteProvider() {
			return mock(RSocketWebSocketNettyRouteProvider.class);
		}

	}

}
