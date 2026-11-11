/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.logging.logback;

import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.logging.ConfigureClasspathToPreferLog4j2;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LogbackMetricsAutoConfiguration} when both Log4j2 and Logback are on
 * the classpath.
 *
 * @author Andy Wilkinson
 */
@ConfigureClasspathToPreferLog4j2
class LogbackMetricsAutoConfigurationWithLog4j2AndLogbackTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(SimpleMeterRegistry.class, SimpleMeterRegistry::new)
		.withConfiguration(AutoConfigurations.of(LogbackMetricsAutoConfiguration.class));

	@Test
	void doesNotConfigureLogbackMetrics() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(LogbackMetrics.class));
	}

}
