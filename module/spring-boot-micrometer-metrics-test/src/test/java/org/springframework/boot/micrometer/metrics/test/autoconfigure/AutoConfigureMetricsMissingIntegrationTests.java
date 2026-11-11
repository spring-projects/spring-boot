/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.test.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify behaviour when
 * {@link AutoConfigureMetrics @AutoConfigureMetrics} is not present on the test class.
 *
 * @author Chris Bono
 * @author Moritz Halbritter
 */
@SpringBootTest
class AutoConfigureMetricsMissingIntegrationTests {

	@Test
	void customizerRunsAndOnlyEnablesSimpleMeterRegistryWhenNoAnnotationPresent(
			@Autowired ApplicationContext applicationContext) {
		assertThat(applicationContext.getBean(MeterRegistry.class)).isInstanceOf(SimpleMeterRegistry.class);
		assertThat(applicationContext.getBeansOfType(PrometheusMeterRegistry.class)).isEmpty();
	}

	@Test
	void customizerRunsAndSetsExclusionPropertiesWhenNoAnnotationPresent(@Autowired Environment environment) {
		assertThat(environment.getProperty("management.defaults.metrics.export.enabled")).isEqualTo("false");
		assertThat(environment.getProperty("management.simple.metrics.export.enabled")).isEqualTo("true");
	}

}
