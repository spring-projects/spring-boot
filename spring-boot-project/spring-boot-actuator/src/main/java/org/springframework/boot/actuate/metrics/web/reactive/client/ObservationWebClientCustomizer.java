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

package org.springframework.boot.actuate.metrics.web.reactive.client;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.web.reactive.function.client.ClientRequestObservationConvention;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link WebClientCustomizer} that configures the {@link WebClient} to record request
 * observations.
 *
 * @author Brian Clozel
 * @since 3.0.0
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
