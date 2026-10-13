/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.reactive.webflux

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Suppress("UNUSED_PARAMETER")
@Component
class MyUserHandler {

	fun getUser(request: ServerRequest?): Mono<ServerResponse> {
		/**/ return ServerResponse.ok().build()
	}

	fun getUserCustomers(request: ServerRequest?): Mono<ServerResponse> {
		/**/ return ServerResponse.ok().build()
	}

	fun deleteUser(request: ServerRequest?): Mono<ServerResponse> {
		/**/ return ServerResponse.ok().build()
	}

}

