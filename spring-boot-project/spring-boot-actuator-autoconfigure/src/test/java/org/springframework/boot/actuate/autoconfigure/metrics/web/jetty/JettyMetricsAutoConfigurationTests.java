/*
 * Copyright 2012-2022 the original author or authors.
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
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.metrics.web.jetty.JettyConnectionMetricsBinder;
import org.springframework.boot.actuate.metrics.web.jetty.JettyServerThreadPoolMetricsBinder;
import org.springframework.boot.actuate.metrics.web.jetty.JettySslHandshakeMetricsBinder;
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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JettyMetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Chris Bono
 */
class JettyMetricsAutoConfigurationTests {

	@Test
	void autoConfiguresThreadPoolMetricsWithEmbeddedServletJetty() {
		new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(JettyMetricsAutoConfiguration.class,
						ServletWebServerFactoryAutoConfiguration.class))
				.withUserConfiguration(ServletWebServerConfiguration.class, MeterRegistryConfiguration.class)
				.run((context) -> {
					context.publishEvent(createApplicationStartedEvent(context.getSourceApplicationContext()));
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
					context.publishEvent(createApplicationStartedEvent(context.getSourceApplicationContext()));
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

	@Test
	void autoConfiguresConnectionMetricsWithEmbeddedServletJetty() {
		new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(JettyMetricsAutoConfiguration.class,
						ServletWebServerFactoryAutoConfiguration.class))
				.withUserConfiguration(ServletWebServerConfiguration.class, MeterRegistryConfiguration.class)
				.run((context) -> {
					context.publishEvent(createApplicationStartedEvent(context.getSourceApplicationContext()));
					assertThat(context).hasSingleBean(JettyConnectionMetricsBinder.class);
					SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
					assertThat(registry.find("jetty.connections.messages.in").meter()).isNotNull();
				});
	}

	@Test
	void autoConfiguresConnectionMetricsWithEmbeddedReactiveJetty() {
		new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(JettyMetricsAutoConfiguration.class,
						ReactiveWebServerFactoryAutoConfiguration.class))
				.withUserConfiguration(ReactiveWebServerConfiguration.class, MeterRegistryConfiguration.class)
				.run((context) -> {
					context.publishEvent(createApplicationStartedEvent(context.getSourceApplicationContext()));
					SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
					assertThat(registry.find("jetty.connections.messages.in").meter()).isNotNull();
				});
	}

	@Test
	void allowsCustomJettyConnectionMetricsBinderToBeUsed() {
		new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(JettyMetricsAutoConfiguration.class,
						ServletWebServerFactoryAutoConfiguration.class))
				.withUserConfiguration(ServletWebServerConfiguration.class, CustomJettyConnectionMetricsBinder.class,
						MeterRegistryConfiguration.class)
				.run((context) -> {
					context.publishEvent(createApplicationStartedEvent(context.getSourceApplicationContext()));
					assertThat(context).hasSingleBean(JettyConnectionMetricsBinder.class)
							.hasBean("customJettyConnectionMetricsBinder");
					SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
					assertThat(registry.find("jetty.connections.messages.in").tag("custom-tag-name", "custom-tag-value")
							.meter()).isNotNull();
				});
	}

	@Test
	void autoConfiguresSslHandshakeMetricsWithEmbeddedServletJetty() {
		new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(JettyMetricsAutoConfiguration.class,
						ServletWebServerFactoryAutoConfiguration.class))
				.withUserConfiguration(ServletWebServerConfiguration.class, MeterRegistryConfiguration.class)
				.withPropertyValues("server.ssl.enabled: true", "server.ssl.key-store: src/test/resources/test.jks",
						"server.ssl.key-store-password: secret", "server.ssl.key-password: password")
				.run((context) -> {
					context.publishEvent(createApplicationStartedEvent(context.getSourceApplicationContext()));
					assertThat(context).hasSingleBean(JettySslHandshakeMetricsBinder.class);
					SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
					assertThat(registry.find("jetty.ssl.handshakes").meter()).isNotNull();
				});
	}

	@Test
	void autoConfiguresSslHandshakeMetricsWithEmbeddedReactiveJetty() {
		new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(JettyMetricsAutoConfiguration.class,
						ReactiveWebServerFactoryAutoConfiguration.class))
				.withUserConfiguration(ReactiveWebServerConfiguration.class, MeterRegistryConfiguration.class)
				.withPropertyValues("server.ssl.enabled: true", "server.ssl.key-store: src/test/resources/test.jks",
						"server.ssl.key-store-password: secret", "server.ssl.key-password: password")
				.run((context) -> {
					context.publishEvent(createApplicationStartedEvent(context.getSourceApplicationContext()));
					SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
					assertThat(registry.find("jetty.ssl.handshakes").meter()).isNotNull();
				});
	}

	@Test
	void allowsCustomJettySslHandshakeMetricsBinderToBeUsed() {
		new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(JettyMetricsAutoConfiguration.class,
						ServletWebServerFactoryAutoConfiguration.class))
				.withUserConfiguration(ServletWebServerConfiguration.class, CustomJettySslHandshakeMetricsBinder.class,
						MeterRegistryConfiguration.class)
				.withPropertyValues("server.ssl.enabled: true", "server.ssl.key-store: src/test/resources/test.jks",
						"server.ssl.key-store-password: secret", "server.ssl.key-password: password")
				.run((context) -> {
					context.publishEvent(createApplicationStartedEvent(context.getSourceApplicationContext()));
					assertThat(context).hasSingleBean(JettySslHandshakeMetricsBinder.class)
							.hasBean("customJettySslHandshakeMetricsBinder");
					SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
					assertThat(registry.find("jetty.ssl.handshakes").tag("custom-tag-name", "custom-tag-value").meter())
							.isNotNull();
				});

		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(JettyMetricsAutoConfiguration.class))
				.withUserConfiguration(CustomJettySslHandshakeMetricsBinder.class, MeterRegistryConfiguration.class)
				.withPropertyValues("server.ssl.enabled: true", "server.ssl.key-store: src/test/resources/test.jks",
						"server.ssl.key-store-password: secret", "server.ssl.key-password: password")
				.run((context) -> assertThat(context).hasSingleBean(JettySslHandshakeMetricsBinder.class)
						.hasBean("customJettySslHandshakeMetricsBinder"));
	}

	@Test
	void doesNotAutoConfigureSslHandshakeMetricsWhenSslEnabledPropertyNotSpecified() {
		new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(JettyMetricsAutoConfiguration.class,
						ServletWebServerFactoryAutoConfiguration.class))
				.withUserConfiguration(ServletWebServerConfiguration.class, MeterRegistryConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(JettySslHandshakeMetricsBinder.class));
	}

	@Test
	void doesNotAutoConfigureSslHandshakeMetricsWhenSslEnabledPropertySetToFalse() {
		new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(JettyMetricsAutoConfiguration.class,
						ServletWebServerFactoryAutoConfiguration.class))
				.withUserConfiguration(ServletWebServerConfiguration.class, MeterRegistryConfiguration.class)
				.withPropertyValues("server.ssl.enabled: false")
				.run((context) -> assertThat(context).doesNotHaveBean(JettySslHandshakeMetricsBinder.class));
	}

	private ApplicationStartedEvent createApplicationStartedEvent(ConfigurableApplicationContext context) {
		return new ApplicationStartedEvent(new SpringApplication(), null, context, null);
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

	@Configuration(proxyBeanMethods = false)
	static class CustomJettyConnectionMetricsBinder {

		@Bean
		JettyConnectionMetricsBinder customJettyConnectionMetricsBinder(MeterRegistry meterRegistry) {
			return new JettyConnectionMetricsBinder(meterRegistry, Tags.of("custom-tag-name", "custom-tag-value"));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJettySslHandshakeMetricsBinder {

		@Bean
		JettySslHandshakeMetricsBinder customJettySslHandshakeMetricsBinder(MeterRegistry meterRegistry) {
			return new JettySslHandshakeMetricsBinder(meterRegistry, Tags.of("custom-tag-name", "custom-tag-value"));
		}

	}

}
