/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
