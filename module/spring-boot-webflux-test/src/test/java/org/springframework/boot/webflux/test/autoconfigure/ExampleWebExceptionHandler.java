/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.test.autoconfigure;

import reactor.core.publisher.Mono;

import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

/**
 * Example {@link WebExceptionHandler} used with {@link WebFluxTest @WebFluxTest} tests.
 *
 * @author Madhura Bhave
 */
@Component
@Order(-2)
public class ExampleWebExceptionHandler implements WebExceptionHandler {

	private final ErrorWebExceptionHandler fallback;

	public ExampleWebExceptionHandler(ErrorWebExceptionHandler fallback) {
		this.fallback = fallback;
	}

	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
		if (ex instanceof RuntimeException && "foo".equals(ex.getMessage())) {
			exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
			return exchange.getResponse().setComplete();
		}
		return this.fallback.handle(exchange, ex);
	}

}
