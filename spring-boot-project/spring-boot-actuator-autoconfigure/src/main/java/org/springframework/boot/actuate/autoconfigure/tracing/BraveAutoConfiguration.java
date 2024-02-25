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

	/**
	 * Constructs a new instance of BraveAutoConfiguration with the specified
	 * TracingProperties.
	 * @param tracingProperties the TracingProperties to be used for configuration
	 */
	BraveAutoConfiguration(TracingProperties tracingProperties) {
		this.tracingProperties = tracingProperties;
	}

	/**
	 * Creates a composite span handler bean if no other bean of the same type is present.
	 * The composite span handler is responsible for handling spans by applying
	 * predicates, reporters, and filters. The order of execution is determined by the
	 * precedence of the beans.
	 * @param predicates an object provider for span exporting predicates
	 * @param reporters an object provider for span reporters
	 * @param filters an object provider for span filters
	 * @return the composite span handler bean
	 */
	@Bean
	@ConditionalOnMissingBean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	CompositeSpanHandler compositeSpanHandler(ObjectProvider<SpanExportingPredicate> predicates,
			ObjectProvider<SpanReporter> reporters, ObjectProvider<SpanFilter> filters) {
		return new CompositeSpanHandler(predicates.orderedStream().toList(), reporters.orderedStream().toList(),
				filters.orderedStream().toList());
	}

	/**
	 * Creates a Tracing bean using Brave library.
	 * @param environment The environment object.
	 * @param spanHandlers The list of SpanHandler objects.
	 * @param tracingCustomizers The list of TracingCustomizer objects.
	 * @param currentTraceContext The CurrentTraceContext object.
	 * @param propagationFactory The Factory object for propagation.
	 * @param sampler The Sampler object.
	 * @return The Tracing bean.
	 * @throws IncompatibleConfigurationException if span joining is supported and W3C
	 * propagation is configured.
	 */
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

	/**
	 * Creates a Brave Tracer bean if no other bean of type Tracer is present.
	 * @param tracing the Tracing instance used to create the Tracer
	 * @return the Brave Tracer bean
	 */
	@Bean
	@ConditionalOnMissingBean
	brave.Tracer braveTracer(Tracing tracing) {
		return tracing.tracer();
	}

	/**
	 * Creates a bean of type CurrentTraceContext if no other bean of the same type is
	 * present.
	 * @param scopeDecorators The list of scope decorators to be added to the builder
	 * @param currentTraceContextCustomizers The list of customizers to customize the
	 * builder
	 * @return The created CurrentTraceContext bean
	 */
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

	/**
	 * Creates a sampler for Brave tracing based on the probability specified in the
	 * tracing properties. If no sampler bean is already defined, this method will be used
	 * to create one.
	 * @return the created sampler
	 */
	@Bean
	@ConditionalOnMissingBean
	Sampler braveSampler() {
		return Sampler.create(this.tracingProperties.getSampling().getProbability());
	}

	/**
	 * Creates a BraveTracer bean if no other bean of type io.micrometer.tracing.Tracer is
	 * present.
	 * @param tracer The Brave Tracer instance to be used.
	 * @param currentTraceContext The CurrentTraceContext instance to be used.
	 * @return A BraveTracer instance.
	 */
	@Bean
	@ConditionalOnMissingBean(io.micrometer.tracing.Tracer.class)
	BraveTracer braveTracerBridge(brave.Tracer tracer, CurrentTraceContext currentTraceContext) {
		return new BraveTracer(tracer, new BraveCurrentTraceContext(currentTraceContext),
				new BraveBaggageManager(this.tracingProperties.getBaggage().getTagFields()));
	}

	/**
	 * Creates a new instance of BravePropagator if there is no existing bean of the same
	 * type.
	 * @param tracing the Tracing instance used for creating the BravePropagator
	 * @return a new instance of BravePropagator
	 */
	@Bean
	@ConditionalOnMissingBean
	BravePropagator bravePropagator(Tracing tracing) {
		return new BravePropagator(tracing);
	}

	/**
	 * Creates a CurrentSpanCustomizer bean if there is no existing bean of type
	 * SpanCustomizer. This bean is created using the provided Tracing bean.
	 * @param tracing the Tracing bean used to create the CurrentSpanCustomizer bean
	 * @return the CurrentSpanCustomizer bean
	 */
	@Bean
	@ConditionalOnMissingBean(SpanCustomizer.class)
	CurrentSpanCustomizer currentSpanCustomizer(Tracing tracing) {
		return CurrentSpanCustomizer.create(tracing);
	}

	/**
	 * Creates a BraveSpanCustomizer bean if there is no existing bean of type
	 * io.micrometer.tracing.SpanCustomizer.
	 * @param spanCustomizer the existing SpanCustomizer bean
	 * @return a new BraveSpanCustomizer bean
	 */
	@Bean
	@ConditionalOnMissingBean(io.micrometer.tracing.SpanCustomizer.class)
	BraveSpanCustomizer braveSpanCustomizer(SpanCustomizer spanCustomizer) {
		return new BraveSpanCustomizer(spanCustomizer);
	}

}
