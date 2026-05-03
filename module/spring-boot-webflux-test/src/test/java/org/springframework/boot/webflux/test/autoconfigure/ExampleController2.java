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
