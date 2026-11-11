/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.micrometertracing.baggage;

import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.Tracer;

import org.springframework.stereotype.Component;

@Component
class CreatingBaggage {

	private final Tracer tracer;

	CreatingBaggage(Tracer tracer) {
		this.tracer = tracer;
	}

	void doSomething() {
		try (BaggageInScope scope = this.tracer.createBaggageInScope("baggage1", "value1")) {
			// Business logic
		}
	}

}
