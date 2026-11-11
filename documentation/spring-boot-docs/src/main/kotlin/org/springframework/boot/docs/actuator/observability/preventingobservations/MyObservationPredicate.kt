/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.observability.preventingobservations

import io.micrometer.observation.Observation.Context
import io.micrometer.observation.ObservationPredicate
import org.springframework.stereotype.Component

@Component
class MyObservationPredicate : ObservationPredicate {

	override fun test(name: String, context: Context): Boolean {
		return !name.contains("denied")
	}

}
