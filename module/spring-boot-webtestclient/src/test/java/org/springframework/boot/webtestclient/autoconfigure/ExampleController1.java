/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webtestclient.autoconfigure;

import reactor.core.publisher.Mono;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example {@link Controller @Controller} used with tests.
 *
 * @author Stephane Nicoll
 */
@RestController
public class ExampleController1 {

	@GetMapping("/one")
	public Mono<String> one() {
		return Mono.just("one");
	}

	@GetMapping("/one/error")
	public Mono<String> error() {
		throw new RuntimeException("foo");
	}

}
