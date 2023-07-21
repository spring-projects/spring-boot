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

import java.util.List;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.scheduling.ScheduledTasksObservabilityAutoConfiguration.ObservabilitySchedulingConfigurer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ScheduledTasksObservabilityAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class ScheduledTasksObservabilityAutoConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(AutoConfigurations
		.of(ObservationAutoConfiguration.class, ScheduledTasksObservabilityAutoConfiguration.class));

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

	@Test
	void isRegisteredInAutoConfigurationsFile() {
		List<String> configurations = ImportCandidates.load(AutoConfiguration.class, null).getCandidates();
		assertThat(configurations).contains(ScheduledTasksObservabilityAutoConfiguration.class.getName());
	}

}
