/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.web.reactive.webclient;

import reactor.core.publisher.Mono;

import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example {@link Controller} used with {@link WebFluxTest} tests.
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
