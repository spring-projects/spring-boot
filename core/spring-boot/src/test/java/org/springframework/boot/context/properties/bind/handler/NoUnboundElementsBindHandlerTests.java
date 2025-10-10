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

package org.springframework.boot.context.properties.bind.handler;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link NoUnboundElementsBindHandler}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class NoUnboundElementsBindHandlerTests {

	private final List<ConfigurationPropertySource> sources = new ArrayList<>();

	private @Nullable Binder binder;

	@Test
	void bindWhenNotUsingNoUnboundElementsHandlerShouldBind() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("example.foo", "bar");
		source.put("example.baz", "bar");
		this.sources.add(source);
		this.binder = new Binder(this.sources);
		Example bound = this.binder.bind(ConfigurationPropertyName.of("example"), Bindable.of(Example.class)).get();
		assertThat(bound.getFoo()).isEqualTo("bar");
	}

	@Test
	void bindWhenUsingNoUnboundElementsHandlerShouldBind() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("example.foo", "bar");
		this.sources.add(source);
		this.binder = new Binder(this.sources);
		Example bound = this.binder.bind("example", Bindable.of(Example.class), new NoUnboundElementsBindHandler())
			.get();
		assertThat(bound.getFoo()).isEqualTo("bar");
	}

	@Test
	void bindWhenUsingNoUnboundElementsHandlerThrowException() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("example.foo", "bar");
		source.put("example.baz", "bar");
		this.sources.add(source);
		this.binder = new Binder(this.sources);
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> {
			assertThat(this.binder).isNotNull();
			this.binder.bind("example", Bindable.of(Example.class), new NoUnboundElementsBindHandler());
		}).satisfies((ex) -> {
			Throwable cause = ex.getCause();
			assertThat(cause).isNotNull();
			assertThat(cause.getMessage()).contains("The elements [example.baz] were left unbound");
		});
	}

	@Test
	void bindWhenUsingNoUnboundElementsHandlerShouldBindIfPrefixDifferent() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("example.foo", "bar");
		source.put("other.baz", "bar");
		this.sources.add(source);
		this.binder = new Binder(this.sources);
		Example bound = this.binder.bind("example", Bindable.of(Example.class), new NoUnboundElementsBindHandler())
			.get();
		assertThat(bound.getFoo()).isEqualTo("bar");
	}

	@Test
	void bindWhenUsingNoUnboundElementsHandlerShouldBindIfUnboundSystemProperties() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("example.foo", "bar");
		source.put("example.other", "baz");
		this.sources.add(source);
		this.binder = new Binder(this.sources);
		NoUnboundElementsBindHandler handler = new NoUnboundElementsBindHandler(BindHandler.DEFAULT,
				((configurationPropertySource) -> false));
		Example bound = this.binder.bind("example", Bindable.of(Example.class), handler).get();
		assertThat(bound.getFoo()).isEqualTo("bar");
	}

	@Test
	void bindWhenUsingNoUnboundElementsHandlerShouldBindIfUnboundCollectionProperties() {
		MockConfigurationPropertySource source1 = new MockConfigurationPropertySource();
		source1.put("example.foo[0]", "bar");
		MockConfigurationPropertySource source2 = new MockConfigurationPropertySource();
		source2.put("example.foo[0]", "bar");
		source2.put("example.foo[1]", "baz");
		this.sources.add(source1);
		this.sources.add(source2);
		this.binder = new Binder(this.sources);
		NoUnboundElementsBindHandler handler = new NoUnboundElementsBindHandler();
		ExampleWithList bound = this.binder.bind("example", Bindable.of(ExampleWithList.class), handler).get();
		assertThat(bound.getFoo()).containsExactly("bar");
	}

	@Test
	void bindWhenUsingNoUnboundElementsHandlerAndUnboundListElementsShouldThrowException() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("example.foo[0]", "bar");
		this.sources.add(source);
		this.binder = new Binder(this.sources);
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> {
			assertThat(this.binder).isNotNull();
			this.binder.bind("example", Bindable.of(Example.class), new NoUnboundElementsBindHandler());
		}).satisfies((ex) -> {
			Throwable cause = ex.getCause();
			assertThat(cause).isNotNull();
			assertThat(cause.getMessage()).contains("The elements [example.foo[0]] were left unbound");
		});
	}

	@Test
	void bindWhenUsingNoUnboundElementsHandlerShouldBindIfUnboundNestedCollectionProperties() {
		MockConfigurationPropertySource source1 = new MockConfigurationPropertySource();
		source1.put("example.nested[0].string-value", "bar");
		MockConfigurationPropertySource source2 = new MockConfigurationPropertySource();
		source2.put("example.nested[0].string-value", "bar");
		source2.put("example.nested[0].int-value", "2");
		source2.put("example.nested[1].string-value", "baz");
		source2.put("example.nested[1].other-nested.baz", "baz");
		this.sources.add(source1);
		this.sources.add(source2);
		this.binder = new Binder(this.sources);
		NoUnboundElementsBindHandler handler = new NoUnboundElementsBindHandler();
		ExampleWithNestedList bound = this.binder.bind("example", Bindable.of(ExampleWithNestedList.class), handler)
			.get();
		List<Nested> nested = bound.getNested();
		assertThat(nested).isNotNull();
		assertThat(nested.get(0).getStringValue()).isEqualTo("bar");
	}

	@Test
	void bindWhenUsingNoUnboundElementsHandlerAndUnboundCollectionElementsWithInvalidPropertyShouldThrowException() {
		MockConfigurationPropertySource source1 = new MockConfigurationPropertySource();
		source1.put("example.nested[0].string-value", "bar");
		MockConfigurationPropertySource source2 = new MockConfigurationPropertySource();
		source2.put("example.nested[0].string-value", "bar");
		source2.put("example.nested[1].int-value", "1");
		source2.put("example.nested[1].invalid", "baz");
		this.sources.add(source1);
		this.sources.add(source2);
		this.binder = new Binder(this.sources);
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> {
			assertThat(this.binder).isNotNull();
			this.binder.bind("example", Bindable.of(ExampleWithNestedList.class), new NoUnboundElementsBindHandler());
		}).satisfies((ex) -> {
			Throwable cause = ex.getCause();
			assertThat(cause).isNotNull();
			assertThat(cause.getMessage()).contains("The elements [example.nested[1].invalid] were left unbound");
		});
	}

	static class Example {

		private @Nullable String foo;

		@Nullable String getFoo() {
			return this.foo;
		}

		void setFoo(@Nullable String foo) {
			this.foo = foo;
		}

	}

	static class ExampleWithList {

		private @Nullable List<String> foo;

		@Nullable List<String> getFoo() {
			return this.foo;
		}

		void setFoo(@Nullable List<String> foo) {
			this.foo = foo;
		}

	}

	static class ExampleWithNestedList {

		private @Nullable List<Nested> nested;

		@Nullable List<Nested> getNested() {
			return this.nested;
		}

		void setNested(@Nullable List<Nested> nested) {
			this.nested = nested;
		}

	}

	static class Nested {

		private @Nullable String stringValue;

		private @Nullable Integer intValue;

		private @Nullable OtherNested otherNested;

		@Nullable String getStringValue() {
			return this.stringValue;
		}

		void setStringValue(@Nullable String value) {
			this.stringValue = value;
		}

		@Nullable Integer getIntValue() {
			return this.intValue;
		}

		void setIntValue(@Nullable Integer intValue) {
			this.intValue = intValue;
		}

		@Nullable OtherNested getOtherNested() {
			return this.otherNested;
		}

		void setOtherNested(@Nullable OtherNested otherNested) {
			this.otherNested = otherNested;
		}

	}

	static class OtherNested {

		private @Nullable String baz;

		@Nullable String getBaz() {
			return this.baz;
		}

		void setBaz(@Nullable String baz) {
			this.baz = baz;
		}

	}

}
