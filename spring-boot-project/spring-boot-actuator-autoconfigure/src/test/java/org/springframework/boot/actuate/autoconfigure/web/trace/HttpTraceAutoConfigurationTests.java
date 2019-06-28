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

package org.springframework.boot.actuate.autoconfigure.web.trace;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.trace.http.HttpTraceAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.trace.http.HttpTraceProperties;
import org.springframework.boot.actuate.trace.http.HttpExchangeTracer;
import org.springframework.boot.actuate.trace.http.HttpTrace;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository;
import org.springframework.boot.actuate.trace.http.Include;
import org.springframework.boot.actuate.web.trace.reactive.HttpTraceWebFilter;
import org.springframework.boot.actuate.web.trace.servlet.HttpTraceFilter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpTraceAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class HttpTraceAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HttpTraceAutoConfiguration.class));

	@Test
	void autoConfigurationIsDisabledByDefault() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(HttpTraceAutoConfiguration.class));
	}

	@Test
	void autoConfigurationIsEnabledWhenHttpTraceRepositoryBeanPresent() {
		this.contextRunner.withUserConfiguration(HttpTraceRepositoryConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(HttpExchangeTracer.class);
			assertThat(context).hasSingleBean(HttpTraceFilter.class);
			assertThat(context).hasSingleBean(HttpTraceRepository.class);
			assertThat(context.getBean(HttpTraceRepository.class)).isInstanceOf(CustomHttpTraceRepository.class);
		});
	}

	@Test
	void usesUserProvidedTracer() {
		this.contextRunner.withUserConfiguration(HttpTraceRepositoryConfiguration.class)
				.withUserConfiguration(CustomTracerConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(HttpExchangeTracer.class);
					assertThat(context.getBean(HttpExchangeTracer.class)).isInstanceOf(CustomHttpExchangeTracer.class);
				});
	}

	@Test
	void usesUserProvidedWebFilterWhenReactiveContext() {
		new ReactiveWebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(HttpTraceAutoConfiguration.class))
				.withUserConfiguration(HttpTraceRepositoryConfiguration.class)
				.withUserConfiguration(CustomWebFilterConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(HttpTraceWebFilter.class);
					assertThat(context.getBean(HttpTraceWebFilter.class)).isInstanceOf(CustomHttpTraceWebFilter.class);
				});
	}

	@Test
	void configuresServletFilter() {
		this.contextRunner.withUserConfiguration(HttpTraceRepositoryConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(HttpTraceFilter.class));
	}

	@Test
	void usesUserProvidedServletFilter() {
		this.contextRunner.withUserConfiguration(HttpTraceRepositoryConfiguration.class)
				.withUserConfiguration(CustomFilterConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(HttpTraceFilter.class);
					assertThat(context.getBean(HttpTraceFilter.class)).isInstanceOf(CustomHttpTraceFilter.class);
				});
	}

	@Test
	void backsOffWhenDisabled() {
		this.contextRunner.withUserConfiguration(HttpTraceRepositoryConfiguration.class)
				.withPropertyValues("management.trace.http.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean(InMemoryHttpTraceRepository.class)
						.doesNotHaveBean(HttpExchangeTracer.class).doesNotHaveBean(HttpTraceFilter.class));
	}

	private static class CustomHttpTraceRepository implements HttpTraceRepository {

		@Override
		public List<HttpTrace> findAll() {
			return null;
		}

		@Override
		public void add(HttpTrace trace) {

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class HttpTraceRepositoryConfiguration {

		@Bean
		public CustomHttpTraceRepository customRepository() {
			return new CustomHttpTraceRepository();
		}

	}

	private static final class CustomHttpExchangeTracer extends HttpExchangeTracer {

		private CustomHttpExchangeTracer(Set<Include> includes) {
			super(includes);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomTracerConfiguration {

		@Bean
		public CustomHttpExchangeTracer customTracer(HttpTraceProperties properties) {
			return new CustomHttpExchangeTracer(properties.getInclude());
		}

	}

	private static final class CustomHttpTraceWebFilter extends HttpTraceWebFilter {

		private CustomHttpTraceWebFilter(HttpTraceRepository repository, HttpExchangeTracer tracer,
				Set<Include> includes) {
			super(repository, tracer, includes);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomWebFilterConfiguration {

		@Bean
		public CustomHttpTraceWebFilter customWebFilter(HttpTraceRepository repository, HttpExchangeTracer tracer,
				HttpTraceProperties properties) {
			return new CustomHttpTraceWebFilter(repository, tracer, properties.getInclude());
		}

	}

	private static final class CustomHttpTraceFilter extends HttpTraceFilter {

		private CustomHttpTraceFilter(HttpTraceRepository repository, HttpExchangeTracer tracer) {
			super(repository, tracer);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomFilterConfiguration {

		@Bean
		public CustomHttpTraceFilter customWebFilter(HttpTraceRepository repository, HttpExchangeTracer tracer) {
			return new CustomHttpTraceFilter(repository, tracer);
		}

	}

}
