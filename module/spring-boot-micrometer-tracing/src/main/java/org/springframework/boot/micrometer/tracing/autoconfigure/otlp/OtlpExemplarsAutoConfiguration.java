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

import io.micrometer.registry.otlp.ExemplarContextProvider;
import io.micrometer.registry.otlp.OtlpExemplarContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.tracing.autoconfigure.MicrometerTracingAutoConfiguration;
import org.springframework.boot.micrometer.tracing.autoconfigure.TracingProperties;
import org.springframework.boot.micrometer.tracing.autoconfigure.TracingProperties.Exemplars.Filter;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Contract;
import org.springframework.util.function.SingletonSupplier;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OTLP (OpenTelemetry Protocol)
 * Exemplars with Micrometer Tracing.
 *
 * @author Jonatan Ivanov
 * @author Moritz Halbritter
 * @since 4.1.0
 */
@AutoConfiguration(
		beforeName = "org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsExportAutoConfiguration",
		after = MicrometerTracingAutoConfiguration.class)
@ConditionalOnBean(Tracer.class)
@ConditionalOnClass({ Tracer.class, ExemplarContextProvider.class })
@EnableConfigurationProperties(TracingProperties.class)
public final class OtlpExemplarsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ExemplarContextProvider exemplarContextProvider(ObjectProvider<Tracer> tracerProvider,
			TracingProperties properties) {
		return new LazyTracingExemplarContextProvider(tracerProvider, properties.getExemplars().getFilter());
	}

	/**
	 * Since the MeterRegistry can depend on the {@link Tracer} (Exemplars) and the
	 * {@link Tracer} can depend on the MeterRegistry (recording metrics), this
	 * {@link ExemplarContextProvider} breaks the cycle by lazily loading the
	 * {@link Tracer}.
	 */
	static class LazyTracingExemplarContextProvider implements ExemplarContextProvider {

		private final SingletonSupplier<Tracer> tracer;

		private final Filter filter;

		LazyTracingExemplarContextProvider(ObjectProvider<Tracer> tracerProvider, Filter filter) {
			this.tracer = SingletonSupplier.of(tracerProvider::getObject);
			this.filter = filter;
		}

		@Override
		public @Nullable OtlpExemplarContext getExemplarContext() {
			Span span = this.tracer.obtain().currentSpan();
			if (isExemplar(span)) {
				TraceContext context = span.context();
				return new OtlpExemplarContext(context.traceId(), context.spanId());
			}
			return null;
		}

		@Contract("null -> false")
		private boolean isExemplar(@Nullable Span span) {
			if (span == null) {
				return false;
			}
			return switch (this.filter) {
				case ALWAYS_ON -> true;
				case ALWAYS_OFF -> false;
				case SAMPLED_TRACES -> isSampled(span);
			};
		}

		private boolean isSampled(Span span) {
			Boolean sampled = span.context().sampled();
			return sampled != null && sampled;
		}

	}

}
