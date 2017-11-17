/*
 * Copyright 2012-2017 the original author or authors.
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

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.web.reactive.server.ConfigurableReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.server.reactive.HttpHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveWebServerAutoConfiguration}.
 *
 * @author Brian Clozel
 */
public class ReactiveWebServerAutoConfigurationTests {

	private AnnotationConfigReactiveWebServerApplicationContext context;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createFromConfigClass() {
		this.context = new AnnotationConfigReactiveWebServerApplicationContext(
				BaseConfiguration.class);
		assertThat(this.context.getBeansOfType(ReactiveWebServerFactory.class))
				.hasSize(1);
		assertThat(this.context.getBeansOfType(WebServerFactoryCustomizer.class))
				.hasSize(1);
		assertThat(this.context.getBeansOfType(DefaultReactiveWebServerCustomizer.class))
				.hasSize(1);
	}

	@Test
	public void missingHttpHandler() {
		this.thrown.expect(ApplicationContextException.class);
		this.thrown.expectMessage(Matchers.containsString("missing HttpHandler bean"));
		this.context = new AnnotationConfigReactiveWebServerApplicationContext(
				MissingHttpHandlerConfiguration.class);
	}

	@Test
	public void multipleHttpHandler() {
		this.thrown.expect(ApplicationContextException.class);
		this.thrown.expectMessage(Matchers.containsString(
				"multiple HttpHandler beans : httpHandler,additionalHttpHandler"));
		this.context = new AnnotationConfigReactiveWebServerApplicationContext(
				BaseConfiguration.class, TooManyHttpHandlers.class);
	}

	@Test
	public void customizeReactiveWebServer() {
		this.context = new AnnotationConfigReactiveWebServerApplicationContext(
				BaseConfiguration.class, ReactiveWebServerCustomization.class);
		MockReactiveWebServerFactory factory = this.context
				.getBean(MockReactiveWebServerFactory.class);
		assertThat(factory.getPort()).isEqualTo(9000);
	}

	@Configuration
	@Import({ MockWebServerAutoConfiguration.class,
			ReactiveWebServerAutoConfiguration.class })
	protected static class BaseConfiguration {

		@Bean
		public HttpHandler httpHandler() {
			return Mockito.mock(HttpHandler.class);
		}

	}

	@Configuration
	@Import({ MockWebServerAutoConfiguration.class,
			ReactiveWebServerAutoConfiguration.class })
	protected static class MissingHttpHandlerConfiguration {

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
			return (server) -> server.setPort(9000);
		}

	}

	@Configuration
	public static class MockWebServerAutoConfiguration {

		@Bean
		public MockReactiveWebServerFactory mockReactiveWebServerFactory() {
			return new MockReactiveWebServerFactory();
		}

	}

}
