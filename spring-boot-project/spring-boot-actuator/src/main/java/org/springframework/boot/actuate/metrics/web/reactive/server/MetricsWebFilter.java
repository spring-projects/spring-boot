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

package org.springframework.boot.actuate.metrics.web.reactive.server;

import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.metrics.AutoTimer;
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

	private final AutoTimer autoTimer;

	/**
	 * Create a new {@code MetricsWebFilter}.
	 * @param registry the registry to which metrics are recorded
	 * @param tagsProvider provider for metrics tags
	 * @param metricName name of the metric to record
	 * @param autoTimeRequests if requests should be automatically timed
	 * @deprecated since 2.2.0 in favor of
	 * {@link #MetricsWebFilter(MeterRegistry, WebFluxTagsProvider, String, AutoTimer)}
	 */
	@Deprecated
	public MetricsWebFilter(MeterRegistry registry, WebFluxTagsProvider tagsProvider, String metricName,
			boolean autoTimeRequests) {
		this(registry, tagsProvider, metricName, AutoTimer.ENABLED);
	}

	/**
	 * Create a new {@code MetricsWebFilter}.
	 * @param registry the registry to which metrics are recorded
	 * @param tagsProvider provider for metrics tags
	 * @param metricName name of the metric to record
	 * @param autoTimer the auto-timers to apply or {@code null} to disable auto-timing
	 * @since 2.2.0
	 */
	public MetricsWebFilter(MeterRegistry registry, WebFluxTagsProvider tagsProvider, String metricName,
			AutoTimer autoTimer) {
		this.registry = registry;
		this.tagsProvider = tagsProvider;
		this.metricName = metricName;
		this.autoTimer = (autoTimer != null) ? autoTimer : AutoTimer.DISABLED;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		if (!this.autoTimer.isEnabled()) {
			return chain.filter(exchange);
		}
		return chain.filter(exchange).compose((call) -> filter(exchange, call));
	}

	private Publisher<Void> filter(ServerWebExchange exchange, Mono<Void> call) {
		long start = System.nanoTime();
		return call.doOnSuccess((done) -> onSuccess(exchange, start))
				.doOnError((cause) -> onError(exchange, start, cause));
	}

	private void onSuccess(ServerWebExchange exchange, long start) {
		record(exchange, start, null);
	}

	private void onError(ServerWebExchange exchange, long start, Throwable cause) {
		ServerHttpResponse response = exchange.getResponse();
		if (response.isCommitted()) {
			record(exchange, start, cause);
		}
		else {
			response.beforeCommit(() -> {
				record(exchange, start, cause);
				return Mono.empty();
			});
		}
	}

	private void record(ServerWebExchange exchange, long start, Throwable cause) {
		Iterable<Tag> tags = this.tagsProvider.httpRequestTags(exchange, cause);
		this.autoTimer.builder(this.metricName).tags(tags).register(this.registry).record(System.nanoTime() - start,
				TimeUnit.NANOSECONDS);
	}

}
