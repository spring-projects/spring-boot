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

package org.springframework.boot.reactor.netty.autoconfigure;

import org.junit.jupiter.api.Test;
import reactor.netty.http.server.HttpServer;

import org.springframework.boot.reactor.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.reactor.netty.NettyServerCustomizer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.autoconfigure.reactive.AbstractReactiveWebServerAutoConfigurationTests;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NettyReactiveWebServerAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 * @author Madhura Bhave
 * @author Scott Frederick
 */
// @DirtiesUrlFactories
class NettyReactiveWebServerAutoConfigurationTests extends AbstractReactiveWebServerAutoConfigurationTests {

	NettyReactiveWebServerAutoConfigurationTests() {
		super(NettyReactiveWebServerAutoConfiguration.class);
	}

	@Test
	void nettyServerCustomizerBeanIsAddedToFactory() {
		this.serverRunner.withUserConfiguration(NettyServerCustomizerConfiguration.class).run((context) -> {
			NettyReactiveWebServerFactory factory = context.getBean(NettyReactiveWebServerFactory.class);
			assertThat(factory.getServerCustomizers())
				.contains(context.getBean("serverCustomizer", NettyServerCustomizer.class));
		});
	}

	@Test
	void nettyServerCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
		this.serverRunner.withUserConfiguration(DoubleRegistrationNettyServerCustomizerConfiguration.class)
			.run((context) -> {
				NettyReactiveWebServerFactory factory = context.getBean(NettyReactiveWebServerFactory.class);
				NettyServerCustomizer customizer = context.getBean("serverCustomizer", NettyServerCustomizer.class);
				assertThat(factory.getServerCustomizers()).contains(customizer);
				then(customizer).should().apply(any(HttpServer.class));
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class NettyServerCustomizerConfiguration {

		@Bean
		NettyServerCustomizer serverCustomizer() {
			return (server) -> server;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DoubleRegistrationNettyServerCustomizerConfiguration {

		private final NettyServerCustomizer customizer = mock(NettyServerCustomizer.class);

		DoubleRegistrationNettyServerCustomizerConfiguration() {
			given(this.customizer.apply(any(HttpServer.class))).willAnswer((invocation) -> invocation.getArgument(0));
		}

		@Bean
		NettyServerCustomizer serverCustomizer() {
			return this.customizer;
		}

		@Bean
		WebServerFactoryCustomizer<NettyReactiveWebServerFactory> nettyCustomizer() {
			return (netty) -> netty.addServerCustomizers(this.customizer);
		}

	}

}
