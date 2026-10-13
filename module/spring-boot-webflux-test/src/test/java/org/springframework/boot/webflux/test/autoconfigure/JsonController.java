/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.test.autoconfigure;

import reactor.core.publisher.Mono;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example {@link Controller @Controller} used with {@link WebFluxTest @WebFluxTest}
 * tests.
 *
 * @author Andy Wilkinson
 */
@RestController
public class JsonController {

	@GetMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ExamplePojo> json() {
		return Mono.just(new ExamplePojo("a", "b"));
	}

}
