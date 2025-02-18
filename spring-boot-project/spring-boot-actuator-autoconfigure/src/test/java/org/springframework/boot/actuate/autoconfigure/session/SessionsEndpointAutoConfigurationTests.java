/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.session;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.session.ReactiveSessionsEndpoint;
import org.springframework.boot.actuate.session.SessionsEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.ReactiveFindByIndexNameSessionRepository;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.SessionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SessionsEndpointAutoConfiguration}.
 *
 * @author Vedran Pavic
 * @author Moritz Halbritter
 */
class SessionsEndpointAutoConfigurationTests {

	@Nested
	class ServletSessionEndpointConfigurationTests {

		private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SessionsEndpointAutoConfiguration.class))
			.withUserConfiguration(IndexedSessionRepositoryConfiguration.class);

		@Test
		void runShouldHaveEndpointBean() {
			this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=sessions")
				.run((context) -> assertThat(context).hasSingleBean(SessionsEndpoint.class));
		}

		@Test
		void runWhenNoIndexedSessionRepositoryShouldHaveEndpointBean() {
			new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(SessionsEndpointAutoConfiguration.class))
				.withUserConfiguration(SessionRepositoryConfiguration.class)
				.withPropertyValues("management.endpoints.web.exposure.include=sessions")
				.run((context) -> assertThat(context).hasSingleBean(SessionsEndpoint.class));
		}

		@Test
		void runWhenNotExposedShouldNotHaveEndpointBean() {
			this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(SessionsEndpoint.class));
		}

		@Test
		void runWhenEnabledPropertyIsFalseShouldNotHaveEndpointBean() {
			this.contextRunner.withPropertyValues("management.endpoint.sessions.enabled:false")
				.run((context) -> assertThat(context).doesNotHaveBean(SessionsEndpoint.class));
		}

		@Configuration(proxyBeanMethods = false)
		static class IndexedSessionRepositoryConfiguration {

			@Bean
			FindByIndexNameSessionRepository<?> sessionRepository() {
				return mock(FindByIndexNameSessionRepository.class);
			}

		}

		@Configuration(proxyBeanMethods = false)
		static class SessionRepositoryConfiguration {

			@Bean
			SessionRepository<?> sessionRepository() {
				return mock(SessionRepository.class);
			}

		}

	}

	@Nested
	class ReactiveSessionEndpointConfigurationTests {

		private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SessionsEndpointAutoConfiguration.class))
			.withUserConfiguration(ReactiveSessionRepositoryConfiguration.class,
					ReactiveIndexedSessionRepositoryConfiguration.class);

		@Test
		void runShouldHaveEndpointBean() {
			this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=sessions")
				.run((context) -> assertThat(context).hasSingleBean(ReactiveSessionsEndpoint.class));
		}

		@Test
		void runWhenNoIndexedSessionRepositoryShouldHaveEndpointBean() {
			new ReactiveWebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(SessionsEndpointAutoConfiguration.class))
				.withUserConfiguration(ReactiveSessionRepositoryConfiguration.class)
				.withPropertyValues("management.endpoints.web.exposure.include=sessions")
				.run((context) -> assertThat(context).hasSingleBean(ReactiveSessionsEndpoint.class));
		}

		@Test
		void runWhenNotExposedShouldNotHaveEndpointBean() {
			this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(ReactiveSessionsEndpoint.class));
		}

		@Test
		void runWhenEnabledPropertyIsFalseShouldNotHaveEndpointBean() {
			this.contextRunner.withPropertyValues("management.endpoint.sessions.enabled:false")
				.run((context) -> assertThat(context).doesNotHaveBean(ReactiveSessionsEndpoint.class));
		}

		@Configuration(proxyBeanMethods = false)
		static class ReactiveIndexedSessionRepositoryConfiguration {

			@Bean
			ReactiveFindByIndexNameSessionRepository<?> indexedSessionRepository() {
				return mock(ReactiveFindByIndexNameSessionRepository.class);
			}

		}

		@Configuration(proxyBeanMethods = false)
		static class ReactiveSessionRepositoryConfiguration {

			@Bean
			ReactiveSessionRepository<?> sessionRepository() {
				return mock(ReactiveSessionRepository.class);
			}

		}

	}

}
