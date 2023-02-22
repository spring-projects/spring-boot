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

package org.springframework.boot.actuate.autoconfigure.quartz;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;

import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.actuate.quartz.QuartzEndpoint;
import org.springframework.boot.actuate.quartz.QuartzEndpointWebExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link QuartzEndpointAutoConfiguration}.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
class QuartzEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(QuartzEndpointAutoConfiguration.class));

	@Test
	void endpointIsAutoConfigured() {
		this.contextRunner.withBean(Scheduler.class, () -> mock(Scheduler.class))
			.withPropertyValues("management.endpoints.web.exposure.include=quartz")
			.run((context) -> assertThat(context).hasSingleBean(QuartzEndpoint.class));
	}

	@Test
	void endpointIsNotAutoConfiguredIfSchedulerIsNotAvailable() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=quartz")
			.run((context) -> assertThat(context).doesNotHaveBean(QuartzEndpoint.class));
	}

	@Test
	void endpointNotAutoConfiguredWhenNotExposed() {
		this.contextRunner.withBean(Scheduler.class, () -> mock(Scheduler.class))
			.run((context) -> assertThat(context).doesNotHaveBean(QuartzEndpoint.class));
	}

	@Test
	void endpointCanBeDisabled() {
		this.contextRunner.withBean(Scheduler.class, () -> mock(Scheduler.class))
			.withPropertyValues("management.endpoint.quartz.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(QuartzEndpoint.class));
	}

	@Test
	void endpointBacksOffWhenUserProvidedEndpointIsPresent() {
		this.contextRunner.withUserConfiguration(CustomEndpointConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(QuartzEndpoint.class).hasBean("customEndpoint"));
	}

	@Test
	void runWhenOnlyExposedOverJmxShouldHaveEndpointBeanWithoutWebExtension() {
		this.contextRunner.withBean(Scheduler.class, () -> mock(Scheduler.class))
			.withPropertyValues("management.endpoints.web.exposure.include=info", "spring.jmx.enabled=true",
					"management.endpoints.jmx.exposure.include=quartz")
			.run((context) -> assertThat(context).hasSingleBean(QuartzEndpoint.class)
				.doesNotHaveBean(QuartzEndpointWebExtension.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	void rolesCanBeConfiguredViaTheEnvironment() {
		this.contextRunner.withBean(Scheduler.class, () -> mock(Scheduler.class))
			.withPropertyValues("management.endpoint.quartz.roles: test")
			.withPropertyValues("management.endpoints.web.exposure.include=quartz")
			.withSystemProperties("dbPassword=123456", "apiKey=123456")
			.run((context) -> {
				assertThat(context).hasSingleBean(QuartzEndpointWebExtension.class);
				QuartzEndpointWebExtension endpoint = context.getBean(QuartzEndpointWebExtension.class);
				Set<String> roles = (Set<String>) ReflectionTestUtils.getField(endpoint, "roles");
				assertThat(roles).contains("test");
			});
	}

	@Test
	void showValuesCanBeConfiguredViaTheEnvironment() {
		this.contextRunner.withBean(Scheduler.class, () -> mock(Scheduler.class))
			.withPropertyValues("management.endpoint.quartz.show-values: WHEN_AUTHORIZED")
			.withPropertyValues("management.endpoints.web.exposure.include=quartz")
			.withSystemProperties("dbPassword=123456", "apiKey=123456")
			.run((context) -> {
				assertThat(context).hasSingleBean(QuartzEndpointWebExtension.class);
				assertThat(context.getBean(QuartzEndpointWebExtension.class)).extracting("showValues")
					.isEqualTo(Show.WHEN_AUTHORIZED);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomEndpointConfiguration {

		@Bean
		CustomEndpoint customEndpoint() {
			return new CustomEndpoint();
		}

	}

	private static final class CustomEndpoint extends QuartzEndpoint {

		private CustomEndpoint() {
			super(mock(Scheduler.class), Collections.emptyList());
		}

	}

}
