/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.reactor;

import reactor.core.publisher.Flux;

/**
 * Utility class that should be instrumented by the reactor debug agent.
 *
 * @author Brian Clozel
 * @see ReactorEnvironmentPostProcessorTests
 */
class InstrumentedFluxProvider {

	Flux<Integer> newFluxJust() {
		return Flux.just(1);
	}

}
