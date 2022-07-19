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

package org.springframework.boot.actuate.autoconfigure.observation;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.TimerObservationHandler;
import io.micrometer.observation.ObservationRegistry;

/**
 * Registers the {@link TimerObservationHandler} with an {@link ObservationRegistry}.
 *
 * @author Moritz Halbritter
 */
class TimerObservationHandlerObservationRegistryCustomizer
		implements ObservationRegistryCustomizer<ObservationRegistry> {

	private final MeterRegistry meterRegistry;

	TimerObservationHandlerObservationRegistryCustomizer(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	@Override
	public void customize(ObservationRegistry registry) {
		registry.observationConfig().observationHandler(new TimerObservationHandler(this.meterRegistry));
	}

}
