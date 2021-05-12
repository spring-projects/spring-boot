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

package org.springframework.boot.autoconfigure.websocket.servlet;

import javax.websocket.server.ServerContainer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebSocketServletAutoConfiguration}
 *
 * @author Andy Wilkinson
 */
class WebSocketServletAutoConfigurationTests {

	private AnnotationConfigServletWebServerApplicationContext context;

	@BeforeEach
	void createContext() {
		this.context = new AnnotationConfigServletWebServerApplicationContext();
	}

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void tomcatServerContainerIsAvailableFromTheServletContext() {
		serverContainerIsAvailableFromTheServletContext(TomcatConfiguration.class,
				WebSocketServletAutoConfiguration.TomcatWebSocketConfiguration.class);
	}

	@Test
	void jettyServerContainerIsAvailableFromTheServletContext() {
		serverContainerIsAvailableFromTheServletContext(JettyConfiguration.class,
				WebSocketServletAutoConfiguration.JettyWebSocketConfiguration.class);
	}

	private void serverContainerIsAvailableFromTheServletContext(Class<?>... configuration) {
		this.context.register(configuration);
		this.context.refresh();
		Object serverContainer = this.context.getServletContext()
				.getAttribute("javax.websocket.server.ServerContainer");
		assertThat(serverContainer).isInstanceOf(ServerContainer.class);

	}

	@Configuration(proxyBeanMethods = false)
	static class CommonConfiguration {

		@Bean
		WebServerFactoryCustomizerBeanPostProcessor ServletWebServerCustomizerBeanPostProcessor() {
			return new WebServerFactoryCustomizerBeanPostProcessor();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TomcatConfiguration extends CommonConfiguration {

		@Bean
		ServletWebServerFactory webServerFactory() {
			TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
			factory.setPort(0);
			return factory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JettyConfiguration extends CommonConfiguration {

		@Bean
		ServletWebServerFactory webServerFactory() {
			JettyServletWebServerFactory JettyServletWebServerFactory = new JettyServletWebServerFactory();
			JettyServletWebServerFactory.setPort(0);
			return JettyServletWebServerFactory;
		}

	}

}
