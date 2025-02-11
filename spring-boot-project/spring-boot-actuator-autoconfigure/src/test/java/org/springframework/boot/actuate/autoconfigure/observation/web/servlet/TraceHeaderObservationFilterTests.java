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

import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.test.simple.SimpleTracer;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TraceHeaderObservationFilter}.
 */
class TraceHeaderObservationFilterTests {

	TestObservationRegistry observationRegistry = TestObservationRegistry.create();

	@Test
	void shouldWriteTraceHeaderWhenCurrentTrace() throws Exception {
		TraceHeaderObservationFilter filter = createFilter(new SimpleTracer());
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(new MockHttpServletRequest(), response, getFilterChain());
		assertThat(response.getHeader("X-Trace-Id")).isNotEmpty();
	}

	@Test
	void shouldNotWriteTraceHeaderWhenNoCurrentTrace() throws Exception {
		TraceHeaderObservationFilter filter = createFilter(Tracer.NOOP);
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(new MockHttpServletRequest(), response, getFilterChain());
		assertThat(response.getHeaderNames()).doesNotContain("X-Trace-Id");
	}

	private TraceHeaderObservationFilter createFilter(Tracer tracer) {
		this.observationRegistry.observationConfig().observationHandler(new DefaultTracingObservationHandler(tracer));
		return new TraceHeaderObservationFilter(tracer, this.observationRegistry);
	}

	private static FilterChain getFilterChain() {
		return (servletRequest, servletResponse) -> servletResponse.getWriter().print("Hello");
	}

}
