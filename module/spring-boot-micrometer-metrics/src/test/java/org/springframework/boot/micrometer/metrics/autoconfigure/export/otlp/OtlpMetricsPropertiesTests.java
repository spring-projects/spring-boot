/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp;

import io.micrometer.registry.otlp.OtlpConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryPropertiesTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OtlpMetricsProperties}.
 *
 * @author Eddú Meléndez
 */
class OtlpMetricsPropertiesTests extends StepRegistryPropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		OtlpMetricsProperties properties = new OtlpMetricsProperties();
		OtlpConfig config = OtlpConfig.DEFAULT;
		assertStepRegistryDefaultValues(properties, config);
		assertThat(properties.getAggregationTemporality()).isSameAs(config.aggregationTemporality());
		assertThat(properties.getCompressionMode()).isNull();
		assertThat(properties.getHistogramFlavor()).isSameAs(config.histogramFlavor());
		assertThat(properties.getMaxScale()).isEqualTo(config.maxScale());
		assertThat(properties.getMaxBucketCount()).isEqualTo(config.maxBucketCount());
		assertThat(properties.getBaseTimeUnit()).isSameAs(config.baseTimeUnit());
		assertThat(properties.getMeter()).isEmpty();
	}

}
