/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.scheduling;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ScheduledTasksEndpointAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class ScheduledTasksEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ScheduledTasksEndpointAutoConfiguration.class));

	@Test
	void endpointIsAutoConfigured() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=scheduledtasks")
			.run((context) -> assertThat(context).hasSingleBean(ScheduledTasksEndpoint.class));
	}

	@Test
	void endpointNotAutoConfiguredWhenNotExposed() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(ScheduledTasksEndpoint.class));
	}

	@Test
	void endpointCanBeDisabled() {
		this.contextRunner.withPropertyValues("management.endpoint.scheduledtasks.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(ScheduledTasksEndpoint.class));
	}

	@Test
	void endpointBacksOffWhenUserProvidedEndpointIsPresent() {
		this.contextRunner.withUserConfiguration(CustomEndpointConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(ScheduledTasksEndpoint.class)
				.hasBean("customEndpoint"));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomEndpointConfiguration {

		@Bean
		CustomEndpoint customEndpoint() {
			return new CustomEndpoint();
		}

	}

	private static final class CustomEndpoint extends ScheduledTasksEndpoint {

		private CustomEndpoint() {
			super(Collections.emptyList());
		}

	}

}
