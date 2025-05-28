/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.metrics.autoconfigure.export.otlp;

import io.micrometer.registry.otlp.OtlpConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.metrics.autoconfigure.export.properties.StepRegistryPropertiesTests;

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
		assertThat(properties.getHistogramFlavor()).isSameAs(config.histogramFlavor());
		assertThat(properties.getMaxScale()).isEqualTo(config.maxScale());
		assertThat(properties.getMaxBucketCount()).isEqualTo(config.maxBucketCount());
		assertThat(properties.getBaseTimeUnit()).isSameAs(config.baseTimeUnit());
		assertThat(properties.getMeter()).isEmpty();
	}

}
