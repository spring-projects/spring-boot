/*
 * Copyright 2012-2018 the original author or authors.
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

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * Intercepts incoming HTTP requests handled by Spring WebFlux handlers.
 *
 * @author Jon Schneider
 * @author Brian Clozel
 * @since 2.0.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class MetricsWebFilter implements WebFilter {

	private final MeterRegistry registry;

	private final WebFluxTagsProvider tagsProvider;

	private final String metricName;

	private final boolean autoTimeRequests;

	/**
	 * Create a new {@code MetricsWebFilter}.
	 * @param registry the registry to which metrics are recorded
	 * @param tagsProvider provider for metrics tags
	 * @param metricName name of the metric to record
	 * @deprecated since 2.0.6 in favor of
	 * {@link #MetricsWebFilter(MeterRegistry, WebFluxTagsProvider, String, boolean)}
	 */
	@Deprecated
	public MetricsWebFilter(MeterRegistry registry, WebFluxTagsProvider tagsProvider,
			String metricName) {
		this(registry, tagsProvider, metricName, true);
	}

	public MetricsWebFilter(MeterRegistry registry, WebFluxTagsProvider tagsProvider,
			String metricName, boolean autoTimeRequests) {
		this.registry = registry;
		this.tagsProvider = tagsProvider;
		this.metricName = metricName;
		this.autoTimeRequests = autoTimeRequests;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		if (this.autoTimeRequests) {
			return chain.filter(exchange).compose((call) -> filter(exchange, call));
		}
		return chain.filter(exchange);
	}

	private Publisher<Void> filter(ServerWebExchange exchange, Mono<Void> call) {
		long start = System.nanoTime();
		ServerHttpResponse response = exchange.getResponse();
		return call.doOnSuccess((done) -> success(exchange, start)).doOnError((cause) -> {
			if (response.isCommitted()) {
				error(exchange, start, cause);
			}
			else {
				response.beforeCommit(() -> {
					error(exchange, start, cause);
					return Mono.empty();
				});
			}
		});
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
