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

package org.springframework.boot.actuate.autoconfigure.tracing;

import java.util.List;

import brave.CurrentSpanCustomizer;
import brave.SpanCustomizer;
import brave.Tracer;
import brave.Tracing;
import brave.Tracing.Builder;
import brave.TracingCustomizer;
import brave.handler.SpanHandler;
import brave.propagation.CurrentTraceContext;
import brave.propagation.CurrentTraceContextCustomizer;
import brave.propagation.Propagation.Factory;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.sampler.Sampler;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BravePropagator;
import io.micrometer.tracing.brave.bridge.BraveSpanCustomizer;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import io.micrometer.tracing.brave.bridge.CompositeSpanHandler;
import io.micrometer.tracing.exporter.SpanExportingPredicate;
import io.micrometer.tracing.exporter.SpanFilter;
import io.micrometer.tracing.exporter.SpanReporter;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties.Propagation.PropagationType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.IncompatibleConfigurationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Brave.
 *
 * @author Moritz Halbritter
 * @author Marcin Grzejszczak
 * @author Jonatan Ivanov
 * @since 3.0.0
 */
@AutoConfiguration(before = { MicrometerTracingAutoConfiguration.class, NoopTracerAutoConfiguration.class })
@ConditionalOnClass({ Tracer.class, BraveTracer.class })
@EnableConfigurationProperties(TracingProperties.class)
@Import({ BravePropagationConfigurations.PropagationWithoutBaggage.class,
		BravePropagationConfigurations.PropagationWithBaggage.class,
		BravePropagationConfigurations.NoPropagation.class })
public class BraveAutoConfiguration {

	/**
	 * Default value for application name if {@code spring.application.name} is not set.
	 */
	private static final String DEFAULT_APPLICATION_NAME = "application";

	private final TracingProperties tracingProperties;

	BraveAutoConfiguration(TracingProperties tracingProperties) {
		this.tracingProperties = tracingProperties;
	}

	@Bean
	@ConditionalOnMissingBean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	CompositeSpanHandler compositeSpanHandler(ObjectProvider<SpanExportingPredicate> predicates,
			ObjectProvider<SpanReporter> reporters, ObjectProvider<SpanFilter> filters) {
		return new CompositeSpanHandler(predicates.orderedStream().toList(), reporters.orderedStream().toList(),
				filters.orderedStream().toList());
	}

	@Bean
	@ConditionalOnMissingBean
	Tracing braveTracing(Environment environment, List<SpanHandler> spanHandlers,
			List<TracingCustomizer> tracingCustomizers, CurrentTraceContext currentTraceContext,
			Factory propagationFactory, Sampler sampler) {
		if (this.tracingProperties.getBrave().isSpanJoiningSupported()) {
			if (this.tracingProperties.getPropagation().getType() != null
					&& this.tracingProperties.getPropagation().getType().contains(PropagationType.W3C)) {
				throw new IncompatibleConfigurationException("management.tracing.propagation.type",
						"management.tracing.brave.span-joining-supported");
			}
			if (this.tracingProperties.getPropagation().getType() == null
					&& this.tracingProperties.getPropagation().getProduce().contains(PropagationType.W3C)) {
				throw new IncompatibleConfigurationException("management.tracing.propagation.produce",
						"management.tracing.brave.span-joining-supported");
			}
			if (this.tracingProperties.getPropagation().getType() == null
					&& this.tracingProperties.getPropagation().getConsume().contains(PropagationType.W3C)) {
				throw new IncompatibleConfigurationException("management.tracing.propagation.consume",
						"management.tracing.brave.span-joining-supported");
			}
		}
		String applicationName = environment.getProperty("spring.application.name", DEFAULT_APPLICATION_NAME);
		Builder builder = Tracing.newBuilder()
			.currentTraceContext(currentTraceContext)
			.traceId128Bit(true)
			.supportsJoin(this.tracingProperties.getBrave().isSpanJoiningSupported())
			.propagationFactory(propagationFactory)
			.sampler(sampler)
			.localServiceName(applicationName);
		spanHandlers.forEach(builder::addSpanHandler);
		for (TracingCustomizer tracingCustomizer : tracingCustomizers) {
			tracingCustomizer.customize(builder);
		}
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	brave.Tracer braveTracer(Tracing tracing) {
		return tracing.tracer();
	}

	@Bean
	@ConditionalOnMissingBean
	CurrentTraceContext braveCurrentTraceContext(List<CurrentTraceContext.ScopeDecorator> scopeDecorators,
			List<CurrentTraceContextCustomizer> currentTraceContextCustomizers) {
		ThreadLocalCurrentTraceContext.Builder builder = ThreadLocalCurrentTraceContext.newBuilder();
		scopeDecorators.forEach(builder::addScopeDecorator);
		for (CurrentTraceContextCustomizer currentTraceContextCustomizer : currentTraceContextCustomizers) {
			currentTraceContextCustomizer.customize(builder);
		}
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	Sampler braveSampler() {
		return Sampler.create(this.tracingProperties.getSampling().getProbability());
	}

	@Bean
	@ConditionalOnMissingBean(io.micrometer.tracing.Tracer.class)
	BraveTracer braveTracerBridge(brave.Tracer tracer, CurrentTraceContext currentTraceContext) {
		return new BraveTracer(tracer, new BraveCurrentTraceContext(currentTraceContext),
				new BraveBaggageManager(this.tracingProperties.getBaggage().getTagFields(),
						this.tracingProperties.getBaggage().getRemoteFields()));
	}

	@Bean
	@ConditionalOnMissingBean
	BravePropagator bravePropagator(Tracing tracing) {
		return new BravePropagator(tracing);
	}

	@Bean
	@ConditionalOnMissingBean(SpanCustomizer.class)
	CurrentSpanCustomizer currentSpanCustomizer(Tracing tracing) {
		return CurrentSpanCustomizer.create(tracing);
	}

	@Bean
	@ConditionalOnMissingBean(io.micrometer.tracing.SpanCustomizer.class)
	BraveSpanCustomizer braveSpanCustomizer(SpanCustomizer spanCustomizer) {
		return new BraveSpanCustomizer(spanCustomizer);
	}

}
