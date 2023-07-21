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

package org.springframework.boot.actuate.autoconfigure.tracing.prometheus;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.tracing.prometheus.PrometheusExemplarsAutoConfiguration.LazyTracingSpanContextSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LazyTracingSpanContextSupplier}.
 *
 * @author Andy Wilkinson
 */
class LazyTracingSpanContextSupplierTests {

	private final Tracer tracer = mock(Tracer.class);

	private final ObjectProvider<Tracer> objectProvider = new ObjectProvider<>() {

		@Override
		public Tracer getObject() throws BeansException {
			return LazyTracingSpanContextSupplierTests.this.tracer;
		}

		@Override
		public Tracer getObject(Object... args) throws BeansException {
			return LazyTracingSpanContextSupplierTests.this.tracer;
		}

		@Override
		public Tracer getIfAvailable() throws BeansException {
			return LazyTracingSpanContextSupplierTests.this.tracer;
		}

		@Override
		public Tracer getIfUnique() throws BeansException {
			return LazyTracingSpanContextSupplierTests.this.tracer;
		}

	};

	private final LazyTracingSpanContextSupplier spanContextSupplier = new LazyTracingSpanContextSupplier(
			this.objectProvider);

	@Test
	void whenCurrentSpanIsNullThenSpanIdIsNull() {
		assertThat(this.spanContextSupplier.getSpanId()).isNull();
	}

	@Test
	void whenCurrentSpanIsNullThenTraceIdIsNull() {
		assertThat(this.spanContextSupplier.getTraceId()).isNull();
	}

	@Test
	void whenCurrentSpanIsNullThenSampledIsFalse() {
		assertThat(this.spanContextSupplier.isSampled()).isFalse();
	}

	@Test
	void whenCurrentSpanHasSpanIdThenSpanIdIsFromSpan() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(traceContext.spanId()).willReturn("span-id");
		given(span.context()).willReturn(traceContext);
		assertThat(this.spanContextSupplier.getSpanId()).isEqualTo("span-id");
	}

	@Test
	void whenCurrentSpanHasTraceIdThenTraceIdIsFromSpan() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(traceContext.traceId()).willReturn("trace-id");
		given(span.context()).willReturn(traceContext);
		assertThat(this.spanContextSupplier.getTraceId()).isEqualTo("trace-id");
	}

	@Test
	void whenCurrentSpanHasNoSpanIdThenSpanIdIsNull() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(span.context()).willReturn(traceContext);
		assertThat(this.spanContextSupplier.getSpanId()).isNull();
	}

	@Test
	void whenCurrentSpanHasNoTraceIdThenTraceIdIsNull() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(span.context()).willReturn(traceContext);
		assertThat(this.spanContextSupplier.getTraceId()).isNull();
	}

	@Test
	void whenCurrentSpanIsSampledThenSampledIsTrue() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(traceContext.sampled()).willReturn(true);
		given(span.context()).willReturn(traceContext);
		assertThat(this.spanContextSupplier.isSampled()).isTrue();
	}

	@Test
	void whenCurrentSpanIsNotSampledThenSampledIsFalse() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(traceContext.sampled()).willReturn(false);
		given(span.context()).willReturn(traceContext);
		assertThat(this.spanContextSupplier.isSampled()).isFalse();
	}

	@Test
	void whenCurrentSpanHasDeferredSamplingThenSampledIsFalse() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(traceContext.sampled()).willReturn(null);
		given(span.context()).willReturn(traceContext);
		assertThat(this.spanContextSupplier.isSampled()).isFalse();
	}

}
