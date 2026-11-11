/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.observation.autoconfigure;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.observation.autoconfigure.ScheduledTasksObservationAutoConfiguration.ObservabilitySchedulingConfigurer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ScheduledTasksObservationAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class ScheduledTasksObservationAutoConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(AutoConfigurations
		.of(ObservationAutoConfiguration.class, ScheduledTasksObservationAutoConfiguration.class));

	@Test
	void shouldProvideObservabilitySchedulingConfigurer() {
		this.runner.run((context) -> assertThat(context).hasSingleBean(ObservabilitySchedulingConfigurer.class));
	}

	@Test
	void observabilitySchedulingConfigurerShouldConfigureObservationRegistry() {
		ObservationRegistry observationRegistry = ObservationRegistry.create();
		ObservabilitySchedulingConfigurer configurer = new ObservabilitySchedulingConfigurer(observationRegistry);
		ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
		configurer.configureTasks(registrar);
		assertThat(registrar.getObservationRegistry()).isEqualTo(observationRegistry);
	}

}
