/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.session;

import java.util.Collections;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SessionAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Vedran Pavic
 */
public class SessionAutoConfigurationTests extends AbstractSessionAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SessionAutoConfiguration.class));

	@Test
	public void contextFailsIfMultipleStoresAreAvailable() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context).getFailure()
					.hasCauseInstanceOf(NonUniqueSessionRepositoryException.class);
			assertThat(context).getFailure().hasMessageContaining(
					"Multiple session repository candidates are available");
		});
	}

	@Test
	public void contextFailsIfStoreTypeNotAvailable() {
		this.contextRunner.withPropertyValues("spring.session.store-type=jdbc")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context).getFailure().hasCauseInstanceOf(
							SessionRepositoryUnavailableException.class);
					assertThat(context).getFailure().hasMessageContaining(
							"No session repository could be auto-configured");
					assertThat(context).getFailure()
							.hasMessageContaining("session store type is 'jdbc'");
				});
	}

	@Test
	public void autoConfigurationDisabledIfStoreTypeSetToNone() {
		this.contextRunner.withPropertyValues("spring.session.store-type=none")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(SessionRepository.class));
	}

	@Test
	public void backOffIfSessionRepositoryIsPresent() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class)
				.withPropertyValues("spring.session.store-type=redis").run((context) -> {
					MapSessionRepository repository = validateSessionRepository(context,
							MapSessionRepository.class);
					assertThat(context).getBean("mySessionRepository")
							.isSameAs(repository);
				});
	}

	@Test
	public void springSessionTimeoutIsNotAValidProperty() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class)
				.withPropertyValues("spring.session.timeout=3000").run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context).getFailure()
							.isInstanceOf(BeanCreationException.class);
					assertThat(context).getFailure()
							.hasMessageContaining("Could not bind");
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void filterIsRegisteredWithAsyncErrorAndRequestDispatcherTypes() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class)
				.run((context) -> {
					FilterRegistrationBean<?> registration = context
							.getBean(FilterRegistrationBean.class);
					assertThat(registration.getFilter())
							.isSameAs(context.getBean(SessionRepositoryFilter.class));
					assertThat((EnumSet<DispatcherType>) ReflectionTestUtils
							.getField(registration, "dispatcherTypes")).containsOnly(
									DispatcherType.ASYNC, DispatcherType.ERROR,
									DispatcherType.REQUEST);
				});
	}

	@Test
	public void filterOrderCanBeCustomizedWithCustomStore() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class)
				.withPropertyValues("spring.session.servlet.filter-order=123")
				.run((context) -> {
					FilterRegistrationBean<?> registration = context
							.getBean(FilterRegistrationBean.class);
					assertThat(registration.getOrder()).isEqualTo(123);
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void filterDispatcherTypesCanBeCustomized() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class)
				.withPropertyValues(
						"spring.session.servlet.filter-dispatcher-types=error, request")
				.run((context) -> {
					FilterRegistrationBean<?> registration = context
							.getBean(FilterRegistrationBean.class);
					assertThat((EnumSet<DispatcherType>) ReflectionTestUtils
							.getField(registration, "dispatcherTypes")).containsOnly(
									DispatcherType.ERROR, DispatcherType.REQUEST);
				});
	}

	@Configuration
	@EnableSpringHttpSession
	static class SessionRepositoryConfiguration {

		@Bean
		public MapSessionRepository mySessionRepository() {
			return new MapSessionRepository(Collections.emptyMap());
		}

	}

}
