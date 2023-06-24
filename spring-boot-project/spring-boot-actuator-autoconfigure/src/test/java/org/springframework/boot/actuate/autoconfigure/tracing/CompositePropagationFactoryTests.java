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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import brave.Span;
import brave.Tracing;
import brave.internal.propagation.StringPropagationAdapter;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link CompositePropagationFactory}.
 *
 * @author Moritz Halbritter
 */
class CompositePropagationFactoryTests {

	@Test
	void supportsJoin() {
		Propagation.Factory supported = Mockito.mock(Propagation.Factory.class);
		given(supported.supportsJoin()).willReturn(true);
		given(supported.get()).willReturn(new DummyPropagation("a"));
		Propagation.Factory unsupported = Mockito.mock(Propagation.Factory.class);
		given(unsupported.supportsJoin()).willReturn(false);
		given(unsupported.get()).willReturn(new DummyPropagation("a"));
		CompositePropagationFactory factory = new CompositePropagationFactory(List.of(supported), List.of(unsupported));
		assertThat(factory.supportsJoin()).isFalse();
	}

	@Test
	void requires128BitTraceId() {
		Propagation.Factory required = Mockito.mock(Propagation.Factory.class);
		given(required.requires128BitTraceId()).willReturn(true);
		given(required.get()).willReturn(new DummyPropagation("a"));
		Propagation.Factory notRequired = Mockito.mock(Propagation.Factory.class);
		given(notRequired.requires128BitTraceId()).willReturn(false);
		given(notRequired.get()).willReturn(new DummyPropagation("a"));
		CompositePropagationFactory factory = new CompositePropagationFactory(List.of(required), List.of(notRequired));
		assertThat(factory.requires128BitTraceId()).isTrue();
	}

	@Nested
	static class CompostePropagationTests {

		private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(CompositePropagationFactory.class));

		@Test
		void keys() {
			CompositePropagationFactory factory = new CompositePropagationFactory(List.of(field("a")),
					List.of(field("b")));
			Propagation<String> propagation = factory.get();
			assertThat(propagation.keys()).containsExactly("a", "b");
		}

		@Test
		void inject() {
			CompositePropagationFactory factory = new CompositePropagationFactory(List.of(field("a"), field("b")),
					List.of(field("c")));
			Propagation<String> propagation = factory.get();
			TraceContext context = context();
			Map<String, String> request = new HashMap<>();
			propagation.injector(new MapSetter()).inject(context, request);
			assertThat(request).containsOnly(entry("a", "a-value"), entry("b", "b-value"));
		}

		@Test
		void thisIsATest() {
			this.contextRunner
					.withPropertyValues("management.tracing.propagation.type=B3",
							"management.tracing.brave.span-joining-supported=true")
					.run((context) -> {
						CompositePropagationFactory factory = new CompositePropagationFactory(List.of(field("a"), field("b")),
								List.of(field("c")));
						Propagation<String> propagation = factory.get();
						Tracing tracing = context.getBean(Tracing.class);
						Span parentSpan = tracing.tracer().nextSpan();
						Span childSpan = tracing.tracer().joinSpan(parentSpan.context());
						Map<String, String> request = new HashMap<>();
						propagation.injector(new CompositePropagationFactoryTests.MapSetter()).inject(parentSpan.context(), request);
					});
		}

		@Test
		void extractorWhenDelegateExtractsReturnsExtraction() {
			CompositePropagationFactory factory = new CompositePropagationFactory(Collections.emptyList(),
					List.of(field("a"), field("b")));
			Propagation<String> propagation = factory.get();
			Map<String, String> request = Map.of("a", "a-value", "b", "b-value");
			TraceContextOrSamplingFlags context = propagation.extractor(new MapGetter()).extract(request);
			assertThat(context.context().extra()).containsExactly("a");
		}

		@Test
		void extractorWhenWhenNoExtractorMatchesReturnsEmptyContext() {
			CompositePropagationFactory factory = new CompositePropagationFactory(Collections.emptyList(),
					Collections.emptyList());
			Propagation<String> propagation = factory.get();
			Map<String, String> request = Collections.emptyMap();
			TraceContextOrSamplingFlags context = propagation.extractor(new MapGetter()).extract(request);
			assertThat(context.context()).isNull();
		}

		private static TraceContext context() {
			return TraceContext.newBuilder().traceId(1).spanId(2).build();
		}

		private static DummyPropagation field(String field) {
			return new DummyPropagation(field);
		}

	}

	private static final class MapSetter implements Propagation.Setter<Map<String, String>, String> {

		@Override
		public void put(Map<String, String> request, String key, String value) {
			request.put(key, value);
		}

	}

	private static final class MapGetter implements Propagation.Getter<Map<String, String>, String> {

		@Override
		public String get(Map<String, String> request, String key) {
			return request.get(key);
		}

	}

	private static final class DummyPropagation extends Propagation.Factory implements Propagation<String> {

		private final String field;

		private DummyPropagation(String field) {
			this.field = field;
		}

		@Override
		@SuppressWarnings("deprecation")
		public <K> Propagation<K> create(Propagation.KeyFactory<K> keyFactory) {
			return StringPropagationAdapter.create(this, keyFactory);
		}

		@Override
		public List<String> keys() {
			return List.of(this.field);
		}

		@Override
		public <R> TraceContext.Injector<R> injector(Propagation.Setter<R, String> setter) {
			return (traceContext, request) -> setter.put(request, this.field, this.field + "-value");
		}

		@Override
		public <R> TraceContext.Extractor<R> extractor(Propagation.Getter<R, String> getter) {
			return (request) -> {
				TraceContext context = TraceContext.newBuilder().traceId(1).spanId(2).addExtra(this.field).build();
				return TraceContextOrSamplingFlags.create(context);
			};
		}

	}

}
