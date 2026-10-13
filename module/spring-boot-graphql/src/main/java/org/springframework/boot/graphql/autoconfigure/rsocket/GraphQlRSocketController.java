/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.graphql.autoconfigure.rsocket;

import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.graphql.server.GraphQlRSocketHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
class GraphQlRSocketController {

	private final GraphQlRSocketHandler handler;

	GraphQlRSocketController(GraphQlRSocketHandler handler) {
		this.handler = handler;
	}

	@MessageMapping("${spring.graphql.rsocket.mapping}")
	Mono<Map<String, Object>> handle(Map<String, Object> payload) {
		return this.handler.handle(payload);
	}

	@MessageMapping("${spring.graphql.rsocket.mapping}")
	Flux<Map<String, Object>> handleSubscription(Map<String, Object> payload) {
		return this.handler.handleSubscription(payload);
	}

}
