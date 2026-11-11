/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.rsocket.requester

import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class MyService(rsocketRequesterBuilder: RSocketRequester.Builder) {

	private val rsocketRequester: RSocketRequester

	init {
		rsocketRequester = rsocketRequesterBuilder.tcp("example.org", 9898)
	}

	fun someRSocketCall(name: String): Mono<User> {
		return rsocketRequester.route("user").data(name).retrieveMono(
			User::class.java
		)
	}

}

