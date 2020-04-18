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

package org.springframework.boot.actuate.autoconfigure.health;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicatorRegistry;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to ensure that the legacy {@link HealthIndicatorRegistry} can still be
 * injected.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("deprecation")
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
public class HealthIndicatorRegistryInjectionIntegrationTests {

	// gh-18194

	@Test
	void meterRegistryBeanHasBeenConfigured(@Autowired MeterRegistry meterRegistry) {
		assertThat(meterRegistry).isNotNull();
		assertThat(meterRegistry.get("health").gauge()).isNotNull();
	}

	@Configuration
	@ImportAutoConfiguration({ HealthEndpointAutoConfiguration.class, HealthContributorAutoConfiguration.class,
			CompositeMeterRegistryAutoConfiguration.class, MetricsAutoConfiguration.class })
	static class Config {

		Config(HealthAggregator healthAggregator, HealthIndicatorRegistry healthIndicatorRegistry,
				MeterRegistry registry) {
			CompositeHealthIndicator healthIndicator = new CompositeHealthIndicator(healthAggregator,
					healthIndicatorRegistry);
			Gauge.builder("health", healthIndicator, this::getGaugeValue)
					.description("Spring boot health indicator.  3=UP, 2=OUT_OF_SERVICE, 1=DOWN, 0=UNKNOWN")
					.strongReference(true).register(registry);
		}

		private double getGaugeValue(CompositeHealthIndicator health) {
			Status status = health.health().getStatus();
			switch (status.getCode()) {
			case "UP":
				return 3;
			case "OUT_OF_SERVICE":
				return 2;
			case "DOWN":
				return 1;
			case "UNKNOWN":
			default:
				return 0;
			}
		}

	}

}
