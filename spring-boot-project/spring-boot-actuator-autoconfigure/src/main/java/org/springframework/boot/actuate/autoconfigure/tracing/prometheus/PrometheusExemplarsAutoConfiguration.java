/*
 * Copyright 2012-2024 the original author or authors.
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
import io.micrometer.tracing.Tracer;
import io.prometheus.metrics.tracer.common.SpanContext;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.util.function.SingletonSupplier;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Prometheus Exemplars with
 * Micrometer Tracing.
 *
 * @author Jonatan Ivanov
 * @since 3.0.0
 */
@AutoConfiguration(before = PrometheusMetricsExportAutoConfiguration.class,
		after = MicrometerTracingAutoConfiguration.class)
@ConditionalOnBean(Tracer.class)
@ConditionalOnClass({ Tracer.class, SpanContext.class })
public class PrometheusExemplarsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	SpanContext spanContext(ObjectProvider<Tracer> tracerProvider) {
		return new LazyTracingSpanContext(tracerProvider);
	}

	/**
	 * Since the MeterRegistry can depend on the {@link Tracer} (Exemplars) and the
	 * {@link Tracer} can depend on the MeterRegistry (recording metrics), this
	 * {@link SpanContext} breaks the cycle by lazily loading the {@link Tracer}.
	 */
	static class LazyTracingSpanContext implements SpanContext {

		private final SingletonSupplier<Tracer> tracer;

		LazyTracingSpanContext(ObjectProvider<Tracer> tracerProvider) {
			this.tracer = SingletonSupplier.of(tracerProvider::getObject);
		}

		@Override
		public String getCurrentTraceId() {
			Span currentSpan = currentSpan();
			return (currentSpan != null) ? currentSpan.context().traceId() : null;
		}

		@Override
		public String getCurrentSpanId() {
			Span currentSpan = currentSpan();
			return (currentSpan != null) ? currentSpan.context().spanId() : null;
		}

		@Override
		public boolean isCurrentSpanSampled() {
			Span currentSpan = currentSpan();
			if (currentSpan == null) {
				return false;
			}
			Boolean sampled = currentSpan.context().sampled();
			return sampled != null && sampled;
		}

		@Override
		public void markCurrentSpanAsExemplar() {
		}

		private Span currentSpan() {
			return this.tracer.obtain().currentSpan();
		}

	}

}
