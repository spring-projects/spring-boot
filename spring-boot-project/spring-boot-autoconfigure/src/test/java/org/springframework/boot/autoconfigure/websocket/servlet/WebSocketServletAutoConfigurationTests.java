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

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpoint;
import org.eclipse.jetty.ee10.websocket.servlet.WebSocketUpgradeFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testsupport.classpath.ForkedClassPath;
import org.springframework.boot.testsupport.web.servlet.DirtiesUrlFactories;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

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

	@ParameterizedTest(name = "{0}")
	@MethodSource("testConfiguration")
	@ForkedClassPath
	void webSocketUpgradeDoesNotPreventAFilterFromRejectingTheRequest(String server, Class<?>... configuration)
			throws DeploymentException {
		try (AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext(
				configuration)) {
			ServerContainer serverContainer = (ServerContainer) context.getServletContext()
				.getAttribute("jakarta.websocket.server.ServerContainer");
			serverContainer.addEndpoint(TestEndpoint.class);
			WebServer webServer = context.getWebServer();
			int port = webServer.getPort();
			TestRestTemplate rest = new TestRestTemplate();
			RequestEntity<Void> request = RequestEntity.get("http://localhost:" + port)
				.header("Upgrade", "websocket")
				.header("Connection", "upgrade")
				.header("Sec-WebSocket-Version", "13")
				.header("Sec-WebSocket-Key", "key")
				.build();
			ResponseEntity<Void> response = rest.exchange(request, Void.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		}
	}

	@Test
	@SuppressWarnings("rawtypes")
	void whenCustomUpgradeFilterRegistrationIsDefinedAutoConfiguredRegistrationOfJettyUpgradeFilterBacksOff() {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(JettyConfiguration.class,
					WebSocketServletAutoConfiguration.JettyWebSocketConfiguration.class))
			.withUserConfiguration(CustomUpgradeFilterRegistrationConfiguration.class)
			.run((context) -> {
				Map<String, FilterRegistrationBean> filterRegistrations = context
					.getBeansOfType(FilterRegistrationBean.class);
				assertThat(filterRegistrations).containsOnlyKeys("unauthorizedFilter",
						"customUpgradeFilterRegistration");
			});
	}

	@Test
	@SuppressWarnings("rawtypes")
	void whenCustomUpgradeFilterIsDefinedAutoConfiguredRegistrationOfJettyUpgradeFilterBacksOff() {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(JettyConfiguration.class,
					WebSocketServletAutoConfiguration.JettyWebSocketConfiguration.class))
			.withUserConfiguration(CustomUpgradeFilterConfiguration.class)
			.run((context) -> {
				Map<String, FilterRegistrationBean> filterRegistrations = context
					.getBeansOfType(FilterRegistrationBean.class);
				assertThat(filterRegistrations).containsOnlyKeys("unauthorizedFilter");
			});
	}

	static Stream<Arguments> testConfiguration() {
		String response = "Tomcat";
		return Stream.of(
				Arguments.of("Jetty",
						new Class<?>[] { JettyConfiguration.class, DispatcherServletAutoConfiguration.class,
								WebSocketServletAutoConfiguration.JettyWebSocketConfiguration.class }),
				Arguments.of(response,
						new Class<?>[] { TomcatConfiguration.class, DispatcherServletAutoConfiguration.class,
								WebSocketServletAutoConfiguration.TomcatWebSocketConfiguration.class }));
	}

	@Configuration(proxyBeanMethods = false)
	static class CommonConfiguration {

		@Bean
		FilterRegistrationBean<Filter> unauthorizedFilter() {
			FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(new Filter() {

				@Override
				public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
						throws IOException, ServletException {
					((HttpServletResponse) response).sendError(HttpStatus.UNAUTHORIZED.value());
				}

			});
			registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
			registration.addUrlPatterns("/*");
			registration.setDispatcherTypes(DispatcherType.REQUEST);
			return registration;
		}

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

	@Configuration(proxyBeanMethods = false)
	static class CustomUpgradeFilterRegistrationConfiguration {

		@Bean
		FilterRegistrationBean<WebSocketUpgradeFilter> customUpgradeFilterRegistration() {
			FilterRegistrationBean<WebSocketUpgradeFilter> registration = new FilterRegistrationBean<>(
					new WebSocketUpgradeFilter());
			return registration;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomUpgradeFilterConfiguration {

		@Bean
		WebSocketUpgradeFilter customUpgradeFilter() {
			return new WebSocketUpgradeFilter();
		}

	}

	@ServerEndpoint("/")
	public static class TestEndpoint {

	}

}
