/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.test.autoconfigure;

import reactor.core.publisher.Mono;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example {@link Controller @Controller} used with {@link WebFluxTest @WebFluxTest}
 * tests.
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

	@PostMapping("/two/{id}")
	@ResponseBody
	public Mono<ExampleResult> twoUpdate(@PathVariable String id) {
		return Mono.just(new ExampleResult(id));
	}

	@PostMapping("/two2/{id}")
	@ResponseBody
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	public Mono<ExampleResult2> two2Update(@PathVariable String id) {
		return Mono.just(new ExampleResult2(id));
	}

}
