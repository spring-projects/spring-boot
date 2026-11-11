/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webclient.observation;

import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.web.reactive.function.client.ClientRequestObservationConvention;
import org.springframework.web.reactive.function.client.DefaultClientRequestObservationConvention;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ObservationWebClientCustomizer}
 *
 * @author Brian Clozel
 */
class ObservationWebClientCustomizerTests {

	private static final String TEST_METRIC_NAME = "http.test.metric.name";

	private final TestObservationRegistry observationRegistry = TestObservationRegistry.create();

	private final ClientRequestObservationConvention observationConvention = new DefaultClientRequestObservationConvention(
			TEST_METRIC_NAME);

	private final ObservationWebClientCustomizer customizer = new ObservationWebClientCustomizer(
			this.observationRegistry, this.observationConvention);

	private final WebClient.Builder clientBuilder = WebClient.builder();

	@Test
	void shouldCustomizeObservationConfiguration() {
		this.customizer.customize(this.clientBuilder);
		assertThat(this.clientBuilder).hasFieldOrPropertyWithValue("observationRegistry", this.observationRegistry);
		assertThat(this.clientBuilder).extracting("observationConvention")
			.isInstanceOf(DefaultClientRequestObservationConvention.class)
			.hasFieldOrPropertyWithValue("name", TEST_METRIC_NAME);
	}

}
