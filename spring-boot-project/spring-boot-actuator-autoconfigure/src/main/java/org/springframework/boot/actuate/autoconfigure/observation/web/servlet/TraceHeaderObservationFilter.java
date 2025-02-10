/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.observation.web.servlet;

import io.micrometer.observation.Observation.Scope;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.server.observation.ServerRequestObservationConvention;
import org.springframework.util.Assert;
import org.springframework.web.filter.ServerHttpObservationFilter;

/**
 * {@link ServerHttpObservationFilter} that writes the current {@link Span} in an HTTP
 * response header. By default, the {@code "X-Trace-Id"} header is used.
 *
 * @author Brian Clozel
 * @since 3.5.0
 */
public class TraceHeaderObservationFilter extends ServerHttpObservationFilter {

	private static final String TRACE_ID_HEADER_NAME = "X-Trace-Id";

	private final Tracer tracer;

	/**
	 * Create a {@link TraceHeaderObservationFilter} that will write the
	 * {@code "X-Trace-Id"} HTTP response header.
	 * @param tracer the current tracer
	 * @param observationRegistry the current observation registry
	 */
	public TraceHeaderObservationFilter(Tracer tracer, ObservationRegistry observationRegistry) {
		super(observationRegistry);
		Assert.notNull(tracer, "Tracer must not be null");
		this.tracer = tracer;
	}

	/**
	 * Create a {@link TraceHeaderObservationFilter} that will write the
	 * {@code "X-Trace-Id"} HTTP response header.
	 * @param tracer the current tracer
	 * @param observationRegistry the current observation registry
	 * @param observationConvention the custom observation convention to use.
	 */
	public TraceHeaderObservationFilter(Tracer tracer, ObservationRegistry observationRegistry,
			ServerRequestObservationConvention observationConvention) {
		super(observationRegistry, observationConvention);
		Assert.notNull(tracer, "Tracer must not be null");
		this.tracer = tracer;
	}

	@Override
	protected void onScopeOpened(Scope scope, HttpServletRequest request, HttpServletResponse response) {
		Span currentSpan = this.tracer.currentSpan();
		if (currentSpan != null && !currentSpan.isNoop()) {
			response.setHeader(TRACE_ID_HEADER_NAME, currentSpan.context().traceId());
		}
	}

}
