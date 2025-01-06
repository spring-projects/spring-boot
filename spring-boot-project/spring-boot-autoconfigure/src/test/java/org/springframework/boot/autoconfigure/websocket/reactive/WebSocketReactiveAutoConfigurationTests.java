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

package org.springframework.boot.autoconfigure.websocket.reactive;

import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.servlet.ServletContext;
import jakarta.websocket.server.ServerContainer;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.testsupport.classpath.ForkedClassPath;
import org.springframework.boot.testsupport.web.servlet.DirtiesUrlFactories;
import org.springframework.boot.web.embedded.jetty.JettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.jetty.JettyWebServer;
import org.springframework.boot.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebSocketReactiveAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
@DirtiesUrlFactories
class WebSocketReactiveAutoConfigurationTests {

	@ParameterizedTest(name = "{0}")
	@MethodSource("testConfiguration")
	@ForkedClassPath
	void serverContainerIsAvailableFromTheServletContext(String server,
			Function<AnnotationConfigReactiveWebServerApplicationContext, ServletContext> servletContextAccessor,
			Class<?>... configuration) {
		try (AnnotationConfigReactiveWebServerApplicationContext context = new AnnotationConfigReactiveWebServerApplicationContext(
				configuration)) {
			Object serverContainer = servletContextAccessor.apply(context)
				.getAttribute("jakarta.websocket.server.ServerContainer");
			assertThat(serverContainer).isInstanceOf(ServerContainer.class);
		}
	}

	static Stream<Arguments> testConfiguration() {
		return Stream.of(Arguments.of("Jetty",
				(Function<AnnotationConfigReactiveWebServerApplicationContext, ServletContext>) WebSocketReactiveAutoConfigurationTests::getJettyServletContext,
				new Class<?>[] { JettyConfiguration.class,
						WebSocketReactiveAutoConfiguration.JettyWebSocketConfiguration.class }),
				Arguments.of("Tomcat",
						(Function<AnnotationConfigReactiveWebServerApplicationContext, ServletContext>) WebSocketReactiveAutoConfigurationTests::getTomcatServletContext,
						new Class<?>[] { TomcatConfiguration.class,
								WebSocketReactiveAutoConfiguration.TomcatWebSocketConfiguration.class }));
	}

	private static ServletContext getJettyServletContext(AnnotationConfigReactiveWebServerApplicationContext context) {
		return ((ServletContextHandler) ((JettyWebServer) context.getWebServer()).getServer().getHandler())
			.getServletContext();
	}

	private static ServletContext getTomcatServletContext(AnnotationConfigReactiveWebServerApplicationContext context) {
		return findContext(((TomcatWebServer) context.getWebServer()).getTomcat()).getServletContext();
	}

	private static Context findContext(Tomcat tomcat) {
		for (Container child : tomcat.getHost().findChildren()) {
			if (child instanceof Context context) {
				return context;
			}
		}
		throw new IllegalStateException("The host does not contain a Context");
	}

	@Configuration(proxyBeanMethods = false)
	static class CommonConfiguration {

		@Bean
		static WebServerFactoryCustomizerBeanPostProcessor webServerFactoryCustomizerBeanPostProcessor() {
			return new WebServerFactoryCustomizerBeanPostProcessor();
		}

		@Bean
		HttpHandler echoHandler() {
			return (request, response) -> response.writeWith(request.getBody());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TomcatConfiguration extends CommonConfiguration {

		@Bean
		ReactiveWebServerFactory webServerFactory() {
			TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory();
			factory.setPort(0);
			return factory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JettyConfiguration extends CommonConfiguration {

		@Bean
		ReactiveWebServerFactory webServerFactory() {
			JettyReactiveWebServerFactory factory = new JettyReactiveWebServerFactory();
			factory.setPort(0);
			return factory;
		}

	}

}
