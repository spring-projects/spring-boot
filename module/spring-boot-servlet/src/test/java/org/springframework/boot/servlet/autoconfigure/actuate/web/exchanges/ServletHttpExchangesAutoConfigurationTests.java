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

package org.springframework.boot.servlet.autoconfigure.actuate.web.exchanges;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.Include;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.servlet.actuate.web.exchanges.HttpExchangesFilter;
import org.springframework.boot.servlet.actuate.web.exchanges.HttpExchangesStartingFilter;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServletHttpExchangesAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class ServletHttpExchangesAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ServletHttpExchangesAutoConfiguration.class));

	@Test
	void whenRecordingIsDisabledThenFilterIsNotCreated() {
		this.contextRunner.withBean(InMemoryHttpExchangeRepository.class)
			.withPropertyValues("management.httpexchanges.recording.enabled=false")
			.run((context) -> {
				assertThat(context).doesNotHaveBean(HttpExchangesFilter.class);
				assertThat(context).doesNotHaveBean(HttpExchangesStartingFilter.class);
			});
	}

	@Test
	void whenNoRepositoryIsDefinedThenFilterIsNotCreated() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(HttpExchangesFilter.class);
			assertThat(context).doesNotHaveBean(HttpExchangesStartingFilter.class);
		});
	}

	@Test
	void bothFiltersAreCreated() {
		this.contextRunner.withBean(InMemoryHttpExchangeRepository.class).run((context) -> {
			assertThat(context).hasSingleBean(HttpExchangesFilter.class);
			assertThat(context).hasSingleBean(HttpExchangesStartingFilter.class);
		});
	}

	@Test
	void usesUserProvidedHttpExchangesFilter() {
		InMemoryHttpExchangeRepository repository = new InMemoryHttpExchangeRepository();
		this.contextRunner.withBean(InMemoryHttpExchangeRepository.class, () -> repository)
			.withBean(CustomHttpExchangesFilter.class,
					() -> new CustomHttpExchangesFilter(repository, EnumSet.allOf(Include.class)))
			.run((context) -> {
				assertThat(context).hasSingleBean(HttpExchangesFilter.class);
				assertThat(context.getBean(HttpExchangesFilter.class)).isInstanceOf(CustomHttpExchangesFilter.class);
				// Starting filter is still auto-created
				assertThat(context).hasSingleBean(HttpExchangesStartingFilter.class);
			});
	}

	@Test
	void usesUserProvidedHttpExchangesStartingFilter() {
		InMemoryHttpExchangeRepository repository = new InMemoryHttpExchangeRepository();
		this.contextRunner.withBean(InMemoryHttpExchangeRepository.class, () -> repository)
			.withBean(CustomHttpExchangesStartingFilter.class,
					() -> new CustomHttpExchangesStartingFilter(repository, EnumSet.allOf(Include.class)))
			.run((context) -> {
				assertThat(context).hasSingleBean(HttpExchangesStartingFilter.class);
				assertThat(context.getBean(HttpExchangesStartingFilter.class))
					.isInstanceOf(CustomHttpExchangesStartingFilter.class);
				// Finishing filter is still auto-created
				assertThat(context).hasSingleBean(HttpExchangesFilter.class);
			});
	}

	private static final class CustomHttpExchangesFilter extends HttpExchangesFilter {

		private CustomHttpExchangesFilter(HttpExchangeRepository repository, Set<Include> includes) {
			super(repository, includes);
		}

	}

	private static final class CustomHttpExchangesStartingFilter extends HttpExchangesStartingFilter {

		private CustomHttpExchangesStartingFilter(HttpExchangeRepository repository, Set<Include> includes) {
			super(repository, includes);
		}

	}

}
