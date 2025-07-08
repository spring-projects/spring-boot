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

package org.springframework.boot.webflux.autoconfigure.actuate.web;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.Include;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.webflux.actuate.exchanges.HttpExchangesWebFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebFluxHttpExchangesAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class WebFluxHttpExchangesAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(WebFluxHttpExchangesAutoConfiguration.class));

	@Test
	void whenRecordingIsDisabledThenFilterIsNotCreated() {
		this.contextRunner.withBean(InMemoryHttpExchangeRepository.class)
			.withPropertyValues("management.httpexchanges.recording.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(HttpExchangesWebFilter.class));
	}

	@Test
	void whenNoRepositoryIsDefinedThenFilterIsNotCreated() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(HttpExchangesWebFilter.class));
	}

	@Test
	void filterIsCreated() {
		this.contextRunner.withBean(InMemoryHttpExchangeRepository.class)
			.run((context) -> assertThat(context).hasSingleBean(HttpExchangesWebFilter.class));
	}

	@Test
	void usesUserProvidedWebFilter() {
		InMemoryHttpExchangeRepository repository = new InMemoryHttpExchangeRepository();
		this.contextRunner.withBean(InMemoryHttpExchangeRepository.class, () -> repository)
			.withBean(CustomHttpExchangesWebFilter.class,
					() -> new CustomHttpExchangesWebFilter(repository, EnumSet.allOf(Include.class)))
			.run((context) -> {
				assertThat(context).hasSingleBean(HttpExchangesWebFilter.class);
				assertThat(context.getBean(HttpExchangesWebFilter.class))
					.isInstanceOf(CustomHttpExchangesWebFilter.class);
			});
	}

	private static final class CustomHttpExchangesWebFilter extends HttpExchangesWebFilter {

		private CustomHttpExchangesWebFilter(HttpExchangeRepository repository, Set<Include> includes) {
			super(repository, includes);
		}

	}

}
