/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.scheduling;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
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
 * @since 3.2.0
 */
@AutoConfiguration(after = ObservationAutoConfiguration.class)
@ConditionalOnBean(ObservationRegistry.class)
@ConditionalOnClass(ThreadPoolTaskScheduler.class)
public class ScheduledTasksObservabilityAutoConfiguration {

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
