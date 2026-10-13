/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.observation;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.http.client.observation.ClientRequestObservationConvention;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient.Builder;

/**
 * {@link RestClientCustomizer} that configures the {@link Builder RestClient builder} to
 * record request observations.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
public class ObservationRestClientCustomizer implements RestClientCustomizer {

	private final ObservationRegistry observationRegistry;

	private final ClientRequestObservationConvention observationConvention;

	/**
	 * Create a new {@link ObservationRestClientCustomizer}.
	 * @param observationRegistry the observation registry
	 * @param observationConvention the observation convention
	 */
	public ObservationRestClientCustomizer(ObservationRegistry observationRegistry,
			ClientRequestObservationConvention observationConvention) {
		Assert.notNull(observationConvention, "'observationConvention' must not be null");
		Assert.notNull(observationRegistry, "'observationRegistry' must not be null");
		this.observationRegistry = observationRegistry;
		this.observationConvention = observationConvention;
	}

	@Override
	public void customize(Builder restClientBuilder) {
		restClientBuilder.observationRegistry(this.observationRegistry);
		restClientBuilder.observationConvention(this.observationConvention);
	}

}
