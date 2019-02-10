/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.webflux;

import java.time.Duration;
import java.util.stream.Stream;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

@Component
public class EchoHandler {

	private String echo = "test";

	public Mono<ServerResponse> echo(ServerRequest request) {

		return request.bodyToMono(String.class).flatMap((echo) -> {
			this.echo = echo;
			return ServerResponse.ok().body(Mono.just(echo), String.class);
		}).switchIfEmpty(ServerResponse.badRequest().build());
	}

	public Mono<ServerResponse> getEcho(ServerRequest request) {

		Stream<Integer> stream = Stream.iterate(0, (i) -> i + 1);
		Flux<String> mapFlux = Flux.fromStream(stream)
				.zipWith(Flux.interval(Duration.ofSeconds(1))).map((i) -> this.echo);
		return ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(mapFlux,
				String.class);
	}

}
