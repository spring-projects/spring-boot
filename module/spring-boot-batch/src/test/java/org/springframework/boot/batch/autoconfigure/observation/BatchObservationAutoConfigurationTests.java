/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.batch.autoconfigure.observation;

import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.configuration.annotation.BatchObservabilityBeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BatchObservationAutoConfiguration}.
 *
 * @author Mark Bonnekessel
 */
class BatchObservationAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(TestObservationRegistry.class, TestObservationRegistry::create)
		.withConfiguration(AutoConfigurations.of(BatchObservationAutoConfiguration.class));

	@Test
	void backsOffWhenObservationRegistryIsMissing() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(BatchObservationAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(BatchObservabilityBeanPostProcessor.class));
	}

	@Test
	void beanIsPresentWhenSpringBatchIsPresent() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(BatchObservabilityBeanPostProcessor.class));
	}

}
