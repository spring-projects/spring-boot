/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnEnabledMetricsExport}.
 *
 * @author Chris Bono
 */
class ConditionalOnEnabledMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(
				AutoConfigurations.of(MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class))
		.withPropertyValues("management.metrics.use-global-registry=false");

	@Test
	void exporterIsEnabledByDefault() {
		this.contextRunner.run((context) -> assertThat(context).hasBean("simpleMeterRegistry"));
	}

	@Test
	void exporterCanBeSpecificallyDisabled() {
		this.contextRunner.withPropertyValues("management.simple.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean("simpleMeterRegistry"));
	}

	@Test
	void exporterCanBeGloballyDisabled() {
		this.contextRunner.withPropertyValues("management.defaults.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean("simpleMeterRegistry"));
	}

	@Test
	void exporterCanBeGloballyDisabledWithSpecificOverride() {
		this.contextRunner
			.withPropertyValues("management.defaults.metrics.export.enabled=false",
					"management.simple.metrics.export.enabled=true")
			.run((context) -> assertThat(context).hasBean("simpleMeterRegistry"));
	}

}
