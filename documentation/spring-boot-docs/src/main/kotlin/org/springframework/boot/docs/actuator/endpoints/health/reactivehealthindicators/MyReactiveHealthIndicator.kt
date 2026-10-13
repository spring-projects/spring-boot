/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.endpoints.health.reactivehealthindicators

import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.ReactiveHealthIndicator
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class MyReactiveHealthIndicator : ReactiveHealthIndicator {

	override fun health(): Mono<Health> {
		// @formatter:off
		return doHealthCheck().onErrorResume { exception: Throwable ->
			Mono.just(Health.Builder().down(exception).build())
		}
		// @formatter:on
	}

	private fun doHealthCheck(): Mono<Health> {
		// perform some specific health check
		return  /**/ Mono.empty()
	}

}

