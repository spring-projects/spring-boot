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

package org.springframework.boot.autoconfigure.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link FlywayAutoConfiguration} with Flyway 7.x.
 *
 * @author Andy Wilkinson
 */
@ClassPathOverrides("org.flywaydb:flyway-core:7.15.0")
class Flyway7xAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
			.withPropertyValues("spring.datasource.generate-unique-name=true");

	@Test
	void defaultFlyway() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(Flyway.class);
			Flyway flyway = context.getBean(Flyway.class);
			assertThat(flyway.getConfiguration().getLocations())
					.containsExactly(new Location("classpath:db/migration"));
		});
	}

	@Test
	void callbacksAreConfigured() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class, CallbackConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(Flyway.class);
					Flyway flyway = context.getBean(Flyway.class);
					Callback callbackOne = context.getBean("callbackOne", Callback.class);
					Callback callbackTwo = context.getBean("callbackTwo", Callback.class);
					assertThat(flyway.getConfiguration().getCallbacks()).hasSize(2);
					assertThat(flyway.getConfiguration().getCallbacks()).containsExactlyInAnyOrder(callbackTwo,
							callbackOne);
					verify(callbackOne, atLeastOnce()).handle(any(Event.class), any(Context.class));
					verify(callbackTwo, atLeastOnce()).handle(any(Event.class), any(Context.class));
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class CallbackConfiguration {

		@Bean
		Callback callbackOne() {
			return mockCallback();
		}

		@Bean
		Callback callbackTwo() {
			return mockCallback();
		}

		private Callback mockCallback() {
			Callback callback = mock(Callback.class);
			given(callback.supports(any(Event.class), any(Context.class))).willReturn(true);
			given(callback.getCallbackName()).willReturn("callback");
			return callback;
		}

	}

}
