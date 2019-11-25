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

package org.springframework.boot.actuate.autoconfigure.metrics.web.jetty;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.metrics.web.jetty.JettyServerThreadPoolMetricsBinder;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.embedded.jetty.JettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JettyMetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class JettyMetricsAutoConfigurationTests {

	@Test
	void autoConfiguresThreadPoolMetricsWithEmbeddedServletJetty() {
		new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(JettyMetricsAutoConfiguration.class,
						ServletWebServerFactoryAutoConfiguration.class))
				.withUserConfiguration(ServletWebServerConfiguration.class, MeterRegistryConfiguration.class)
				.run((context) -> {
					context.publishEvent(new ApplicationStartedEvent(new SpringApplication(), null,
							context.getSourceApplicationContext()));
					assertThat(context).hasSingleBean(JettyServerThreadPoolMetricsBinder.class);
					SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
					assertThat(registry.find("jetty.threads.config.min").meter()).isNotNull();
				});
	}

	@Test
	void autoConfiguresThreadPoolMetricsWithEmbeddedReactiveJetty() {
		new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(JettyMetricsAutoConfiguration.class,
						ReactiveWebServerFactoryAutoConfiguration.class))
				.withUserConfiguration(ReactiveWebServerConfiguration.class, MeterRegistryConfiguration.class)
				.run((context) -> {
					context.publishEvent(new ApplicationStartedEvent(new SpringApplication(), null,
							context.getSourceApplicationContext()));
					SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
					assertThat(registry.find("jetty.threads.config.min").meter()).isNotNull();
				});
	}

	@Test
	void allowsCustomJettyServerThreadPoolMetricsBinderToBeUsed() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(JettyMetricsAutoConfiguration.class))
				.withUserConfiguration(CustomJettyServerThreadPoolMetricsBinder.class, MeterRegistryConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(JettyServerThreadPoolMetricsBinder.class)
						.hasBean("customJettyServerThreadPoolMetricsBinder"));
	}

	@Configuration(proxyBeanMethods = false)
	static class MeterRegistryConfiguration {

		@Bean
		SimpleMeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ServletWebServerConfiguration {

		@Bean
		JettyServletWebServerFactory jettyFactory() {
			return new JettyServletWebServerFactory(0);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ReactiveWebServerConfiguration {

		@Bean
		JettyReactiveWebServerFactory jettyFactory() {
			return new JettyReactiveWebServerFactory(0);
		}

		@Bean
		HttpHandler httpHandler() {
			return mock(HttpHandler.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJettyServerThreadPoolMetricsBinder {

		@Bean
		JettyServerThreadPoolMetricsBinder customJettyServerThreadPoolMetricsBinder(MeterRegistry meterRegistry) {
			return new JettyServerThreadPoolMetricsBinder(meterRegistry);
		}

	}

}
