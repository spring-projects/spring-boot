/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.logging.log4j2;

import io.micrometer.core.instrument.binder.logging.Log4j2Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.slf4j.SLF4JLoggerContext;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Log4J2MetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class Log4J2MetricsWithSlf4jLoggerContextAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(SimpleMeterRegistry.class, SimpleMeterRegistry::new)
		.withConfiguration(AutoConfigurations.of(Log4J2MetricsAutoConfiguration.class));

	@Test
	void backsOffWhenLoggerContextIsBackedBySlf4j() {
		assertThat(LogManager.getContext()).isInstanceOf(SLF4JLoggerContext.class);
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(Log4j2Metrics.class));
	}

}
