/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.observation;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.boot.restclient.RestTemplateCustomizer;
import org.springframework.http.client.observation.ClientRequestObservationConvention;
import org.springframework.web.client.RestTemplate;

/**
 * {@link RestTemplateCustomizer} that configures the {@link RestTemplate} to record
 * request observations.
 *
 * @author Brian Clozel
 * @since 4.0.0
 * @deprecated since 4.2.0 for removal in 4.4.0 in favor of
 * {@link ObservationRestClientCustomizer}
 */
@Deprecated(since = "4.2.0", forRemoval = true)
@SuppressWarnings("removal")
public class ObservationRestTemplateCustomizer implements RestTemplateCustomizer {

	private final ObservationRegistry observationRegistry;

	private final ClientRequestObservationConvention observationConvention;

	/**
	 * Create a new {@code ObservationRestTemplateCustomizer}.
	 * @param observationConvention the observation convention
	 * @param observationRegistry the observation registry
	 */
	public ObservationRestTemplateCustomizer(ObservationRegistry observationRegistry,
			ClientRequestObservationConvention observationConvention) {
		this.observationConvention = observationConvention;
		this.observationRegistry = observationRegistry;
	}

	@Override
	public void customize(RestTemplate restTemplate) {
		restTemplate.setObservationConvention(this.observationConvention);
		restTemplate.setObservationRegistry(this.observationRegistry);
	}

}
