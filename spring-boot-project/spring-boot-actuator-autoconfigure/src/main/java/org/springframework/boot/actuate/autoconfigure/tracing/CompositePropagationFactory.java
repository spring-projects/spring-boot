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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import brave.internal.propagation.StringPropagationAdapter;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import brave.propagation.Propagation.Factory;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import io.micrometer.tracing.BaggageManager;
import io.micrometer.tracing.brave.bridge.W3CPropagation;

import org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties.Propagation.PropagationType;

/**
 * {@link brave.propagation.Propagation.Factory Propagation factory} which supports
 * multiple tracing formats. It is able to configure different formats for injecting and
 * for extracting.
 *
 * @author Marcin Grzejszczak
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class CompositePropagationFactory extends Propagation.Factory {

	private final PropagationFactories injectors;

	private final PropagationFactories extractors;

	private final CompositePropagation propagation;

	/**
	 * Constructs a new CompositePropagationFactory with the specified injectorFactories
	 * and extractorFactories.
	 * @param injectorFactories the collection of factories used for creating injectors
	 * @param extractorFactories the collection of factories used for creating extractors
	 */
	CompositePropagationFactory(Collection<Factory> injectorFactories, Collection<Factory> extractorFactories) {
		this.injectors = new PropagationFactories(injectorFactories);
		this.extractors = new PropagationFactories(extractorFactories);
		this.propagation = new CompositePropagation(this.injectors, this.extractors);
	}

	/**
	 * Returns a stream of Factory objects representing the injectors in this
	 * CompositePropagationFactory.
	 * @return a stream of Factory objects representing the injectors in this
	 * CompositePropagationFactory
	 */
	Stream<Factory> getInjectors() {
		return this.injectors.stream();
	}

	/**
	 * Returns true if both the injectors and extractors support join operations.
	 * @return true if both the injectors and extractors support join operations, false
	 * otherwise
	 */
	@Override
	public boolean supportsJoin() {
		return this.injectors.supportsJoin() && this.extractors.supportsJoin();
	}

	/**
	 * Returns a boolean value indicating whether a 128-bit trace ID is required.
	 * @return {@code true} if a 128-bit trace ID is required, {@code false} otherwise.
	 */
	@Override
	public boolean requires128BitTraceId() {
		return this.injectors.requires128BitTraceId() || this.extractors.requires128BitTraceId();
	}

	/**
	 * Creates a new instance of Propagation using the specified key factory.
	 * @param keyFactory the key factory used to create keys for the Propagation
	 * @return a new instance of Propagation
	 * @deprecated This method is deprecated and may be removed in future versions. Use
	 * StringPropagationAdapter.create() instead.
	 */
	@Override
	@SuppressWarnings("deprecation")
	public <K> Propagation<K> create(Propagation.KeyFactory<K> keyFactory) {
		return StringPropagationAdapter.create(this.propagation, keyFactory);
	}

	/**
	 * Decorates the given TraceContext with the injectors and extractors provided by this
	 * CompositePropagationFactory.
	 * @param context the TraceContext to be decorated
	 * @return the decorated TraceContext
	 */
	@Override
	public TraceContext decorate(TraceContext context) {
		return Stream.concat(this.injectors.stream(), this.extractors.stream())
			.map((factory) -> factory.decorate(context))
			.filter((decorated) -> decorated != context)
			.findFirst()
			.orElse(context);
	}

	/**
	 * Creates a new {@link CompositePropagationFactory} which doesn't do any propagation.
	 * @return the {@link CompositePropagationFactory}
	 */
	static CompositePropagationFactory noop() {
		return new CompositePropagationFactory(Collections.emptyList(), Collections.emptyList());
	}

	/**
	 * Creates a new {@link CompositePropagationFactory}.
	 * @param properties the propagation properties
	 * @return the {@link CompositePropagationFactory}
	 */
	static CompositePropagationFactory create(TracingProperties.Propagation properties) {
		return create(properties, null, null);
	}

	/**
	 * Creates a new {@link CompositePropagationFactory}.
	 * @param properties the propagation properties
	 * @param baggageManager the baggage manager to use, or {@code null}
	 * @param localFields the local fields, or {@code null}
	 * @return the {@link CompositePropagationFactory}
	 */
	static CompositePropagationFactory create(TracingProperties.Propagation properties, BaggageManager baggageManager,
			LocalBaggageFields localFields) {
		PropagationFactoryMapper mapper = new PropagationFactoryMapper(baggageManager, localFields);
		List<Factory> injectors = properties.getEffectiveProducedTypes().stream().map(mapper::map).toList();
		List<Factory> extractors = properties.getEffectiveConsumedTypes().stream().map(mapper::map).toList();
		return new CompositePropagationFactory(injectors, extractors);
	}

	/**
	 * Mapper used to create a {@link brave.propagation.Propagation.Factory Propagation
	 * factory} from a {@link PropagationType}.
	 */
	private static class PropagationFactoryMapper {

		private final BaggageManager baggageManager;

		private final LocalBaggageFields localFields;

		/**
		 * Constructs a new PropagationFactoryMapper with the specified BaggageManager and
		 * LocalBaggageFields.
		 * @param baggageManager the BaggageManager to be used by the
		 * PropagationFactoryMapper
		 * @param localFields the LocalBaggageFields to be used by the
		 * PropagationFactoryMapper, or null if none
		 */
		PropagationFactoryMapper(BaggageManager baggageManager, LocalBaggageFields localFields) {
			this.baggageManager = baggageManager;
			this.localFields = (localFields != null) ? localFields : LocalBaggageFields.empty();
		}

		/**
		 * Maps the given PropagationType to the corresponding Propagation.Factory
		 * implementation.
		 * @param type the PropagationType to be mapped
		 * @return the Propagation.Factory implementation based on the given
		 * PropagationType
		 * @throws IllegalArgumentException if the given PropagationType is not supported
		 */
		Propagation.Factory map(PropagationType type) {
			return switch (type) {
				case B3 -> b3Single();
				case B3_MULTI -> b3Multi();
				case W3C -> w3c();
			};
		}

		/**
		 * Creates a new B3 propagation factory using a single B3 header.
		 * @return the B3 propagation factory
		 */
		private Propagation.Factory b3Single() {
			return B3Propagation.newFactoryBuilder().injectFormat(B3Propagation.Format.SINGLE).build();
		}

		/**
		 * Creates a new B3 propagation factory using multiple B3 headers.
		 * @return the B3 propagation factory
		 */
		private Propagation.Factory b3Multi() {
			return B3Propagation.newFactoryBuilder().injectFormat(B3Propagation.Format.MULTI).build();
		}

		/**
		 * Creates a new W3C propagation factory.
		 * @return the W3C propagation factory
		 */
		private Propagation.Factory w3c() {
			if (this.baggageManager == null) {
				return new W3CPropagation();
			}
			return new W3CPropagation(this.baggageManager, this.localFields.asList());
		}

	}

	/**
	 * A collection of propagation factories.
	 */
	private static class PropagationFactories {

		private final List<Propagation.Factory> factories;

		/**
		 * Constructs a new instance of PropagationFactories with the specified collection
		 * of factories.
		 * @param factories the collection of factories to be used
		 */
		PropagationFactories(Collection<Factory> factories) {
			this.factories = List.copyOf(factories);
		}

		/**
		 * Returns true if any of the propagation factories in the stream requires a
		 * 128-bit trace ID.
		 * @return true if any of the propagation factories requires a 128-bit trace ID,
		 * false otherwise
		 */
		boolean requires128BitTraceId() {
			return stream().anyMatch(Propagation.Factory::requires128BitTraceId);
		}

		/**
		 * Returns true if all propagation factories in the stream support join, false
		 * otherwise.
		 * @return true if all propagation factories support join, false otherwise
		 */
		boolean supportsJoin() {
			return stream().allMatch(Propagation.Factory::supportsJoin);
		}

		/**
		 * Retrieves a list of Propagation objects using the Factory class.
		 * @return a list of Propagation objects
		 */
		List<Propagation<String>> get() {
			return stream().map(Factory::get).toList();
		}

		/**
		 * Returns a sequential Stream with the elements of the factories collection.
		 * @return a sequential Stream of Factory elements
		 */
		Stream<Factory> stream() {
			return this.factories.stream();
		}

	}

	/**
	 * A composite {@link Propagation}.
	 */
	private static class CompositePropagation implements Propagation<String> {

		private final List<Propagation<String>> injectors;

		private final List<Propagation<String>> extractors;

		private final List<String> keys;

		/**
		 * Constructs a new CompositePropagation object with the given injector and
		 * extractor factories.
		 * @param injectorFactories the factories used to create injectors for propagating
		 * values
		 * @param extractorFactories the factories used to create extractors for
		 * retrieving values
		 */
		CompositePropagation(PropagationFactories injectorFactories, PropagationFactories extractorFactories) {
			this.injectors = injectorFactories.get();
			this.extractors = extractorFactories.get();
			this.keys = Stream.concat(keys(this.injectors), keys(this.extractors)).distinct().toList();
		}

		/**
		 * Returns a stream of keys from the given list of propagations.
		 * @param propagations the list of propagations
		 * @return a stream of keys
		 */
		private Stream<String> keys(List<Propagation<String>> propagations) {
			return propagations.stream().flatMap((propagation) -> propagation.keys().stream());
		}

		/**
		 * Returns a list of keys.
		 * @return the list of keys
		 */
		@Override
		public List<String> keys() {
			return this.keys;
		}

		/**
		 * Returns a {@link TraceContext.Injector} that injects trace context into a
		 * request using the provided setter.
		 * @param setter the setter to use for injecting trace context into the request
		 * @param <R> the type of the request
		 * @return the injector for injecting trace context into the request
		 */
		@Override
		public <R> TraceContext.Injector<R> injector(Setter<R, String> setter) {
			return (traceContext, request) -> this.injectors.stream()
				.map((propagation) -> propagation.injector(setter))
				.forEach((injector) -> injector.inject(traceContext, request));
		}

		/**
		 * Returns a TraceContext.Extractor that extracts trace context from the given
		 * request using the provided getter.
		 * @param getter the getter function to extract trace context from the request
		 * @param <R> the type of the request
		 * @return a TraceContext.Extractor that extracts trace context from the request
		 */
		@Override
		public <R> TraceContext.Extractor<R> extractor(Getter<R, String> getter) {
			return (request) -> this.extractors.stream()
				.map((propagation) -> propagation.extractor(getter))
				.map((extractor) -> extractor.extract(request))
				.filter(Predicate.not(TraceContextOrSamplingFlags.EMPTY::equals))
				.findFirst()
				.orElse(TraceContextOrSamplingFlags.EMPTY);
		}

	}

}
