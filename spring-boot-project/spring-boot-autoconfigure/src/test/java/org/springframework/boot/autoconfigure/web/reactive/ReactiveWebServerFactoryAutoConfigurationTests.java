/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.web.reactive;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.web.reactive.server.ConfigurableReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveWebServerFactoryAutoConfiguration}.
 *
 * @author Brian Clozel
 */
public class ReactiveWebServerFactoryAutoConfigurationTests {

	private ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner(
			AnnotationConfigReactiveWebServerApplicationContext::new)
					.withConfiguration(AutoConfigurations
							.of(ReactiveWebServerFactoryAutoConfiguration.class));

	@Test
	public void createFromConfigClass() {
		this.contextRunner.withUserConfiguration(MockWebServerConfiguration.class,
				HttpHandlerConfiguration.class).run((context) -> {
					assertThat(context.getBeansOfType(ReactiveWebServerFactory.class))
							.hasSize(1);
					assertThat(context.getBeansOfType(WebServerFactoryCustomizer.class))
							.hasSize(1);
					assertThat(context
							.getBeansOfType(ReactiveWebServerFactoryCustomizer.class))
									.hasSize(1);
				});
	}

	@Test
	public void missingHttpHandler() {
		this.contextRunner.withUserConfiguration(MockWebServerConfiguration.class)
				.run((context) -> assertThat(context.getStartupFailure())
						.isInstanceOf(ApplicationContextException.class)
						.hasMessageContaining("missing HttpHandler bean"));
	}

	@Test
	public void multipleHttpHandler() {
		this.contextRunner
				.withUserConfiguration(MockWebServerConfiguration.class,
						HttpHandlerConfiguration.class, TooManyHttpHandlers.class)
				.run((context) -> assertThat(context.getStartupFailure())
						.isInstanceOf(ApplicationContextException.class)
						.hasMessageContaining("multiple HttpHandler beans : "
								+ "httpHandler,additionalHttpHandler"));
	}

	@Test
	public void customizeReactiveWebServer() {
		this.contextRunner.withUserConfiguration(MockWebServerConfiguration.class,
				HttpHandlerConfiguration.class, ReactiveWebServerCustomization.class)
				.run((context) -> assertThat(
						context.getBean(MockReactiveWebServerFactory.class).getPort())
								.isEqualTo(9000));
	}

	@Test
	public void defaultWebServerIsTomcat() {
		// Tomcat should be chosen over Netty if the Tomcat library is present.
		this.contextRunner.withUserConfiguration(HttpHandlerConfiguration.class)
				.withPropertyValues("server.port=0")
				.run((context) -> assertThat(
						context.getBean(ReactiveWebServerFactory.class))
								.isInstanceOf(TomcatReactiveWebServerFactory.class));
	}

	@Configuration
	protected static class HttpHandlerConfiguration {

		@Bean
		public HttpHandler httpHandler() {
			return Mockito.mock(HttpHandler.class);
		}

	}

	@Configuration
	protected static class TooManyHttpHandlers {

		@Bean
		public HttpHandler additionalHttpHandler() {
			return Mockito.mock(HttpHandler.class);
		}

	}

	@Configuration
	protected static class ReactiveWebServerCustomization {

		@Bean
		public WebServerFactoryCustomizer<ConfigurableReactiveWebServerFactory> reactiveWebServerCustomizer() {
			return (factory) -> factory.setPort(9000);
		}

	}

	@Configuration
	public static class MockWebServerConfiguration {

		@Bean
		public MockReactiveWebServerFactory mockReactiveWebServerFactory() {
			return new MockReactiveWebServerFactory();
		}

	}

}
