/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.endpoints.health.reactivehealthindicators;

import reactor.core.publisher.Mono;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class MyReactiveHealthIndicator implements ReactiveHealthIndicator {

	@Override
	public Mono<Health> health() {
		// @formatter:off
		return doHealthCheck().onErrorResume((exception) ->
			Mono.just(new Health.Builder().down(exception).build()));
		// @formatter:on
	}

	private Mono<Health> doHealthCheck() {
		// perform some specific health check
		return /**/ null;
	}

}
