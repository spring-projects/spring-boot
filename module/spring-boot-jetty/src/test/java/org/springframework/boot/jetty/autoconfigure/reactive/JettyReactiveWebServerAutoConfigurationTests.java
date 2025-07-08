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

package org.springframework.boot.jetty.autoconfigure.reactive;

import jakarta.websocket.server.ServerContainer;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.junit.jupiter.api.Test;

import org.springframework.boot.jetty.JettyServerCustomizer;
import org.springframework.boot.jetty.JettyWebServer;
import org.springframework.boot.jetty.reactive.JettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.autoconfigure.reactive.AbstractReactiveWebServerAutoConfigurationTests;
import org.springframework.boot.web.server.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JettyReactiveWebServerAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 * @author Madhura Bhave
 * @author Scott Frederick
 */
class JettyReactiveWebServerAutoConfigurationTests extends AbstractReactiveWebServerAutoConfigurationTests {

	JettyReactiveWebServerAutoConfigurationTests() {
		super(JettyReactiveWebServerAutoConfiguration.class);
	}

	@Test
	void jettyServerCustomizerBeanIsAddedToFactory() {
		this.serverRunner.withUserConfiguration(JettyServerCustomizerConfiguration.class).run((context) -> {
			JettyReactiveWebServerFactory factory = context.getBean(JettyReactiveWebServerFactory.class);
			assertThat(factory.getServerCustomizers())
				.contains(context.getBean("serverCustomizer", JettyServerCustomizer.class));
		});
	}

	@Test
	void jettyServerCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
		this.serverRunner.withUserConfiguration(DoubleRegistrationJettyServerCustomizerConfiguration.class)
			.run((context) -> {
				JettyReactiveWebServerFactory factory = context.getBean(JettyReactiveWebServerFactory.class);
				JettyServerCustomizer customizer = context.getBean("serverCustomizer", JettyServerCustomizer.class);
				assertThat(factory.getServerCustomizers()).contains(customizer);
				then(customizer).should().customize(any(Server.class));
			});
	}

	@Test
	void webSocketServerContainerIsAvailableFromServletContext() {
		this.serverRunner.run((context) -> {
			WebServer webServer = ((ReactiveWebServerApplicationContext) context.getSourceApplicationContext())
				.getWebServer();
			ServletContextHandler servletContextHandler = (ServletContextHandler) ((StatisticsHandler) ((JettyWebServer) webServer)
				.getServer()
				.getHandler()).getHandler();
			Object serverContainer = servletContextHandler.getContext()
				.getAttribute("jakarta.websocket.server.ServerContainer");
			assertThat(serverContainer).isInstanceOf(ServerContainer.class);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class JettyServerCustomizerConfiguration {

		@Bean
		JettyServerCustomizer serverCustomizer() {
			return (server) -> {
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DoubleRegistrationJettyServerCustomizerConfiguration {

		private final JettyServerCustomizer customizer = mock(JettyServerCustomizer.class);

		@Bean
		JettyServerCustomizer serverCustomizer() {
			return this.customizer;
		}

		@Bean
		WebServerFactoryCustomizer<JettyReactiveWebServerFactory> jettyCustomizer() {
			return (jetty) -> jetty.addServerCustomizers(this.customizer);
		}

	}

}
