/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.observation.autoconfigure;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to enable observability for
 * scheduled tasks.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@AutoConfiguration(after = ObservationAutoConfiguration.class)
@ConditionalOnBean(ObservationRegistry.class)
@ConditionalOnClass(ThreadPoolTaskScheduler.class)
public final class ScheduledTasksObservationAutoConfiguration {

	@Bean
	ObservabilitySchedulingConfigurer observabilitySchedulingConfigurer(ObservationRegistry observationRegistry) {
		return new ObservabilitySchedulingConfigurer(observationRegistry);
	}

	static final class ObservabilitySchedulingConfigurer implements SchedulingConfigurer {

		private final ObservationRegistry observationRegistry;

		ObservabilitySchedulingConfigurer(ObservationRegistry observationRegistry) {
			this.observationRegistry = observationRegistry;
		}

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			taskRegistrar.setObservationRegistry(this.observationRegistry);
		}

	}

}
