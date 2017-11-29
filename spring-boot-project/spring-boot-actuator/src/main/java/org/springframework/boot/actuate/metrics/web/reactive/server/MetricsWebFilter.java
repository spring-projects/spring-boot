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

package org.springframework.boot.actuate.metrics.web.reactive.server;

import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * Intercepts incoming HTTP requests modeled with the Webflux annotation-based programming
 * model.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
public class MetricsWebFilter implements WebFilter {

	private final MeterRegistry registry;

	private final WebFluxTagsProvider tagsProvider;

	private final String metricName;

	public MetricsWebFilter(MeterRegistry registry, WebFluxTagsProvider tagsProvider,
			String metricName) {
		this.registry = registry;
		this.tagsProvider = tagsProvider;
		this.metricName = metricName;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return chain.filter(exchange).compose((call) -> filter(exchange, call));
	}

	private Publisher<Void> filter(ServerWebExchange exchange, Mono<Void> call) {
		long start = System.nanoTime();
		return call.doOnSuccess((done) -> success(exchange, start))
				.doOnError((cause) -> error(exchange, start, cause));
	}

	private void success(ServerWebExchange exchange, long start) {
		Iterable<Tag> tags = this.tagsProvider.httpRequestTags(exchange, null);
		this.registry.timer(this.metricName, tags).record(System.nanoTime() - start,
				TimeUnit.NANOSECONDS);
	}

	private void error(ServerWebExchange exchange, long start, Throwable cause) {
		Iterable<Tag> tags = this.tagsProvider.httpRequestTags(exchange, cause);
		this.registry.timer(this.metricName, tags).record(System.nanoTime() - start,
				TimeUnit.NANOSECONDS);
	}

}
