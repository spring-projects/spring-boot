/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.actuate.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
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
