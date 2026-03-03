/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.micrometer.tracing.autoconfigure.otlp;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.micrometer.tracing.autoconfigure.otlp.OtlpExemplarsAutoConfiguration.LazyTracingExemplarContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LazyTracingExemplarContextProvider}.
 *
 * @author Jonatan Ivanov
 */
class LazyTracingExemplarContextProviderTests {

	private final Tracer tracer = mock(Tracer.class);

	private final ObjectProvider<Tracer> objectProvider = new ObjectProvider<>() {
		@Override
		public Tracer getObject() throws BeansException {
			return LazyTracingExemplarContextProviderTests.this.tracer;
		}

		@Override
		public Tracer getObject(@Nullable Object... args) throws BeansException {
			return LazyTracingExemplarContextProviderTests.this.tracer;
		}

		@Override
		public Tracer getIfAvailable() throws BeansException {
			return LazyTracingExemplarContextProviderTests.this.tracer;
		}

		@Override
		public Tracer getIfUnique() throws BeansException {
			return LazyTracingExemplarContextProviderTests.this.tracer;
		}

	};

	private final LazyTracingExemplarContextProvider contextProvider = new LazyTracingExemplarContextProvider(
			this.objectProvider);

	@Test
	void whenCurrentSpanIsNullThenExemplarContextIsNull() {
		assertThat(this.contextProvider.getExemplarContext()).isNull();
	}

	@Test
	void whenCurrentSpanHasSpanIdThenSpanIdIsFromSpan() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(traceContext.sampled()).willReturn(true);
		given(traceContext.spanId()).willReturn("span-id");
		given(span.context()).willReturn(traceContext);
		assertThat(this.contextProvider.getExemplarContext()).isNotNull();
		assertThat(this.contextProvider.getExemplarContext().getSpanId()).isEqualTo("span-id");
	}

	@Test
	void whenCurrentSpanHasTraceIdThenTraceIdIsFromSpan() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(traceContext.sampled()).willReturn(true);
		given(traceContext.traceId()).willReturn("trace-id");
		given(span.context()).willReturn(traceContext);
		assertThat(this.contextProvider.getExemplarContext()).isNotNull();
		assertThat(this.contextProvider.getExemplarContext().getTraceId()).isEqualTo("trace-id");
	}

	@Test
	void whenCurrentSpanHasNoSpanIdThenSpanIdIsNull() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(traceContext.sampled()).willReturn(true);
		given(span.context()).willReturn(traceContext);
		assertThat(this.contextProvider.getExemplarContext()).isNotNull();
		assertThat(this.contextProvider.getExemplarContext().getSpanId()).isNull();
	}

	@Test
	void whenCurrentSpanHasNoTraceIdThenTraceIdIsNull() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(traceContext.sampled()).willReturn(true);
		given(span.context()).willReturn(traceContext);
		assertThat(this.contextProvider.getExemplarContext()).isNotNull();
		assertThat(this.contextProvider.getExemplarContext().getTraceId()).isNull();
	}

	@Test
	void whenCurrentSpanIsSampledThenExemplarContextIsNotNull() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(traceContext.sampled()).willReturn(true);
		given(span.context()).willReturn(traceContext);
		assertThat(this.contextProvider.getExemplarContext()).isNotNull();
	}

	@Test
	void whenCurrentSpanIsNotSampledThenExemplarContextIsNull() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(traceContext.sampled()).willReturn(false);
		given(span.context()).willReturn(traceContext);
		assertThat(this.contextProvider.getExemplarContext()).isNull();
	}

	@Test
	void whenCurrentSpanHasDeferredSamplingThenExemplarContextIsNull() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(traceContext.sampled()).willReturn(null);
		given(span.context()).willReturn(traceContext);
		assertThat(this.contextProvider.getExemplarContext()).isNull();
	}

}
