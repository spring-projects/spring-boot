/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link PropertyMapper}.
 *
 * @author Phillip Webb
 * @author Artsiom Yudovin
 */
class PropertyMapperTests {

	private PropertyMapper map = PropertyMapper.get();

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
	void fromValueAlwaysApplyingWhenNonNullShouldAlwaysApplyNonNullToSource() {
		this.map.alwaysApplyingWhenNonNull().from((String) null).toCall(Assertions::fail);
	}

	@Test
	void fromWhenSupplierIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.map.from((Supplier<?>) null))
				.withMessageContaining("Supplier must not be null");
	}

	@Test
	void toWhenConsumerIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.map.from(() -> "").to(null))
				.withMessageContaining("Consumer must not be null");
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
	void asWhenAdapterIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.map.from(() -> "").as(null))
				.withMessageContaining("Adapter must not be null");
	}

	@Test
	void asShouldAdaptSupplier() {
		ExampleDest dest = new ExampleDest();
		this.map.from(() -> 123).as(String::valueOf).to(dest::setName);
		assertThat(dest.getName()).isEqualTo("123");
	}

	@Test
	void whenNonNullWhenSuppliedNullShouldNotMap() {
		this.map.from(() -> null).whenNonNull().as(String::valueOf).toCall(Assertions::fail);
	}

	@Test
	void whenNonNullWhenSuppliedThrowsNullPointerExceptionShouldNotMap() {
		this.map.from(() -> {
			throw new NullPointerException();
		}).whenNonNull().as(String::valueOf).toCall(Assertions::fail);
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
		Long result = this.map.from(source).when("123"::equals).as(Integer::valueOf).when((v) -> v == 123)
				.as(Integer::longValue).toInstance(Long::valueOf);
		assertThat(result).isEqualTo(123);
		assertThat(source.getCount()).isOne();
	}

	@Test
	void alwaysApplyingWhenNonNullShouldAlwaysApplyNonNullToSource() {
		this.map.alwaysApplyingWhenNonNull().from(() -> null).toCall(Assertions::fail);
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

		private String name;

		void setName(String name) {
			this.name = name;
		}

		String getName() {
			return this.name;
		}

	}

}
