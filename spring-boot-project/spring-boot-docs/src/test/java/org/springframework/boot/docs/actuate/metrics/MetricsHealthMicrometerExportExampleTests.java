/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.docs.actuate.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetricsHealthMicrometerExportExample}.
 *
 * @author Phillip Webb
 */
@SpringBootTest
class MetricsHealthMicrometerExportExampleTests {

	@Autowired
	private MeterRegistry registry;

	@Test
	void registryExportsHealth() throws Exception {
		Gauge gauge = this.registry.get("health").gauge();
		assertThat(gauge.value()).isEqualTo(2);
	}

	@Configuration(proxyBeanMethods = false)
	@Import(MetricsHealthMicrometerExportExample.HealthMetricsConfiguration.class)
	@ImportAutoConfiguration(classes = { HealthContributorAutoConfiguration.class, MetricsAutoConfiguration.class,
			HealthEndpointAutoConfiguration.class })
	static class Config {

		@Bean
		MetricsHealthMicrometerExportExample example() {
			return new MetricsHealthMicrometerExportExample();
		}

		@Bean
		SimpleMeterRegistry simpleMeterRegistry() {
			return new SimpleMeterRegistry();
		}

		@Bean
		HealthIndicator outOfService() {
			return () -> new Health.Builder().outOfService().build();
		}

	}

}
