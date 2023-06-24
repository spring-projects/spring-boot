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

	CompositePropagationFactory(Collection<Factory> injectorFactories, Collection<Factory> extractorFactories) {
		this.injectors = new PropagationFactories(injectorFactories);
		this.extractors = new PropagationFactories(extractorFactories);
		this.propagation = new CompositePropagation(this.injectors, this.extractors);
	}

	Stream<Factory> getInjectors() {
		return this.injectors.stream();
	}

	@Override
	public boolean supportsJoin() {
		return this.injectors.supportsJoin() && this.extractors.supportsJoin();
	}

	@Override
	public boolean requires128BitTraceId() {
		return this.injectors.requires128BitTraceId() || this.extractors.requires128BitTraceId();
	}

	@Override
	@SuppressWarnings("deprecation")
	public <K> Propagation<K> create(Propagation.KeyFactory<K> keyFactory) {
		return StringPropagationAdapter.create(this.propagation, keyFactory);
	}

	@Override
	public TraceContext decorate(TraceContext context) {
		return Stream.concat(this.injectors.stream(), this.extractors.stream())
			.map((factory) -> factory.decorate(context))
			.filter((decorated) -> decorated != context)
			.findFirst()
			.orElse(context);
	}

	/**
	 * Creates a new {@link CompositePropagationFactory}, which uses the given
	 * {@code injectionTypes} for injection and {@code extractionTypes} for extraction.
	 * @param properties the propagation properties
	 * @param baggageManager the baggage manager to use, or {@code null}
	 * @return the {@link CompositePropagationFactory}
	 */
	static CompositePropagationFactory create(TracingProperties.Propagation properties, BaggageManager baggageManager) {
		PropagationFactoryMapper mapper = new PropagationFactoryMapper(baggageManager);
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

		PropagationFactoryMapper(BaggageManager baggageManager) {
			this.baggageManager = baggageManager;
		}

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
			return (this.baggageManager != null) ? new W3CPropagation(this.baggageManager, Collections.emptyList())
					: new W3CPropagation();
		}

	}

	/**
	 * A collection of propagation factories.
	 */
	private static class PropagationFactories {

		private final List<Propagation.Factory> factories;

		PropagationFactories(Collection<Factory> factories) {
			this.factories = List.copyOf(factories);
		}

		boolean requires128BitTraceId() {
			return stream().anyMatch(Propagation.Factory::requires128BitTraceId);
		}

		boolean supportsJoin() {
			return stream().allMatch(Propagation.Factory::supportsJoin);
		}

		List<Propagation<String>> get() {
			return stream().map(Factory::get).toList();
		}

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

		CompositePropagation(PropagationFactories injectorFactories, PropagationFactories extractorFactories) {
			this.injectors = injectorFactories.get();
			this.extractors = extractorFactories.get();
			this.keys = Stream.concat(keys(this.injectors), keys(this.extractors)).distinct().toList();
		}

		private Stream<String> keys(List<Propagation<String>> propagations) {
			return propagations.stream().flatMap((propagation) -> propagation.keys().stream());
		}

		@Override
		public List<String> keys() {
			return this.keys;
		}

		@Override
		public <R> TraceContext.Injector<R> injector(Setter<R, String> setter) {
			return (traceContext, request) -> this.injectors.stream()
				.map((propagation) -> propagation.injector(setter))
				.forEach((injector) -> injector.inject(traceContext, request));
		}

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
