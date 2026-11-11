/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webclient.observation;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.web.reactive.function.client.ClientRequestObservationConvention;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link WebClientCustomizer} that configures the {@link WebClient} to record request
 * observations.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
public class ObservationWebClientCustomizer implements WebClientCustomizer {

	private final ObservationRegistry observationRegistry;

	private final ClientRequestObservationConvention observationConvention;

	/**
	 * Create a new {@code ObservationWebClientCustomizer} that will configure the
	 * {@code Observation} setup on the client.
	 * @param observationRegistry the registry to publish observations to
	 * @param observationConvention the convention to use to populate observations
	 */
	public ObservationWebClientCustomizer(ObservationRegistry observationRegistry,
			ClientRequestObservationConvention observationConvention) {
		this.observationRegistry = observationRegistry;
		this.observationConvention = observationConvention;
	}

	@Override
	public void customize(WebClient.Builder webClientBuilder) {
		webClientBuilder.observationRegistry(this.observationRegistry)
			.observationConvention(this.observationConvention);
	}

}
