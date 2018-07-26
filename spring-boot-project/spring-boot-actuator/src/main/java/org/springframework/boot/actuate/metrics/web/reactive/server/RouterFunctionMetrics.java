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
import java.util.function.BiFunction;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import reactor.core.publisher.Mono;

import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Support class for WebFlux {@link RouterFunction}-related metrics.
 *
 * @author Jon Schneider
 * @since 2.0.0
 * @deprecated in favor of the auto-configured {@link MetricsWebFilter}
 */
@Deprecated
public class RouterFunctionMetrics {

	private final MeterRegistry registry;

	private final BiFunction<ServerRequest, ServerResponse, Iterable<Tag>> defaultTags;

	public RouterFunctionMetrics(MeterRegistry registry) {
		Assert.notNull(registry, "Registry must not be null");
		this.registry = registry;
		this.defaultTags = this::defaultTags;
	}

	private RouterFunctionMetrics(MeterRegistry registry,
			BiFunction<ServerRequest, ServerResponse, Iterable<Tag>> defaultTags) {
		Assert.notNull(registry, "Registry must not be null");
		Assert.notNull(defaultTags, "DefaultTags must not be null");
		this.registry = registry;
		this.defaultTags = defaultTags;
	}

	private Iterable<Tag> defaultTags(ServerRequest request, ServerResponse response) {
		if (response == null) {
			return Tags.of(getMethodTag(request));
		}
		return Tags.of(getMethodTag(request), getStatusTag(response));
	}

	/**
	 * Returns a new {@link RouterFunctionMetrics} instance with the specified default
	 * tags.
	 * @param defaultTags function to generate a list of tags to apply to the timer
	 * {@code ServerResponse} may be null
	 * @return {@code this} for further configuration
	 */
	public RouterFunctionMetrics defaultTags(
			BiFunction<ServerRequest, ServerResponse, Iterable<Tag>> defaultTags) {
		return new RouterFunctionMetrics(this.registry, defaultTags);
	}

	public HandlerFilterFunction<ServerResponse, ServerResponse> timer(String name) {
		return timer(name, Tags.empty());
	}

	public HandlerFilterFunction<ServerResponse, ServerResponse> timer(String name,
			String... tags) {
		return timer(name, Tags.of(tags));
	}

	public HandlerFilterFunction<ServerResponse, ServerResponse> timer(String name,
			Iterable<Tag> tags) {
		return new MetricsFilter(name, Tags.of(tags));
	}

	/**
	 * Creates a {@code method} tag from the method of the given {@code request}.
	 * @param request the HTTP request
	 * @return a "method" tag whose value is a capitalized method (e.g. GET)
	 */
	public static Tag getMethodTag(ServerRequest request) {
		return Tag.of("method", request.method().toString());
	}

	/**
	 * Creates a {@code status} tag from the status of the given {@code response}.
	 * @param response the HTTP response
	 * @return a "status" tag whose value is the numeric status code
	 */
	public static Tag getStatusTag(ServerResponse response) {
		return Tag.of("status", response.statusCode().toString());
	}

	/**
	 * {@link HandlerFilterFunction} to handle calling micrometer.
	 */
	private class MetricsFilter
			implements HandlerFilterFunction<ServerResponse, ServerResponse> {

		private final String name;

		private final Tags tags;

		MetricsFilter(String name, Tags tags) {
			this.name = name;
			this.tags = tags;
		}

		@Override
		public Mono<ServerResponse> filter(ServerRequest request,
				HandlerFunction<ServerResponse> next) {
			long start = System.nanoTime();
			return next.handle(request)
					.doOnSuccess((response) -> timer(start, request, response))
					.doOnError((error) -> timer(start, request, null));
		}

		private Iterable<Tag> getDefaultTags(ServerRequest request,
				ServerResponse response) {
			return RouterFunctionMetrics.this.defaultTags.apply(request, response);
		}

		private void timer(long start, ServerRequest request, ServerResponse response) {
			Tags allTags = this.tags.and(getDefaultTags(request, response));
			RouterFunctionMetrics.this.registry.timer(this.name, allTags)
					.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
		}

	}

}
