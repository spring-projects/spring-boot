/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.micrometertracing.creatingspans

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import org.springframework.stereotype.Component

@Component
class CustomObservation(private val observationRegistry: ObservationRegistry) {

	fun someOperation() {
		Observation.createNotStarted("some-operation", observationRegistry)
			.lowCardinalityKeyValue("some-tag", "some-value")
			.observe {
				// Business logic ...
			}
	}

}
