/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.integration.autoconfigure.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.integration.autoconfigure.IntegrationAutoConfiguration;
import org.springframework.boot.integration.autoconfigure.IntegrationGraphEndpointAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IntegrationMetricsAutoConfiguration}.
 *
 * @author Artem Bilan
 */
class IntegrationMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(IntegrationAutoConfiguration.class,
				IntegrationGraphEndpointAutoConfiguration.class, IntegrationMetricsAutoConfiguration.class,
				MetricsAutoConfiguration.class))
		.withBean(SimpleMeterRegistry.class)
		.withPropertyValues("management.metrics.tags.someTag=someValue",
				"management.metrics.use-global-registry=false");

	@Test
	void integrationMetersAreInstrumented() {
		this.contextRunner.run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			Gauge gauge = registry.get("spring.integration.channels").tag("someTag", "someValue").gauge();
			assertThat(gauge).isNotNull().extracting(Gauge::value).isEqualTo(2.0);
		});
	}

}
