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

package org.springframework.boot.micrometer.metrics.autoconfigure.export.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoggingMetricsExportPropertiesConfigAdapter}.
 *
 * @author Vasily Pelikh
 */
class LoggingMetricsExportPropertiesConfigAdapterTests {

	private LoggingMetricsExportProperties properties;

	@BeforeEach
	void setUp() {
		this.properties = new LoggingMetricsExportProperties();
	}

	@Test
	void whenPropertiesAggregationTemporalityIsNotSetAdapterAggregationTemporalityReturnsCumulative() {
		assertThat(createAdapter().logInactive()).isFalse();
	}

	@Test
	void whenPropertiesAggregationTemporalityIsSetAdapterAggregationTemporalityReturnsIt() {
		this.properties.setLogInactive(true);
		assertThat(createAdapter().logInactive()).isTrue();
	}

	private LoggingMetricsExportPropertiesConfigAdapter createAdapter() {
		return new LoggingMetricsExportPropertiesConfigAdapter(this.properties);
	}

}
