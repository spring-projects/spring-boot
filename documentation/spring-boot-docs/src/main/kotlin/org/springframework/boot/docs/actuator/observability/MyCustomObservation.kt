/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.observability

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry;

import org.springframework.stereotype.Component

@Component
class MyCustomObservation(private val observationRegistry: ObservationRegistry) {

	fun doSomething() {
		Observation.createNotStarted("doSomething", observationRegistry)
			.lowCardinalityKeyValue("locale", "en-US")
			.highCardinalityKeyValue("userId", "42")
			.observe {
				// Execute business logic here
			}
	}

}
