/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify behaviour when
 * {@link AutoConfigureMetrics @AutoConfigureMetrics} is present on the test class.
 *
 * @author Chris Bono
 * @author Moritz Halbritter
 */
@SpringBootTest
@AutoConfigureMetrics
class AutoConfigureMetricsPresentIntegrationTests {

	@Test
	void customizerDoesNotDisableAvailableMeterRegistriesWhenAnnotationPresent(
			@Autowired ApplicationContext applicationContext) {
		assertThat(applicationContext.getBeansOfType(io.micrometer.prometheusmetrics.PrometheusMeterRegistry.class))
			.hasSize(1);
	}

	@Test
	void customizerDoesNotSetExclusionPropertiesWhenAnnotationPresent(@Autowired Environment environment) {
		assertThat(environment.containsProperty("management.defaults.metrics.export.enabled")).isFalse();
		assertThat(environment.containsProperty("management.simple.metrics.export.enabled")).isFalse();
		assertThat(environment.containsProperty("management.tracing.export.enabled")).isFalse();
	}

}
