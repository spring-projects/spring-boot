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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CompositeTextMapPropagator}.
 *
 * @author Moritz Halbritter
 */
class CompositeTextMapPropagatorTests {

	private ContextKeyRegistry contextKeyRegistry;

	@BeforeEach
	void setUp() {
		this.contextKeyRegistry = new ContextKeyRegistry();
	}

	@Test
	void collectsAllFields() {
		CompositeTextMapPropagator propagator = new CompositeTextMapPropagator(List.of(field("a")), List.of(field("b")),
				field("c"));
		assertThat(propagator.fields()).containsExactly("a", "b", "c");
	}

	@Test
	void injectAllFields() {
		CompositeTextMapPropagator propagator = new CompositeTextMapPropagator(List.of(field("a"), field("b")),
				Collections.emptyList(), null);
		TextMapSetter<Object> setter = setter();
		Object carrier = carrier();
		propagator.inject(context(), carrier, setter);
		InOrder inOrder = Mockito.inOrder(setter);
		inOrder.verify(setter).set(carrier, "a", "a-value");
		inOrder.verify(setter).set(carrier, "b", "b-value");
	}

	@Test
	void extractWithoutBaggagePropagator() {
		CompositeTextMapPropagator propagator = new CompositeTextMapPropagator(Collections.emptyList(),
				List.of(field("a"), field("b")), null);
		Context context = context();
		Map<String, String> carrier = Map.of("a", "a-value", "b", "b-value");
		context = propagator.extract(context, carrier, new MapTextMapGetter());
		Object a = context.get(getObjectContextKey("a"));
		assertThat(a).isEqualTo("a-value");
		Object b = context.get(getObjectContextKey("b"));
		assertThat(b).isNull();
	}

	@Test
	void extractWithBaggagePropagator() {
		CompositeTextMapPropagator propagator = new CompositeTextMapPropagator(Collections.emptyList(),
				List.of(field("a"), field("b")), field("c"));
		Context context = context();
		Map<String, String> carrier = Map.of("a", "a-value", "b", "b-value", "c", "c-value");
		context = propagator.extract(context, carrier, new MapTextMapGetter());
		Object c = context.get(getObjectContextKey("c"));
		assertThat(c).isEqualTo("c-value");
	}

	private DummyTextMapPropagator field(String field) {
		return new DummyTextMapPropagator(field, this.contextKeyRegistry);
	}

	private ContextKey<Object> getObjectContextKey(String name) {
		return this.contextKeyRegistry.get(name);
	}

	@SuppressWarnings("unchecked")
	private static <T> TextMapSetter<T> setter() {
		return Mockito.mock(TextMapSetter.class);
	}

	private static Object carrier() {
		return new Object();
	}

	private static Context context() {
		return Context.current();
	}

	private static final class ContextKeyRegistry {

		private final Map<String, ContextKey<Object>> contextKeys = new HashMap<>();

		private ContextKey<Object> get(String name) {
			return this.contextKeys.computeIfAbsent(name, (ignore) -> ContextKey.named(name));
		}

	}

	private static final class MapTextMapGetter implements TextMapGetter<Map<String, String>> {

		@Override
		public Iterable<String> keys(Map<String, String> carrier) {
			return carrier.keySet();
		}

		@Override
		public String get(Map<String, String> carrier, String key) {
			if (carrier == null) {
				return null;
			}
			return carrier.get(key);
		}

	}

	private static final class DummyTextMapPropagator implements TextMapPropagator {

		private final String field;

		private final ContextKeyRegistry contextKeyRegistry;

		private DummyTextMapPropagator(String field, ContextKeyRegistry contextKeyRegistry) {
			this.field = field;
			this.contextKeyRegistry = contextKeyRegistry;
		}

		@Override
		public Collection<String> fields() {
			return List.of(this.field);
		}

		@Override
		public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
			setter.set(carrier, this.field, this.field + "-value");
		}

		@Override
		public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
			String value = getter.get(carrier, this.field);
			if (value != null) {
				return context.with(this.contextKeyRegistry.get(this.field), value);
			}
			return context;
		}

	}

}
