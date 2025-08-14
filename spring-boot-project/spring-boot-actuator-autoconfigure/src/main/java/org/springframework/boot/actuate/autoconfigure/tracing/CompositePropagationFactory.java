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

package org.springframework.boot.actuate.autoconfigure.tracing;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

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
	public Propagation<String> get() {
		return this.propagation;
	}

	@Override
	public TraceContext decorate(TraceContext context) {
		for (Propagation.Factory factory : this.injectors.factories) {
			TraceContext decorated = factory.decorate(context);
			if (decorated != context) {
				return decorated;
			}
		}
		for (Propagation.Factory factory : this.extractors.factories) {
			TraceContext decorated = factory.decorate(context);
			if (decorated != context) {
				return decorated;
			}
		}
		return context;
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

		PropagationFactoryMapper(BaggageManager baggageManager, LocalBaggageFields localFields) {
			this.baggageManager = baggageManager;
			this.localFields = (localFields != null) ? localFields : LocalBaggageFields.empty();
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

		PropagationFactories(Collection<Factory> factories) {
			this.factories = List.copyOf(factories);
		}

		boolean requires128BitTraceId() {
			for (Propagation.Factory factory : this.factories) {
				if (factory.requires128BitTraceId()) {
					return true;
				}
			}
			return false;
		}

		boolean supportsJoin() {
			for (Propagation.Factory factory : this.factories) {
				if (!factory.supportsJoin()) {
					return false;
				}
			}
			return true;
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
			return (traceContext, request) -> {
				for (Propagation<String> propagation : this.injectors) {
					propagation.injector(setter).inject(traceContext, request);
				}
			};
		}

		@Override
		public <R> TraceContext.Extractor<R> extractor(Getter<R, String> getter) {
			return (request) -> {
				for (Propagation<String> propagation : this.extractors) {
					TraceContextOrSamplingFlags extracted = propagation.extractor(getter).extract(request);
					if (!TraceContextOrSamplingFlags.EMPTY.equals(extracted)) {
						return extracted;
					}
				}
				return TraceContextOrSamplingFlags.EMPTY;
			};
		}

	}

}
