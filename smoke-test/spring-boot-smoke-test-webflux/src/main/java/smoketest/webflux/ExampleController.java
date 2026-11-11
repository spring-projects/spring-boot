/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.webflux;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ExampleController {

	@PostMapping(path = "/", consumes = { MediaType.APPLICATION_JSON_VALUE, "!application/xml" },
			produces = MediaType.TEXT_PLAIN_VALUE, headers = "X-Custom=Foo", params = "a!=alpha")
	public String example() {
		return "Hello World";
	}

	@GetMapping("/bad-request")
	Mono<String> badRequest() {
		return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST));
	}

	@GetMapping("five-hundred")
	void fiveHundred() {
		throw new RuntimeException("Expected!");
	}

}
