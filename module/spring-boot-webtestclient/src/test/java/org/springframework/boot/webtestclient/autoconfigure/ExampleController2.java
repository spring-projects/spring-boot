/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webtestclient.autoconfigure;

import reactor.core.publisher.Mono;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example {@link Controller @Controller} used with tests.
 *
 * @author Stephane Nicoll
 */
@RestController
public class ExampleController2 {

	@GetMapping("/two")
	public Mono<String> two() {
		return Mono.just("two");
	}

	@GetMapping("/two/{id}")
	public Mono<String> two(@PathVariable ExampleId id) {
		return Mono.just(id.getId() + "two");
	}

}
