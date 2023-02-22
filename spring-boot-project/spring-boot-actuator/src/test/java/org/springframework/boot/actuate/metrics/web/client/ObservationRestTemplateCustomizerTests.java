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

package org.springframework.boot.actuate.metrics.web.client;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.http.client.observation.DefaultClientRequestObservationConvention;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ObservationRestTemplateCustomizer}.
 *
 * @author Brian Clozel
 */
class ObservationRestTemplateCustomizerTests {

	private static final String TEST_METRIC_NAME = "http.test.metric.name";

	private final ObservationRegistry observationRegistry = TestObservationRegistry.create();

	private final RestTemplate restTemplate = new RestTemplate();

	private final ObservationRestTemplateCustomizer customizer = new ObservationRestTemplateCustomizer(
			this.observationRegistry, new DefaultClientRequestObservationConvention(TEST_METRIC_NAME));

	@Test
	void shouldCustomizeObservationConfiguration() {
		this.customizer.customize(this.restTemplate);
		assertThat(this.restTemplate).hasFieldOrPropertyWithValue("observationRegistry", this.observationRegistry);
		assertThat(this.restTemplate).extracting("observationConvention")
			.isInstanceOf(DefaultClientRequestObservationConvention.class)
			.hasFieldOrPropertyWithValue("name", TEST_METRIC_NAME);
	}

}
