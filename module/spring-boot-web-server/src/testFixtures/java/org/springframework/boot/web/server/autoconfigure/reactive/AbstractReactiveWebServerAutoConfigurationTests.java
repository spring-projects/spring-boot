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

package org.springframework.boot.web.server.autoconfigure.reactive;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.reactive.ConfigurableReactiveWebServerFactory;
import org.springframework.boot.web.server.reactive.MockReactiveWebServerFactory;
import org.springframework.boot.web.server.reactive.ReactiveWebServerFactory;
import org.springframework.boot.web.server.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Base class for testing sub-classes of {@link ReactiveWebServerConfiguration}.
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 * @author Madhura Bhave
 * @author Scott Frederick
 */
// @DirtiesUrlFactories
public abstract class AbstractReactiveWebServerAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner mockServerRunner;

	protected final ReactiveWebApplicationContextRunner serverRunner;

	protected AbstractReactiveWebServerAutoConfigurationTests(Class<?> serverAutoConfiguration) {
		ReactiveWebApplicationContextRunner common = new ReactiveWebApplicationContextRunner(
				AnnotationConfigReactiveWebServerApplicationContext::new)
			.withConfiguration(AutoConfigurations.of(serverAutoConfiguration));
		this.serverRunner = common.withPropertyValues("server.port=0")
			.withUserConfiguration(HttpHandlerConfiguration.class);
		this.mockServerRunner = common.withUserConfiguration(MockWebServerConfiguration.class);
	}

	@Test
	void createFromConfigClass() {
		this.mockServerRunner.withUserConfiguration(MockWebServerConfiguration.class, HttpHandlerConfiguration.class)
			.run((context) -> {
				assertThat(context.getBeansOfType(ReactiveWebServerFactory.class)).hasSize(1);
				assertThat(context.getBeansOfType(WebServerFactoryCustomizer.class)).hasSizeGreaterThanOrEqualTo(1);
				assertThat(context.getBeansOfType(ReactiveWebServerFactoryCustomizer.class)).hasSize(1);
			});
	}

	@Test
	void missingHttpHandler() {
		this.mockServerRunner.withUserConfiguration(MockWebServerConfiguration.class)
			.run((context) -> assertThat(context.getStartupFailure()).isInstanceOf(ApplicationContextException.class)
				.rootCause()
				.hasMessageContaining("missing HttpHandler bean"));
	}

	@Test
	void multipleHttpHandler() {
		this.mockServerRunner
			.withUserConfiguration(MockWebServerConfiguration.class, HttpHandlerConfiguration.class,
					TooManyHttpHandlers.class)
			.run((context) -> assertThat(context.getStartupFailure()).isInstanceOf(ApplicationContextException.class)
				.rootCause()
				.hasMessageContaining("multiple HttpHandler beans : httpHandler,additionalHttpHandler"));
	}

	@Test
	void customizeReactiveWebServer() {
		this.mockServerRunner
			.withUserConfiguration(MockWebServerConfiguration.class, HttpHandlerConfiguration.class,
					ReactiveWebServerCustomization.class)
			.run((context) -> assertThat(context.getBean(MockReactiveWebServerFactory.class).getPort())
				.isEqualTo(9000));
	}

	@Test
	void webServerFailsWithInvalidSslBundle() {
		this.serverRunner.withUserConfiguration(HttpHandlerConfiguration.class)
			.withBean(WebServerFactoryCustomizer.class,
					() -> (WebServerFactoryCustomizer<ConfigurableWebServerFactory>) (factory) -> factory
						.setSslBundles(new DefaultSslBundleRegistry()))
			.withPropertyValues("server.ssl.bundle=test-bundle")
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure().getCause()).isInstanceOf(NoSuchSslBundleException.class)
					.withFailMessage("test");
			});
	}

	@Test
	void forwardedHeaderTransformerShouldBeConfigured() {
		this.mockServerRunner.withUserConfiguration(HttpHandlerConfiguration.class)
			.withPropertyValues("server.forward-headers-strategy=framework", "server.port=0")
			.run((context) -> assertThat(context).hasSingleBean(ForwardedHeaderTransformer.class));
	}

	@Test
	void forwardedHeaderTransformerWhenStrategyNotFilterShouldNotBeConfigured() {
		this.mockServerRunner.withUserConfiguration(HttpHandlerConfiguration.class)
			.withPropertyValues("server.forward-headers-strategy=native", "server.port=0")
			.run((context) -> assertThat(context).doesNotHaveBean(ForwardedHeaderTransformer.class));
	}

	@Test
	void forwardedHeaderTransformerWhenAlreadyRegisteredShouldBackOff() {
		this.mockServerRunner
			.withUserConfiguration(ForwardedHeaderTransformerConfiguration.class, HttpHandlerConfiguration.class)
			.withPropertyValues("server.forward-headers-strategy=framework", "server.port=0")
			.run((context) -> assertThat(context).hasSingleBean(ForwardedHeaderTransformer.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class HttpHandlerConfiguration {

		@Bean
		HttpHandler httpHandler() {
			return mock(HttpHandler.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TooManyHttpHandlers {

		@Bean
		HttpHandler additionalHttpHandler() {
			return mock(HttpHandler.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ReactiveWebServerCustomization {

		@Bean
		WebServerFactoryCustomizer<ConfigurableReactiveWebServerFactory> reactiveWebServerCustomizer() {
			return (factory) -> factory.setPort(9000);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MockWebServerConfiguration {

		@Bean
		MockReactiveWebServerFactory mockReactiveWebServerFactory() {
			return new MockReactiveWebServerFactory();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ForwardedHeaderTransformerConfiguration {

		@Bean
		ForwardedHeaderTransformer testForwardedHeaderTransformer() {
			return new ForwardedHeaderTransformer();
		}

	}

}
