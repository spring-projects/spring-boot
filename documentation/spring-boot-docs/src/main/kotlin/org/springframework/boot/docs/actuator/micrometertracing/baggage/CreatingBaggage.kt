/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.micrometertracing.baggage

import io.micrometer.tracing.Tracer
import org.springframework.stereotype.Component

@Component
class CreatingBaggage(private val tracer: Tracer) {

	fun doSomething() {
		tracer.createBaggageInScope("baggage1", "value1").use {
			// Business logic
		}
	}

}
