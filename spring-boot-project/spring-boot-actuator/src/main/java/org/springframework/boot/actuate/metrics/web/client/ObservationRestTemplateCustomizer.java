/*
 * Copyright 2012-2022 the original author or authors.
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

import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.client.observation.ClientRequestObservationConvention;
import org.springframework.web.client.RestTemplate;

/**
 * {@link RestTemplateCustomizer} that configures the {@link RestTemplate} to record
 * request observations.
 *
 * @author Brian Clozel
 * @since 3.0.0
 */
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
