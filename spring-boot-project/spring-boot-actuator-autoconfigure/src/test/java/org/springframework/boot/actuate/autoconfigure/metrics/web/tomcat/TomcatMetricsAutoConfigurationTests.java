/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.web.tomcat;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.tomcat.TomcatMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.tomcat.util.modeler.Registry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.metrics.web.tomcat.TomcatMetricsBinder;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TomcatMetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class TomcatMetricsAutoConfigurationTests {

	@Test
	void autoConfiguresTomcatMetricsWithEmbeddedServletTomcat() {
		resetTomcatState();
		new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(TomcatMetricsAutoConfiguration.class,
						ServletWebServerFactoryAutoConfiguration.class))
				.withUserConfiguration(ServletWebServerConfiguration.class, MeterRegistryConfiguration.class)
				.withPropertyValues("server.tomcat.mbeanregistry.enabled=true").run((context) -> {
					context.publishEvent(createApplicationStartedEvent(context.getSourceApplicationContext()));
					assertThat(context).hasSingleBean(TomcatMetricsBinder.class);
					SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
					assertThat(registry.find("tomcat.sessions.active.max").meter()).isNotNull();
					assertThat(registry.find("tomcat.threads.current").meter()).isNotNull();
				});
	}

	@Test
	void autoConfiguresTomcatMetricsWithEmbeddedReactiveTomcat() {
		resetTomcatState();
		new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(TomcatMetricsAutoConfiguration.class,
						ReactiveWebServerFactoryAutoConfiguration.class))
				.withUserConfiguration(ReactiveWebServerConfiguration.class, MeterRegistryConfiguration.class)
				.withPropertyValues("server.tomcat.mbeanregistry.enabled=true").run((context) -> {
					context.publishEvent(createApplicationStartedEvent(context.getSourceApplicationContext()));
					SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
					assertThat(registry.find("tomcat.sessions.active.max").meter()).isNotNull();
					assertThat(registry.find("tomcat.threads.current").meter()).isNotNull();
				});
	}

	@Test
	void autoConfiguresTomcatMetricsWithStandaloneTomcat() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(TomcatMetricsAutoConfiguration.class))
				.withUserConfiguration(MeterRegistryConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(TomcatMetricsBinder.class));
	}

	@Test
	void allowsCustomTomcatMetricsBinderToBeUsed() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(TomcatMetricsAutoConfiguration.class))
				.withUserConfiguration(MeterRegistryConfiguration.class, CustomTomcatMetricsBinder.class)
				.run((context) -> assertThat(context).hasSingleBean(TomcatMetricsBinder.class)
						.hasBean("customTomcatMetricsBinder"));
	}

	@Test
	void allowsCustomTomcatMetricsToBeUsed() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(TomcatMetricsAutoConfiguration.class))
				.withUserConfiguration(MeterRegistryConfiguration.class, CustomTomcatMetrics.class)
				.run((context) -> assertThat(context).doesNotHaveBean(TomcatMetricsBinder.class)
						.hasBean("customTomcatMetrics"));
	}

	private ApplicationStartedEvent createApplicationStartedEvent(ConfigurableApplicationContext context) {
		return new ApplicationStartedEvent(new SpringApplication(), null, context, null);
	}

	private void resetTomcatState() {
		ReflectionTestUtils.setField(Registry.class, "registry", null);
		AtomicInteger containerCounter = (AtomicInteger) ReflectionTestUtils.getField(TomcatWebServer.class,
				"containerCounter");
		containerCounter.set(-1);
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
		TomcatServletWebServerFactory tomcatFactory() {
			return new TomcatServletWebServerFactory(0);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ReactiveWebServerConfiguration {

		@Bean
		TomcatReactiveWebServerFactory tomcatFactory() {
			return new TomcatReactiveWebServerFactory(0);
		}

		@Bean
		HttpHandler httpHandler() {
			return mock(HttpHandler.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomTomcatMetrics {

		@Bean
		TomcatMetrics customTomcatMetrics() {
			return new TomcatMetrics(null, Collections.emptyList());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomTomcatMetricsBinder {

		@Bean
		TomcatMetricsBinder customTomcatMetricsBinder(MeterRegistry meterRegistry) {
			return new TomcatMetricsBinder(meterRegistry);
		}

	}

}
