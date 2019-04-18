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

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
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

	private final double[] percentiles;

	private final boolean histogram;

	/**
	 * Create a new {@code MetricsWebFilter}.
	 * @param registry the registry to which metrics are recorded
	 * @param tagsProvider provider for metrics tags
	 * @param metricName name of the metric to record
	 * @param autoTimeRequests if requests should be automatically timed
	 * @deprecated since 2.2.0 in favor of
	 * {@link #MetricsWebFilter(MeterRegistry, WebFluxTagsProvider, String, boolean, List, boolean)}
	 */
	@Deprecated
	public MetricsWebFilter(MeterRegistry registry, WebFluxTagsProvider tagsProvider,
			String metricName, boolean autoTimeRequests) {
		this(registry, tagsProvider, metricName, autoTimeRequests, null, false);
	}

	/**
	 * Create a new {@code MetricsWebFilter}.
	 * @param registry the registry to which metrics are recorded
	 * @param tagsProvider provider for metrics tags
	 * @param metricName name of the metric to record
	 * @param autoTimeRequests if requests should be automatically timed
	 * @param percentileList percentiles for auto time requests
	 * @param histogram histogram or not for auto time requests
	 * @since 2.2.0
	 */
	public MetricsWebFilter(MeterRegistry registry, WebFluxTagsProvider tagsProvider,
			String metricName, boolean autoTimeRequests, List<Double> percentileList,
			boolean histogram) {

		double[] percentiles = (percentileList != null)
				? percentileList.stream().mapToDouble(Double::doubleValue).toArray()
				: null;

		this.registry = registry;
		this.tagsProvider = tagsProvider;
		this.metricName = metricName;
		this.autoTimeRequests = autoTimeRequests;
		this.percentiles = percentiles;
		this.histogram = histogram;
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
		return call.doOnSuccess((done) -> record(exchange, start, null))
				.doOnError((cause) -> {
					if (response.isCommitted()) {
						record(exchange, start, cause);
					}
					else {
						response.beforeCommit(() -> {
							record(exchange, start, cause);
							return Mono.empty();
						});
					}
				});
	}

	private void record(ServerWebExchange exchange, long start, Throwable cause) {
		Iterable<Tag> tags = this.tagsProvider.httpRequestTags(exchange, cause);
		Timer.builder(this.metricName).tags(tags).publishPercentiles(this.percentiles)
				.publishPercentileHistogram(this.histogram).register(this.registry)
				.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
	}

}
