/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.stream.Stream;

import jakarta.websocket.server.ServerContainer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.testsupport.classpath.ForkedClassPath;
import org.springframework.boot.testsupport.web.servlet.DirtiesUrlFactories;
import org.springframework.boot.testsupport.web.servlet.Servlet5ClassPathOverrides;
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
@DirtiesUrlFactories
class WebSocketServletAutoConfigurationTests {

	@ParameterizedTest(name = "{0}")
	@MethodSource("testConfiguration")
	@ForkedClassPath
	void serverContainerIsAvailableFromTheServletContext(String server, Class<?>... configuration) {
		try (AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext(
				configuration)) {
			Object serverContainer = context.getServletContext()
				.getAttribute("jakarta.websocket.server.ServerContainer");
			assertThat(serverContainer).isInstanceOf(ServerContainer.class);
		}
	}

	static Stream<Arguments> testConfiguration() {
		return Stream.of(
				Arguments.of("Jetty",
						new Class<?>[] { JettyConfiguration.class,
								WebSocketServletAutoConfiguration.JettyWebSocketConfiguration.class }),
				Arguments.of("Tomcat", new Class<?>[] { TomcatConfiguration.class,
						WebSocketServletAutoConfiguration.TomcatWebSocketConfiguration.class }));
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

	@Servlet5ClassPathOverrides
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
