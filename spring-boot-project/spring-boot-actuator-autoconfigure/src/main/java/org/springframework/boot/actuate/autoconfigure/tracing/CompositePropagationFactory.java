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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import brave.internal.propagation.StringPropagationAdapter;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import io.micrometer.tracing.BaggageManager;
import io.micrometer.tracing.brave.bridge.W3CPropagation;

/**
 * {@link Factory} which supports multiple tracing formats. It is able to configure
 * different formats for injecting and for extracting.
 *
 * @author Marcin Grzejszczak
 * @author Moritz Halbritter
 */
class CompositePropagationFactory extends Propagation.Factory implements Propagation<String> {

	private final Collection<Propagation.Factory> injectorFactories;

	private final Collection<Propagation.Factory> extractorFactories;

	private final List<Propagation<String>> injectors;

	private final List<Propagation<String>> extractors;

	private final boolean supportsJoin;

	private final boolean requires128BitTraceId;

	private final List<String> keys;

	CompositePropagationFactory(Collection<Factory> injectorFactories, Collection<Factory> extractorFactories) {
		this.injectorFactories = injectorFactories;
		this.extractorFactories = extractorFactories;
		this.injectors = this.injectorFactories.stream().map(Factory::get).toList();
		this.extractors = this.extractorFactories.stream().map(Factory::get).toList();
		this.supportsJoin = Stream.concat(this.injectorFactories.stream(), this.extractorFactories.stream())
			.allMatch(Factory::supportsJoin);
		this.requires128BitTraceId = Stream.concat(this.injectorFactories.stream(), this.extractorFactories.stream())
			.anyMatch(Factory::requires128BitTraceId);
		this.keys = Stream.concat(this.injectors.stream(), this.extractors.stream())
			.flatMap((entry) -> entry.keys().stream())
			.distinct()
			.toList();
	}

	Collection<Factory> getInjectorFactories() {
		return this.injectorFactories;
	}

	@Override
	public List<String> keys() {
		return this.keys;
	}

	@Override
	public <R> TraceContext.Injector<R> injector(Setter<R, String> setter) {
		return (traceContext, request) -> {
			for (Propagation<String> injector : this.injectors) {
				injector.injector(setter).inject(traceContext, request);
			}
		};
	}

	@Override
	public <R> TraceContext.Extractor<R> extractor(Getter<R, String> getter) {
		return (request) -> {
			for (Propagation<String> extractor : this.extractors) {
				TraceContextOrSamplingFlags extract = extractor.extractor(getter).extract(request);
				if (extract != TraceContextOrSamplingFlags.EMPTY) {
					return extract;
				}
			}
			return TraceContextOrSamplingFlags.EMPTY;
		};
	}

	@Override
	@SuppressWarnings("deprecation")
	public <K> Propagation<K> create(KeyFactory<K> keyFactory) {
		return StringPropagationAdapter.create(this, keyFactory);
	}

	@Override
	public boolean supportsJoin() {
		return this.supportsJoin;
	}

	@Override
	public boolean requires128BitTraceId() {
		return this.requires128BitTraceId;
	}

	@Override
	public TraceContext decorate(TraceContext context) {
		for (Factory injectorFactory : this.injectorFactories) {
			TraceContext decorated = injectorFactory.decorate(context);
			if (decorated != context) {
				return decorated;
			}
		}
		for (Factory extractorFactory : this.extractorFactories) {
			TraceContext decorated = extractorFactory.decorate(context);
			if (decorated != context) {
				return decorated;
			}
		}
		return super.decorate(context);
	}

	/**
	 * Creates a new {@link CompositePropagationFactory}, which uses the given
	 * {@code injectionTypes} for injection and all supported types for extraction.
	 * @param baggageManager the baggage manager to use, or {@code null}
	 * @param injectionTypes the propagation types for injection
	 * @return the {@link CompositePropagationFactory}
	 */
	static CompositePropagationFactory create(BaggageManager baggageManager,
			Collection<TracingProperties.Propagation.PropagationType> injectionTypes) {
		List<Factory> injectors = injectionTypes.stream()
			.map((injection) -> factoryForType(baggageManager, injection))
			.toList();
		List<Factory> extractors = Arrays.stream(TracingProperties.Propagation.PropagationType.values())
			.map((extraction) -> factoryForType(baggageManager, extraction))
			.toList();
		return new CompositePropagationFactory(injectors, extractors);
	}

	/**
	 * Creates a new {@link CompositePropagationFactory}, which uses the given
	 * {@code injectionTypes} for injection and all supported types for extraction.
	 * @param injectionTypes the propagation types for injection
	 * @return the {@link CompositePropagationFactory}
	 */
	static CompositePropagationFactory create(
			Collection<TracingProperties.Propagation.PropagationType> injectionTypes) {
		return create(null, injectionTypes);
	}

	private static Factory factoryForType(BaggageManager baggageManager,
			TracingProperties.Propagation.PropagationType type) {
		return switch (type) {
			case B3 -> b3Single();
			case B3_MULTI -> b3Multi();
			case W3C -> w3c(baggageManager);
		};
	}

	/**
	 * Creates a new B3 propagation factory using a single B3 header.
	 * @return the B3 propagation factory
	 */
	private static Factory b3Single() {
		return B3Propagation.newFactoryBuilder().injectFormat(B3Propagation.Format.SINGLE_NO_PARENT).build();
	}

	/**
	 * Creates a new B3 propagation factory using multiple B3 headers.
	 * @return the B3 propagation factory
	 */
	private static Factory b3Multi() {
		return B3Propagation.newFactoryBuilder().injectFormat(B3Propagation.Format.MULTI).build();
	}

	/**
	 * Creates a new W3C propagation factory.
	 * @param baggageManager baggage manager to use, or {@code null}
	 * @return the W3C propagation factory
	 */
	private static W3CPropagation w3c(BaggageManager baggageManager) {
		return (baggageManager != null) ? new W3CPropagation(baggageManager, Collections.emptyList())
				: new W3CPropagation();
	}

}
