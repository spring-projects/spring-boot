/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.extension.trace.propagation.B3Propagator;

import org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties.Propagation.PropagationType;

/**
 * {@link TextMapPropagator} which supports multiple tracing formats. It is able to
 * configure different formats for injecting and for extracting.
 *
 * @author Moritz Halbritter
 * @author Scott Frederick
 */
class CompositeTextMapPropagator implements TextMapPropagator {

	private final Collection<TextMapPropagator> injectors;

	private final Collection<TextMapPropagator> extractors;

	private final TextMapPropagator baggagePropagator;

	private final Set<String> fields;

	/**
	 * Creates a new {@link CompositeTextMapPropagator}.
	 * @param injectors the injectors
	 * @param mutuallyExclusiveExtractors the mutually exclusive extractors. They are
	 * applied in order, and as soon as an extractor extracts a context, the other
	 * extractors after it are no longer invoked
	 * @param baggagePropagator the baggage propagator to use, or {@code null}
	 */
	CompositeTextMapPropagator(Collection<TextMapPropagator> injectors,
			Collection<TextMapPropagator> mutuallyExclusiveExtractors, TextMapPropagator baggagePropagator) {
		this.injectors = injectors;
		this.extractors = mutuallyExclusiveExtractors;
		this.baggagePropagator = baggagePropagator;
		Set<String> fields = new LinkedHashSet<>();
		fields(this.injectors).forEach(fields::add);
		fields(this.extractors).forEach(fields::add);
		if (baggagePropagator != null) {
			fields.addAll(baggagePropagator.fields());
		}
		this.fields = Collections.unmodifiableSet(fields);
	}

	private Stream<String> fields(Collection<TextMapPropagator> propagators) {
		return propagators.stream().flatMap((propagator) -> propagator.fields().stream());
	}

	Collection<TextMapPropagator> getInjectors() {
		return this.injectors;
	}

	Collection<TextMapPropagator> getExtractors() {
		return this.extractors;
	}

	@Override
	public Collection<String> fields() {
		return this.fields;
	}

	@Override
	public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
		if (context != null && setter != null) {
			this.injectors.forEach((injector) -> injector.inject(context, carrier, setter));
		}
	}

	@Override
	public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
		if (context == null) {
			return Context.root();
		}
		if (getter == null) {
			return context;
		}
		Context result = this.extractors.stream()
			.map((extractor) -> extractor.extract(context, carrier, getter))
			.filter((extracted) -> extracted != context)
			.findFirst()
			.orElse(context);
		if (this.baggagePropagator != null) {
			result = this.baggagePropagator.extract(result, carrier, getter);
		}
		return result;
	}

	/**
	 * Creates a new {@link CompositeTextMapPropagator}.
	 * @param properties the tracing properties
	 * @param baggagePropagator the baggage propagator to use, or {@code null}
	 * @return the {@link CompositeTextMapPropagator}
	 */
	static TextMapPropagator create(TracingProperties.Propagation properties, TextMapPropagator baggagePropagator) {
		TextMapPropagatorMapper mapper = new TextMapPropagatorMapper(baggagePropagator != null);
		List<TextMapPropagator> injectors = properties.getEffectiveProducedTypes()
			.stream()
			.map(mapper::map)
			.collect(Collectors.toCollection(ArrayList::new));
		if (baggagePropagator != null) {
			injectors.add(baggagePropagator);
		}
		List<TextMapPropagator> extractors = properties.getEffectiveConsumedTypes().stream().map(mapper::map).toList();
		return new CompositeTextMapPropagator(injectors, extractors, baggagePropagator);
	}

	/**
	 * Mapper used to create a {@link TextMapPropagator} from a {@link PropagationType}.
	 */
	private static class TextMapPropagatorMapper {

		private final boolean baggage;

		TextMapPropagatorMapper(boolean baggage) {
			this.baggage = baggage;
		}

		TextMapPropagator map(PropagationType type) {
			return switch (type) {
				case B3 -> b3Single();
				case B3_MULTI -> b3Multi();
				case W3C -> w3c();
			};
		}

		/**
		 * Creates a new B3 propagator using a single B3 header.
		 * @return the B3 propagator
		 */
		private TextMapPropagator b3Single() {
			return B3Propagator.injectingSingleHeader();
		}

		/**
		 * Creates a new B3 propagator using multiple B3 headers.
		 * @return the B3 propagator
		 */
		private TextMapPropagator b3Multi() {
			return B3Propagator.injectingMultiHeaders();
		}

		/**
		 * Creates a new W3C propagator.
		 * @return the W3C propagator
		 */
		private TextMapPropagator w3c() {
			return (!this.baggage) ? W3CTraceContextPropagator.getInstance() : TextMapPropagator
				.composite(W3CTraceContextPropagator.getInstance(), W3CBaggagePropagator.getInstance());
		}

	}

}
