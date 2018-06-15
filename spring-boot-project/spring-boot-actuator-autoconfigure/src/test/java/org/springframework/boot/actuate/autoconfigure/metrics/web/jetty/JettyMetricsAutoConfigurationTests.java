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

package org.springframework.boot.actuate.autoconfigure.metrics.web.jetty;

import java.util.Collections;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jetty.JettyStatisticsMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.embedded.jetty.JettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.server.reactive.HttpHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JettyMetricsAutoConfiguration}.
 *
 * @author Even Holthe
 */
public class JettyMetricsAutoConfigurationTests {
	@Test
	public void autoConfiguresJettyStatisticsMetricsWithEmbeddedServletJetty() {
		new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
					.withConfiguration(
							AutoConfigurations.of(
								JettyMetricsAutoConfiguration.class,
								ServletWebServerFactoryAutoConfiguration.class))
					.withUserConfiguration(JettyMetricsAutoConfigurationTests.ServletWebServerConfiguration.class)
					.run((context) -> {
						assertThat(context)
								.hasSingleBean(JettyStatisticsMetrics.class);
						SimpleMeterRegistry registry = new SimpleMeterRegistry();
						context.getBean(JettyStatisticsMetrics.class)
								.bindTo(registry);

						assertThat(registry.find("jetty.requests").meter())
								.isNotNull();
						assertThat(registry.find("jetty.requests.active").gauge())
								.isNotNull();
					});
	}

	@Test
	public void jettyMetricsAreAvailableWhenEarlyMeterBinderInitializationOccurs() {
		new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(
						JettyMetricsAutoConfiguration.class,
						ServletWebServerFactoryAutoConfiguration.class))
				.withUserConfiguration(JettyMetricsAutoConfigurationTests.ServletWebServerConfiguration.class,
						JettyMetricsAutoConfigurationTests.EarlyMeterBinderInitializationConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(JettyStatisticsMetrics.class);
					SimpleMeterRegistry registry = new SimpleMeterRegistry();
					context.getBean(JettyStatisticsMetrics.class).bindTo(registry);

					assertThat(registry.find("jetty.requests").meter())
							.isNotNull();
					assertThat(registry.find("jetty.requests.active").gauge())
							.isNotNull();
				});
	}

	@Test
	public void autoConfiguresJettyStatisticsMetricsWithEmbeddedReactiveJetty() {
		new ReactiveWebApplicationContextRunner(
				AnnotationConfigReactiveWebServerApplicationContext::new)
					.withConfiguration(AutoConfigurations.of(
							JettyMetricsAutoConfiguration.class,
							ReactiveWebServerFactoryAutoConfiguration.class))
					.withUserConfiguration(JettyMetricsAutoConfigurationTests.ReactiveWebServerConfiguration.class)
					.run((context) -> {
						assertThat(context).hasSingleBean(JettyStatisticsMetrics.class);
						SimpleMeterRegistry registry = new SimpleMeterRegistry();
						context.getBean(JettyStatisticsMetrics.class).bindTo(registry);

						assertThat(registry.find("jetty.requests").meter())
								.isNotNull();
						assertThat(registry.find("jetty.requests.active").gauge())
								.isNotNull();
					});
	}

	@Test
	public void autoConfiguresJettyStatisticsMetricsWithStandaloneJetty() {
		new WebApplicationContextRunner()
				.withConfiguration(
						AutoConfigurations.of(JettyMetricsAutoConfiguration.class))
				.run((context) -> assertThat(context).hasSingleBean(JettyMetricsAutoConfiguration.class));
	}

	@Test
	public void allowsCustomJettyStatisticsMetricsToBeUsed() {
		new WebApplicationContextRunner()
				.withConfiguration(
						AutoConfigurations.of(JettyMetricsAutoConfiguration.class))
				.withUserConfiguration(JettyMetricsAutoConfigurationTests.CustomJettyMetrics.class)
				.run((context) -> assertThat(context).hasSingleBean(JettyStatisticsMetrics.class)
						.hasBean("customJettyMetrics"));
	}

	@Test
	public void autoConfiguresJettyStatisticsMetricsWithCustomJettyStatisticsHandler() {
		new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(
						JettyMetricsAutoConfiguration.class,
						ServletWebServerFactoryAutoConfiguration.class))
				.withUserConfiguration(JettyMetricsAutoConfigurationTests.ServletWebServerConfiguration.class,
						JettyMetricsAutoConfigurationTests.CustomJettyStatisticsHandler.class)
				.run((context -> {
					assertThat(context).hasSingleBean(JettyStatisticsMetrics.class);
					SimpleMeterRegistry registry = new SimpleMeterRegistry();
					context.getBean(JettyStatisticsMetrics.class).bindTo(registry);

					final Meter fixedMeter = registry.find("jetty.async.requests").meter();

					assertThat(fixedMeter)
							.isNotNull();

					assertThat(fixedMeter.measure().iterator().next().getValue())
							.isEqualTo(42.0);
				}));
	}

	@Configuration
	static class ServletWebServerConfiguration {

		@Bean
		public JettyServletWebServerFactory jettyFactory() {
			return new JettyServletWebServerFactory(0);
		}

	}

	@Configuration
	static class ReactiveWebServerConfiguration {

		@Bean
		public JettyReactiveWebServerFactory tomcatFactory() {
			return new JettyReactiveWebServerFactory(0);
		}

		@Bean
		public HttpHandler httpHandler() {
			return mock(HttpHandler.class);
		}

	}

	@Configuration
	static class CustomJettyMetrics {

		@Bean
		public JettyStatisticsMetrics customJettyMetrics() {
			return new JettyStatisticsMetrics(null, Collections.emptyList());
		}

	}

	@Configuration
	static class CustomJettyStatisticsHandler {
		@Bean
		@Primary
		public WebServerFactoryCustomizer<JettyServletWebServerFactory> customJettyStatisticsCustomizer() {
			return factory -> {
				factory.addServerCustomizers(server -> {
					final StatisticsHandler statsHandler = new CustomStatisticsHandler();

					statsHandler.setHandler(server.getHandler());
					server.setHandler(statsHandler);
				});
			};
		}

		public class CustomStatisticsHandler extends StatisticsHandler {
			@Override
			public int getAsyncRequests() {
				return 42;
			}
		}
	}

	@Configuration
	static class EarlyMeterBinderInitializationConfiguration {
		@Bean
		public ServletContextInitializer earlyInitializer(ApplicationContext context) {
			return (servletContext) -> context.getBeansOfType(MeterBinder.class);
		}

	}
}
