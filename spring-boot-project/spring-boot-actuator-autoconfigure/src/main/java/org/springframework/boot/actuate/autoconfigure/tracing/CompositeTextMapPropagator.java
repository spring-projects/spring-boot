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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

/**
 * {@link TextMapPropagator} which supports multiple tracing formats. It is able to
 * configure different formats for injecting and for extracting.
 *
 * @author Moritz Halbritter
 */
class CompositeTextMapPropagator implements TextMapPropagator {

	private final Collection<TextMapPropagator> injectors;

	private final Collection<TextMapPropagator> mutuallyExclusiveExtractors;

	private final Collection<TextMapPropagator> alwaysRunningExtractors;

	private final Set<String> fields;

	/**
	 * Creates a new {@link CompositeTextMapPropagator}.
	 * @param injectors the injectors
	 * @param mutuallyExclusiveExtractors the mutually exclusive extractors. They are
	 * applied in order, and as soon as an extractor extracts a context, the other
	 * extractors after it are no longer invoked
	 * @param alwaysRunningExtractors the always running extractors. They always run in
	 * order, regardless of the mutually exclusive extractors or whether the extractor
	 * before it has already extracted a context
	 */
	CompositeTextMapPropagator(Collection<TextMapPropagator> injectors,
			Collection<TextMapPropagator> mutuallyExclusiveExtractors,
			Collection<TextMapPropagator> alwaysRunningExtractors) {
		this.injectors = injectors;
		this.mutuallyExclusiveExtractors = mutuallyExclusiveExtractors;
		this.alwaysRunningExtractors = alwaysRunningExtractors;
		this.fields = concat(this.injectors, this.mutuallyExclusiveExtractors, this.alwaysRunningExtractors)
			.flatMap((entry) -> entry.fields().stream())
			.collect(Collectors.toSet());
	}

	Collection<TextMapPropagator> getInjectors() {
		return this.injectors;
	}

	@Override
	public Collection<String> fields() {
		return this.fields;
	}

	@Override
	public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
		if (context == null || setter == null) {
			return;
		}
		for (TextMapPropagator injector : this.injectors) {
			injector.inject(context, carrier, setter);
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
		Context currentContext = context;
		for (TextMapPropagator extractor : this.mutuallyExclusiveExtractors) {
			Context extractedContext = extractor.extract(currentContext, carrier, getter);
			if (extractedContext != currentContext) {
				currentContext = extractedContext;
				break;
			}
		}
		for (TextMapPropagator extractor : this.alwaysRunningExtractors) {
			currentContext = extractor.extract(currentContext, carrier, getter);
		}
		return currentContext;
	}

	/**
	 * Creates a new {@link CompositeTextMapPropagator}, which uses the given
	 * {@code injectionTypes} for injection and all supported types for extraction.
	 * @param injectionTypes the propagation types for injection
	 * @return the {@link CompositeTextMapPropagator}
	 */
	static TextMapPropagator create(Collection<TracingProperties.Propagation.PropagationType> injectionTypes) {
		return create(null, injectionTypes);
	}

	/**
	 * Creates a new {@link CompositeTextMapPropagator}, which uses the given
	 * {@code injectionTypes} for injection and all supported types for extraction.
	 * @param baggagePropagator the baggage propagator to use, or {@code null}
	 * @param injectionTypes the propagation types for injection
	 * @return the {@link CompositeTextMapPropagator}
	 */
	static CompositeTextMapPropagator create(TextMapPropagator baggagePropagator,
			Collection<TracingProperties.Propagation.PropagationType> injectionTypes) {
		List<TextMapPropagator> injectors = injectionTypes.stream()
			.map((injection) -> forType(injection, baggagePropagator != null))
			.collect(Collectors.toCollection(ArrayList::new));
		if (baggagePropagator != null) {
			injectors.add(baggagePropagator);
		}
		List<TextMapPropagator> extractors = Arrays.stream(TracingProperties.Propagation.PropagationType.values())
			.map((extraction) -> forType(extraction, baggagePropagator != null))
			.toList();
		return new CompositeTextMapPropagator(injectors, extractors,
				(baggagePropagator != null) ? List.of(baggagePropagator) : Collections.emptyList());
	}

	@SafeVarargs
	private static <T> Stream<T> concat(Collection<T>... collections) {
		Stream<T> result = Stream.empty();
		for (Collection<T> collection : collections) {
			result = Stream.concat(result, collection.stream());
		}
		return result;
	}

	/**
	 * Creates a new B3 propagator using a single B3 header.
	 * @return the B3 propagator
	 */
	private static TextMapPropagator b3Single() {
		return B3Propagator.injectingSingleHeader();
	}

	/**
	 * Creates a new B3 propagator using multiple B3 headers.
	 * @return the B3 propagator
	 */
	private static TextMapPropagator b3Multi() {
		return B3Propagator.injectingMultiHeaders();
	}

	/**
	 * Creates a new W3C propagator.
	 * @param baggage whether baggage propagation should be supported
	 * @return the W3C propagator
	 */
	private static TextMapPropagator w3c(boolean baggage) {
		if (!baggage) {
			return W3CTraceContextPropagator.getInstance();
		}
		return TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(), W3CBaggagePropagator.getInstance());
	}

	private static TextMapPropagator forType(TracingProperties.Propagation.PropagationType type, boolean baggage) {
		return switch (type) {
			case B3 -> b3Single();
			case B3_MULTI -> b3Multi();
			case W3C -> w3c(baggage);
		};
	}

}
