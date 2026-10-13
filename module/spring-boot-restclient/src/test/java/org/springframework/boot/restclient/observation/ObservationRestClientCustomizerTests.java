/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.observation;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.http.client.observation.DefaultClientRequestObservationConvention;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ObservationRestClientCustomizer}.
 *
 * @author Brian Clozel
 * @author Moritz Halbritter
 */
class ObservationRestClientCustomizerTests {

	private static final String TEST_METRIC_NAME = "http.test.metric.name";

	private final ObservationRegistry observationRegistry = TestObservationRegistry.create();

	private final RestClient.Builder restClientBuilder = RestClient.builder();

	private final ObservationRestClientCustomizer customizer = new ObservationRestClientCustomizer(
			this.observationRegistry, new DefaultClientRequestObservationConvention(TEST_METRIC_NAME));

	@Test
	void shouldCustomizeObservationConfiguration() {
		this.customizer.customize(this.restClientBuilder);
		assertThat(this.restClientBuilder).hasFieldOrPropertyWithValue("observationRegistry", this.observationRegistry);
		assertThat(this.restClientBuilder).extracting("observationConvention")
			.isInstanceOf(DefaultClientRequestObservationConvention.class)
			.hasFieldOrPropertyWithValue("name", TEST_METRIC_NAME);
	}

}
