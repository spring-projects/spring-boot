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

package org.springframework.boot.context.properties;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import org.assertj.core.api.Assertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.PropertyMapper.Source.Always;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link PropertyMapper}.
 *
 * @author Phillip Webb
 * @author Artsiom Yudovin
 * @author Chris Bono
 */
class PropertyMapperTests {

	private final PropertyMapper map = PropertyMapper.get();

	@Test
	void fromNullValue() {
		ExampleDest dest = new ExampleDest();
		this.map.from((String) null).to(dest::setName);
		assertThat(dest.getName()).isNull();
	}

	@Test
	void fromValue() {
		ExampleDest dest = new ExampleDest();
		this.map.from("Hello World").to(dest::setName);
		assertThat(dest.getName()).isEqualTo("Hello World");
	}

	@Test
	void fromValueAsIntShouldAdaptValue() {
		Integer result = this.map.from("123").asInt(Long::valueOf).toInstance(Integer::valueOf);
		assertThat(result).isEqualTo(123);
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void fromWhenSupplierIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.map.from((Supplier<?>) null))
			.withMessageContaining("'supplier' must not be null");
	}

	@Test
	void orFromWhenSuppliedWithNull() {
		assertThat(this.map.from("value").orFrom(() -> "fallback").toInstance(Function.identity())).isEqualTo("value");
	}

	@Test
	void orFromWhenSuppliedWithNonNull() {
		assertThat(this.map.from((String) null).orFrom(() -> "fallback").toInstance(Function.identity()))
			.isEqualTo("fallback");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void toWhenConsumerIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.map.from(() -> "").to(null))
			.withMessageContaining("'consumer' must not be null");
	}

	@Test
	void toShouldMapFromSupplier() {
		ExampleSource source = new ExampleSource("test");
		ExampleDest dest = new ExampleDest();
		this.map.from(source::getName).to(dest::setName);
		assertThat(dest.getName()).isEqualTo("test");
	}

	@Test
	void asIntShouldAdaptSupplier() {
		Integer result = this.map.from(() -> "123").asInt(Long::valueOf).toInstance(Integer::valueOf);
		assertThat(result).isEqualTo(123);
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void asWhenAdapterIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.map.from(() -> "").as(null))
			.withMessageContaining("'adapter' must not be null");
	}

	@Test
	void asShouldAdaptSupplier() {
		ExampleDest dest = new ExampleDest();
		this.map.from(() -> 123).as(String::valueOf).to(dest::setName);
		assertThat(dest.getName()).isEqualTo("123");
	}

	@Test
	void whenTrueWhenValueIsTrueShouldMap() {
		Boolean result = this.map.from(true).whenTrue().toInstance(Boolean::valueOf);
		assertThat(result).isTrue();
	}

	@Test
	void whenTrueWhenValueIsFalseShouldNotMap() {
		this.map.from(false).whenTrue().toCall(Assertions::fail);
	}

	@Test
	void whenFalseWhenValueIsFalseShouldMap() {
		Boolean result = this.map.from(false).whenFalse().toInstance(Boolean::valueOf);
		assertThat(result).isFalse();
	}

	@Test
	void whenFalseWhenValueIsTrueShouldNotMap() {
		this.map.from(true).whenFalse().toCall(Assertions::fail);
	}

	@Test
	void whenHasTextWhenValueIsNullShouldNotMap() {
		this.map.from(() -> null).whenHasText().toCall(Assertions::fail);
	}

	@Test
	void whenHasTextWhenValueIsEmptyShouldNotMap() {
		this.map.from("").whenHasText().toCall(Assertions::fail);
	}

	@Test
	void whenHasTextWhenValueHasTextShouldMap() {
		Integer result = this.map.from(123).whenHasText().toInstance(Integer::valueOf);
		assertThat(result).isEqualTo(123);
	}

	@Test
	void whenEqualToWhenValueIsEqualShouldMatch() {
		String result = this.map.from("123").whenEqualTo("123").toInstance(String::new);
		assertThat(result).isEqualTo("123");
	}

	@Test
	void whenEqualToWhenValueIsNotEqualShouldNotMatch() {
		this.map.from("123").whenEqualTo("321").toCall(Assertions::fail);
	}

	@Test
	void whenInstanceOfWhenValueIsTargetTypeShouldMatch() {
		Long result = this.map.from(123L).whenInstanceOf(Long.class).toInstance((value) -> value + 1);
		assertThat(result).isEqualTo(124L);
	}

	@Test
	void whenInstanceOfWhenValueIsNotTargetTypeShouldNotMatch() {
		Supplier<Number> supplier = () -> 123L;
		this.map.from(supplier).whenInstanceOf(Double.class).toCall(Assertions::fail);
	}

	@Test
	void whenWhenValueMatchesShouldMap() {
		String result = this.map.from("123").when("123"::equals).toInstance(String::new);
		assertThat(result).isEqualTo("123");
	}

	@Test
	void whenWhenValueDoesNotMatchShouldNotMap() {
		this.map.from("123").when("321"::equals).toCall(Assertions::fail);
	}

	@Test
	void whenWhenCombinedWithAsUsesSourceValue() {
		Count<String> source = new Count<>(() -> "123");
		Long result = this.map.from(source)
			.when("123"::equals)
			.as(Integer::valueOf)
			.when((v) -> v == 123)
			.as(Integer::longValue)
			.toInstance(Long::valueOf);
		assertThat(result).isEqualTo(123);
		assertThat(source.getCount()).isOne();
	}

	@Test
	void whenWhenValueNotMatchesShouldSupportChainedCalls() {
		this.map.from("123").when("456"::equals).when("123"::equals).toCall(Assertions::fail);
	}

	@Test
	void whenWhenValueMatchesShouldSupportChainedCalls() {
		String result = this.map.from("123").when((s) -> s.contains("2")).when("123"::equals).toInstance(String::new);
		assertThat(result).isEqualTo("123");
	}

	@Test
	void toImmutableReturnsNewInstance() {
		Immutable instance = this.map.from("Spring").toInstance(Immutable::of);
		instance = this.map.from("123").as(Integer::valueOf).to(instance, Immutable::withAge);
		assertThat(instance).hasToString("Spring 123");
	}

	@Test
	void toImmutableWhenFilteredReturnsOriginalInstance() {
		Immutable instance = this.map.from("Spring").toInstance(Immutable::of);
		instance = this.map.from("123").when("345"::equals).as(Integer::valueOf).to(instance, Immutable::withAge);
		assertThat(instance).hasToString("Spring null");
	}

	@Test
	void toConsumerWhenNull() {
		ExampleDest dest = new ExampleDest();
		this.map.from((String) null).to(dest::setName);
		assertThat(dest.getName()).isNull();
		assertThat(dest.setNameCalled).isFalse();
	}

	@Test
	void toImmutableWhenNull() {
		Immutable instance = this.map.from("Spring").toInstance(Immutable::of);
		instance = this.map.from((Integer) null).to(instance, Immutable::withAge);
		assertThat(instance).hasToString("Spring null");
		assertThat(instance.withAgeCalled).isFalse();
	}

	@Test
	void toInstanceWhenNull() {
		assertThatExceptionOfType(NoSuchElementException.class)
			.isThrownBy(() -> this.map.from((String) null).toInstance(String::valueOf));
	}

	@Test
	void toCallWhenNull() {
		AtomicBoolean called = new AtomicBoolean();
		Runnable call = () -> called.set(true);
		this.map.from((String) null).toCall(call);
		assertThat(called).isFalse();
	}

	/**
	 * Tests for {@link Always}.
	 */
	@Nested
	class AlwaysTests {

		private final PropertyMapper map = PropertyMapperTests.this.map;

		@Test
		void asWhenNull() {
			String value = this.map.from((String) null).always().as(String::valueOf).toInstance((string) -> {
				assertThat(string).isNotNull();
				return string;
			});
			assertThat(value).isEqualTo("null");
		}

		@Test
		void toConsumerWhenNull() {
			ExampleDest dest = new ExampleDest();
			this.map.from((String) null).always().to(dest::setName);
			assertThat(dest.getName()).isNull();
			assertThat(dest.setNameCalled).isTrue();
		}

		@Test
		void toImmutableWhenNull() {
			Immutable instance = this.map.from("Spring").toInstance(Immutable::of);
			instance = this.map.from((Integer) null).always().to(instance, Immutable::withAge);
			assertThat(instance).hasToString("Spring null");
			assertThat(instance.withAgeCalled).isTrue();
		}

		@Test
		void toInstanceWhenNull() {
			String value = this.map.from((String) null).always().toInstance(String::valueOf);
			assertThat(value).isEqualTo("null");
		}

		@Test
		void toCallWhenNull() {
			AtomicBoolean called = new AtomicBoolean();
			Runnable call = () -> called.set(true);
			this.map.from((String) null).always().toCall(call);
			assertThat(called).isTrue();
		}

	}

	static class Count<T> implements Supplier<T> {

		private final Supplier<T> source;

		private int count;

		Count(Supplier<T> source) {
			this.source = source;
		}

		@Override
		public T get() {
			this.count++;
			return this.source.get();
		}

		int getCount() {
			return this.count;
		}

	}

	static class ExampleSource {

		private final String name;

		ExampleSource(String name) {
			this.name = name;
		}

		String getName() {
			return this.name;
		}

	}

	static class ExampleDest {

		private @Nullable String name;

		boolean setNameCalled;

		void setName(@Nullable String name) {
			this.name = name;
			this.setNameCalled = true;
		}

		@Nullable String getName() {
			return this.name;
		}

	}

	static class Immutable {

		private final String name;

		private final @Nullable Integer age;

		final boolean withAgeCalled;

		Immutable(String name, @Nullable Integer age) {
			this(name, age, false);
		}

		private Immutable(String name, @Nullable Integer age, boolean withAgeCalled) {
			this.name = name;
			this.age = age;
			this.withAgeCalled = withAgeCalled;
		}

		Immutable withAge(@Nullable Integer age) {
			return new Immutable(this.name, age, true);
		}

		@Override
		public String toString() {
			return "%s %s".formatted(this.name, this.age);
		}

		static Immutable of(String name) {
			return new Immutable(name, null);
		}

	}

}
