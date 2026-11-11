/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.test.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MetricsContextCustomizerFactory} in a real Spring test
 * context, verifying that auto-configured {@link MeterRegistry meter registries} are kept
 * out of {@link Metrics#globalRegistry} by default and can be opted back in with
 * {@link AutoConfigureMetrics @AutoConfigureMetrics} or the
 * {@code management.metrics.use-global-registry} property.
 *
 * @author Moritz Halbritter
 * @author Lordwill Kandiro
 */
class GlobalMeterRegistryIntegrationTests {

	@SpringJUnitConfig
	@Import({ MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class })
	@DirtiesContext
	static class WhenNotAnnotatedTests {

		@Autowired
		private MeterRegistry meterRegistry;

		@Test
		void meterRegistryIsNotAddedToTheGlobalRegistry() {
			assertThat(Metrics.globalRegistry.getRegistries()).doesNotContain(this.meterRegistry);
		}

	}

	@SpringJUnitConfig
	@Import({ MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class })
	@AutoConfigureMetrics(useGlobalRegistry = true)
	@DirtiesContext
	static class WhenAnnotatedWithTrueUseGlobalRegistryAttributeTests {

		@Autowired
		private MeterRegistry meterRegistry;

		@Test
		void meterRegistryIsAddedToTheGlobalRegistry() {
			assertThat(Metrics.globalRegistry.getRegistries()).contains(this.meterRegistry);
		}

	}

	@SpringJUnitConfig
	@Import({ MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class })
	@TestPropertySource(properties = "management.metrics.use-global-registry=true")
	@DirtiesContext
	static class WhenPropertyIsExplicitlyEnabledTests {

		@Autowired
		private MeterRegistry meterRegistry;

		@Test
		void meterRegistryIsAddedToTheGlobalRegistry() {
			assertThat(Metrics.globalRegistry.getRegistries()).contains(this.meterRegistry);
		}

	}

}
