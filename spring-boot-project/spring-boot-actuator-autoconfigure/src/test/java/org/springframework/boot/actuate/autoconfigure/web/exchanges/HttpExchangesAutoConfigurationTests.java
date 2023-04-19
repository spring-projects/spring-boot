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

package org.springframework.boot.actuate.autoconfigure.web.exchanges;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.Include;
import org.springframework.boot.actuate.web.exchanges.reactive.HttpExchangesWebFilter;
import org.springframework.boot.actuate.web.exchanges.servlet.HttpExchangesFilter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpExchangesAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class HttpExchangesAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HttpExchangesAutoConfiguration.class));

	@Test
	void autoConfigurationIsDisabledByDefault() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(HttpExchangesAutoConfiguration.class));
	}

	@Test
	void autoConfigurationIsEnabledWhenHttpExchangeRepositoryBeanPresent() {
		this.contextRunner.withUserConfiguration(CustomHttpExchangesRepositoryConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(HttpExchangesFilter.class);
			assertThat(context).hasSingleBean(HttpExchangeRepository.class);
			assertThat(context.getBean(HttpExchangeRepository.class)).isInstanceOf(CustomHttpExchangesRepository.class);
		});
	}

	@Test
	void usesUserProvidedWebFilterWhenReactiveContext() {
		new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HttpExchangesAutoConfiguration.class))
			.withUserConfiguration(CustomHttpExchangesRepositoryConfiguration.class)
			.withUserConfiguration(CustomWebFilterConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(HttpExchangesWebFilter.class);
				assertThat(context.getBean(HttpExchangesWebFilter.class))
					.isInstanceOf(CustomHttpExchangesWebFilter.class);
			});
	}

	@Test
	void configuresServletFilter() {
		this.contextRunner.withUserConfiguration(CustomHttpExchangesRepositoryConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(HttpExchangesFilter.class));
	}

	@Test
	void usesUserProvidedServletFilter() {
		this.contextRunner.withUserConfiguration(CustomHttpExchangesRepositoryConfiguration.class)
			.withUserConfiguration(CustomFilterConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(HttpExchangesFilter.class);
				assertThat(context.getBean(HttpExchangesFilter.class)).isInstanceOf(CustomHttpExchangesFilter.class);
			});
	}

	@Test
	void backsOffWhenNotRecording() {
		this.contextRunner.withUserConfiguration(CustomHttpExchangesRepositoryConfiguration.class)
			.withPropertyValues("management.httpexchanges.recording.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(InMemoryHttpExchangeRepository.class)
				.doesNotHaveBean(HttpExchangesFilter.class));
	}

	static class CustomHttpExchangesRepository implements HttpExchangeRepository {

		@Override
		public List<HttpExchange> findAll() {
			return null;
		}

		@Override
		public void add(HttpExchange exchange) {

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomHttpExchangesRepositoryConfiguration {

		@Bean
		CustomHttpExchangesRepository customRepository() {
			return new CustomHttpExchangesRepository();
		}

	}

	private static final class CustomHttpExchangesWebFilter extends HttpExchangesWebFilter {

		private CustomHttpExchangesWebFilter(HttpExchangeRepository repository, Set<Include> includes) {
			super(repository, includes);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomWebFilterConfiguration {

		@Bean
		CustomHttpExchangesWebFilter customWebFilter(HttpExchangeRepository repository,
				HttpExchangesProperties properties) {
			return new CustomHttpExchangesWebFilter(repository, properties.getRecording().getInclude());
		}

	}

	private static final class CustomHttpExchangesFilter extends HttpExchangesFilter {

		private CustomHttpExchangesFilter(HttpExchangeRepository repository, Set<Include> includes) {
			super(repository, includes);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomFilterConfiguration {

		@Bean
		CustomHttpExchangesFilter customWebFilter(HttpExchangeRepository repository, Set<Include> includes) {
			return new CustomHttpExchangesFilter(repository, includes);
		}

	}

}
