/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.autoconfigure.prometheus;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.micrometer.tracing.autoconfigure.TracingProperties.Exemplars.Include;
import org.springframework.boot.micrometer.tracing.autoconfigure.prometheus.PrometheusExemplarsAutoConfiguration.LazyTracingSpanContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LazyTracingSpanContext}.
 *
 * @author Andy Wilkinson
 */
class LazyTracingSpanContextTests {

	private final Tracer tracer = mock(Tracer.class);

	private final ObjectProvider<Tracer> objectProvider = new ObjectProvider<>() {

		@Override
		public Tracer getObject() throws BeansException {
			return LazyTracingSpanContextTests.this.tracer;
		}

		@Override
		public Tracer getObject(@Nullable Object... args) throws BeansException {
			return LazyTracingSpanContextTests.this.tracer;
		}

		@Override
		public Tracer getIfAvailable() throws BeansException {
			return LazyTracingSpanContextTests.this.tracer;
		}

		@Override
		public Tracer getIfUnique() throws BeansException {
			return LazyTracingSpanContextTests.this.tracer;
		}

	};

	private LazyTracingSpanContext spanContext = new LazyTracingSpanContext(this.objectProvider,
			Include.SAMPLED_TRACES);

	@Test
	void whenCurrentSpanIsNullThenSpanIdIsNull() {
		assertThat(this.spanContext.getCurrentSpanId()).isNull();
	}

	@Test
	void whenCurrentSpanIsNullThenTraceIdIsNull() {
		assertThat(this.spanContext.getCurrentTraceId()).isNull();
	}

	@Test
	void whenCurrentSpanIsNullThenSampledIsFalse() {
		assertThat(this.spanContext.isCurrentSpanSampled()).isFalse();
	}

	@Test
	void whenCurrentSpanHasSpanIdThenSpanIdIsFromSpan() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(traceContext.spanId()).willReturn("span-id");
		given(span.context()).willReturn(traceContext);
		assertThat(this.spanContext.getCurrentSpanId()).isEqualTo("span-id");
	}

	@Test
	void whenCurrentSpanHasTraceIdThenTraceIdIsFromSpan() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(traceContext.traceId()).willReturn("trace-id");
		given(span.context()).willReturn(traceContext);
		assertThat(this.spanContext.getCurrentTraceId()).isEqualTo("trace-id");
	}

	@Test
	void whenCurrentSpanHasNoSpanIdThenSpanIdIsNull() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(span.context()).willReturn(traceContext);
		assertThat(this.spanContext.getCurrentSpanId()).isNull();
	}

	@Test
	void whenCurrentSpanHasNoTraceIdThenTraceIdIsNull() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(span.context()).willReturn(traceContext);
		assertThat(this.spanContext.getCurrentTraceId()).isNull();
	}

	@Test
	void whenCurrentSpanIsSampledThenSampledIsTrue() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(traceContext.sampled()).willReturn(true);
		given(span.context()).willReturn(traceContext);
		assertThat(this.spanContext.isCurrentSpanSampled()).isTrue();
	}

	@Test
	void whenCurrentSpanIsNotSampledThenSampledIsFalse() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(traceContext.sampled()).willReturn(false);
		given(span.context()).willReturn(traceContext);
		assertThat(this.spanContext.isCurrentSpanSampled()).isFalse();
	}

	@Test
	void whenCurrentSpanHasDeferredSamplingThenSampledIsFalse() {
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(traceContext.sampled()).willReturn(null);
		given(span.context()).willReturn(traceContext);
		assertThat(this.spanContext.isCurrentSpanSampled()).isFalse();
	}

	@Test
	void whenIncludeIsAllThenConstructorThrows() {
		assertThatExceptionOfType(InvalidConfigurationPropertyValueException.class)
			.isThrownBy(() -> new LazyTracingSpanContext(this.objectProvider, Include.ALL));
	}

	@Test
	void whenIncludeIsNoneAndSpanIsSampledThenSampledIsFalse() {
		this.spanContext = new LazyTracingSpanContext(this.objectProvider, Include.NONE);
		Span span = mock(Span.class);
		given(this.tracer.currentSpan()).willReturn(span);
		TraceContext traceContext = mock(TraceContext.class);
		given(traceContext.sampled()).willReturn(true);
		given(span.context()).willReturn(traceContext);
		assertThat(this.spanContext.isCurrentSpanSampled()).isFalse();
	}

}
