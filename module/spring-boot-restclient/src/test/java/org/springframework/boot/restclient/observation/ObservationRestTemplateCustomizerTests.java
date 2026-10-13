/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.observation;

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
 * @deprecated since 4.2.0 for removal in 4.4.0
 */
@Deprecated(since = "4.2.0", forRemoval = true)
@SuppressWarnings("removal")
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
