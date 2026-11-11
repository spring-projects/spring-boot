/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.logging.logback;

import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LogbackMetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
class LogbackMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(SimpleMeterRegistry.class, SimpleMeterRegistry::new)
		.withConfiguration(AutoConfigurations.of(LogbackMetricsAutoConfiguration.class));

	@Test
	void autoConfiguresLogbackMetrics() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(LogbackMetrics.class));
	}

	@Test
	void allowsCustomLogbackMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomLogbackMetricsConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(LogbackMetrics.class).hasBean("customLogbackMetrics"));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomLogbackMetricsConfiguration {

		@Bean
		LogbackMetrics customLogbackMetrics() {
			return new LogbackMetrics();
		}

	}

}
