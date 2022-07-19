/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.metrics.AutoTimer;
import org.springframework.boot.actuate.metrics.annotation.TimedAnnotations;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * Intercepts incoming HTTP requests handled by Spring WebFlux handlers and records
 * metrics about execution time and results.
 *
 * @author Jon Schneider
 * @author Brian Clozel
 * @since 2.0.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class MetricsWebFilter implements WebFilter {

	private static Log logger = LogFactory.getLog(MetricsWebFilter.class);

	private final MeterRegistry registry;

	private final WebFluxTagsProvider tagsProvider;

	private final String metricName;

	private final AutoTimer autoTimer;

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
		return chain.filter(exchange).transformDeferred((call) -> filter(exchange, call));
	}

	private Publisher<Void> filter(ServerWebExchange exchange, Mono<Void> call) {
		long start = System.nanoTime();
		return call.doOnEach((signal) -> onTerminalSignal(exchange, signal.getThrowable(), start))
				.doOnCancel(() -> onTerminalSignal(exchange, new CancelledServerWebExchangeException(), start));
	}

	private void onTerminalSignal(ServerWebExchange exchange, Throwable cause, long start) {
		ServerHttpResponse response = exchange.getResponse();
		if (response.isCommitted() || cause instanceof CancelledServerWebExchangeException) {
			record(exchange, cause, start);
		}
		else {
			response.beforeCommit(() -> {
				record(exchange, cause, start);
				return Mono.empty();
			});
		}
	}

	private void record(ServerWebExchange exchange, Throwable cause, long start) {
		try {
			cause = (cause != null) ? cause : exchange.getAttribute(ErrorAttributes.ERROR_ATTRIBUTE);
			Object handler = exchange.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
			Set<Timed> annotations = getTimedAnnotations(handler);
			Iterable<Tag> tags = this.tagsProvider.httpRequestTags(exchange, cause);
			long duration = System.nanoTime() - start;
			AutoTimer.apply(this.autoTimer, this.metricName, annotations,
					(builder) -> builder.description("Duration of HTTP server request handling").tags(tags)
							.register(this.registry).record(duration, TimeUnit.NANOSECONDS));
		}
		catch (Exception ex) {
			logger.warn("Failed to record timer metrics", ex);
			// Allow exchange to continue, unaffected by metrics problem
		}
	}

	private Set<Timed> getTimedAnnotations(Object handler) {
		if (handler instanceof HandlerMethod handlerMethod) {
			return TimedAnnotations.get(handlerMethod.getMethod(), handlerMethod.getBeanType());
		}
		return Collections.emptySet();
	}

}
