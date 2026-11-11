/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.logging;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LoggersEndpointAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class LoggersEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(LoggersEndpointAutoConfiguration.class))
		.withUserConfiguration(LoggingConfiguration.class);

	@Test
	void runShouldHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=loggers")
			.run((context) -> assertThat(context).hasSingleBean(LoggersEndpoint.class));
	}

	@Test
	void runWhenEnabledPropertyIsFalseShouldNotHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoint.loggers.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(LoggersEndpoint.class));
	}

	@Test
	void runWhenNotExposedShouldNotHaveEndpointBean() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(LoggersEndpoint.class));
	}

	@Test
	void runWithNoneLoggingSystemShouldNotHaveEndpointBean() {
		this.contextRunner.withSystemProperties("org.springframework.boot.logging.LoggingSystem=none")
			.run((context) -> assertThat(context).doesNotHaveBean(LoggersEndpoint.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class LoggingConfiguration {

		@Bean
		LoggingSystem loggingSystem() {
			return mock(LoggingSystem.class);
		}

	}

}
