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

package org.springframework.boot.actuate.autoconfigure.metrics.web.tomcat;

import java.util.Collections;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.tomcat.TomcatMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TomcatMetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public class TomcatMetricsAutoConfigurationTests {

	@Test
	public void autoConfiguresTomcatMetricsWithEmbeddedServletTomcat() {
		new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(TomcatMetricsAutoConfiguration.class,
						ServletWebServerFactoryAutoConfiguration.class))
				.withUserConfiguration(ServletWebServerConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(TomcatMetrics.class);
					SimpleMeterRegistry registry = new SimpleMeterRegistry();
					context.getBean(TomcatMetrics.class).bindTo(registry);
					assertThat(registry.find("tomcat.sessions.active.max").meter()).isNotNull();
					assertThat(registry.find("tomcat.threads.current").meter()).isNotNull();
				});
	}

	@Test
	public void sessionMetricsAreAvailableWhenEarlyMeterBinderInitializationOccurs() {
		new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(TomcatMetricsAutoConfiguration.class,
						ServletWebServerFactoryAutoConfiguration.class))
				.withUserConfiguration(ServletWebServerConfiguration.class,
						EarlyMeterBinderInitializationConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(TomcatMetrics.class);
					SimpleMeterRegistry registry = new SimpleMeterRegistry();
					context.getBean(TomcatMetrics.class).bindTo(registry);
					assertThat(registry.find("tomcat.sessions.active.max").meter()).isNotNull();
					assertThat(registry.find("tomcat.threads.current").meter()).isNotNull();
				});
	}

	@Test
	public void autoConfiguresTomcatMetricsWithEmbeddedReactiveTomcat() {
		new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(TomcatMetricsAutoConfiguration.class,
						ReactiveWebServerFactoryAutoConfiguration.class))
				.withUserConfiguration(ReactiveWebServerConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(TomcatMetrics.class);
					SimpleMeterRegistry registry = new SimpleMeterRegistry();
					context.getBean(TomcatMetrics.class).bindTo(registry);
					assertThat(registry.find("tomcat.sessions.active.max").meter()).isNotNull();
					assertThat(registry.find("tomcat.threads.current").meter()).isNotNull();
				});
	}

	@Test
	public void autoConfiguresTomcatMetricsWithStandaloneTomcat() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(TomcatMetricsAutoConfiguration.class))
				.run((context) -> assertThat(context).hasSingleBean(TomcatMetrics.class));
	}

	@Test
	public void allowsCustomTomcatMetricsToBeUsed() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(TomcatMetricsAutoConfiguration.class))
				.withUserConfiguration(CustomTomcatMetrics.class).run((context) -> assertThat(context)
						.hasSingleBean(TomcatMetrics.class).hasBean("customTomcatMetrics"));
	}

	@Configuration
	static class ServletWebServerConfiguration {

		@Bean
		public TomcatServletWebServerFactory tomcatFactory() {
			return new TomcatServletWebServerFactory(0);
		}

	}

	@Configuration
	static class ReactiveWebServerConfiguration {

		@Bean
		public TomcatReactiveWebServerFactory tomcatFactory() {
			return new TomcatReactiveWebServerFactory(0);
		}

		@Bean
		public HttpHandler httpHandler() {
			return mock(HttpHandler.class);
		}

	}

	@Configuration
	static class CustomTomcatMetrics {

		@Bean
		public TomcatMetrics customTomcatMetrics() {
			return new TomcatMetrics(null, Collections.emptyList());
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
