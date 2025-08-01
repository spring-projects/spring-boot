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

package org.springframework.boot.opentelemetry.autoconfigure.metrics;

import org.junit.jupiter.api.Test;

import org.springframework.boot.opentelemetry.autoconfigure.metrics.OpenTelemetryMetricsProperties.ExemplarFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryMetricsProperties}.
 *
 * @author Thomas Vitale
 */
class OpenTelemetryMetricsPropertiesTests {

	@Test
	void shouldCreateInstanceWithDefaultValues() {
		OpenTelemetryMetricsProperties properties = new OpenTelemetryMetricsProperties();
		assertThat(properties.getExemplars().isEnabled()).isFalse();
		assertThat(properties.getExemplars().getFilter()).isEqualTo(ExemplarFilter.TRACE_BASED);
		assertThat(properties.getCardinalityLimit()).isEqualTo(2000);
	}

	@Test
	void shouldUpdateCardinalityLimit() {
		OpenTelemetryMetricsProperties properties = new OpenTelemetryMetricsProperties();
		properties.setCardinalityLimit(3000);
		assertThat(properties.getCardinalityLimit()).isEqualTo(3000);
	}

	@Test
	void shouldUpdateExemplars() {
		OpenTelemetryMetricsProperties properties = new OpenTelemetryMetricsProperties();
		properties.getExemplars().setEnabled(true);
		properties.getExemplars().setFilter(ExemplarFilter.ALWAYS_ON);
		assertThat(properties.getExemplars().isEnabled()).isTrue();
		assertThat(properties.getExemplars().getFilter()).isEqualTo(ExemplarFilter.ALWAYS_ON);
	}

}
