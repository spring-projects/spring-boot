/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.actuator.maphealthindicatorstometrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetricsHealthMicrometerExport}.
 *
 * @author Phillip Webb
 */
@SpringBootTest
class MetricsHealthMicrometerExportTests {

	@Autowired
	private MeterRegistry registry;

	@Test
	void registryExportsHealth() {
		Gauge gauge = this.registry.get("health").gauge();
		assertThat(gauge.value()).isEqualTo(2);
	}

	@Configuration(proxyBeanMethods = false)
	@Import(MyHealthMetricsExportConfiguration.class)
	@ImportAutoConfiguration({ HealthContributorAutoConfiguration.class,
			HealthContributorRegistryAutoConfiguration.class, MetricsAutoConfiguration.class,
			HealthEndpointAutoConfiguration.class })
	static class Config {

		@Bean
		MetricsHealthMicrometerExport example() {
			return new MetricsHealthMicrometerExport();
		}

		@Bean
		SimpleMeterRegistry simpleMeterRegistry() {
			return new SimpleMeterRegistry();
		}

		@Bean
		HealthIndicator outOfService() {
			return () -> Health.outOfService().build();
		}

	}

}
