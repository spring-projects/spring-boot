/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.reactive.webflux;

import reactor.core.publisher.Mono;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

@Component
public class MyUserHandler {

	public Mono<ServerResponse> getUser(ServerRequest request) {
		/**/ return ServerResponse.ok().build();
	}

	public Mono<ServerResponse> getUserCustomers(ServerRequest request) {
		/**/ return ServerResponse.ok().build();
	}

	public Mono<ServerResponse> deleteUser(ServerRequest request) {
		/**/ return ServerResponse.ok().build();
	}

}
