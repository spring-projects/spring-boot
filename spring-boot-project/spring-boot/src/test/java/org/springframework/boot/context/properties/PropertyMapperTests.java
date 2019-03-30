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

import org.junit.Assert;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link PropertyMapper}.
 *
 * @author Phillip Webb
 */
public class PropertyMapperTests {

	private PropertyMapper map = PropertyMapper.get();

	@Test
	public void fromNullValue() {
		ExampleDest dest = new ExampleDest();
		this.map.from((String) null).to(dest::setName);
		assertThat(dest.getName()).isNull();
	}

	@Test
	public void fromValue() {
		ExampleDest dest = new ExampleDest();
		this.map.from("Hello World").to(dest::setName);
		assertThat(dest.getName()).isEqualTo("Hello World");
	}

	@Test
	public void fromValueAsIntShouldAdaptValue() {
		Integer result = this.map.from("123").asInt(Long::valueOf)
				.toInstance(Integer::valueOf);
		assertThat(result).isEqualTo(123);
	}

	@Test
	public void fromValueAlwaysApplyingWhenNonNullShouldAlwaysApplyNonNullToSource() {
		this.map.alwaysApplyingWhenNonNull().from((String) null).toCall(Assert::fail);
	}

	@Test
	public void fromWhenSupplierIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.map.from((Supplier<?>) null))
				.withMessageContaining("Supplier must not be null");
	}

	@Test
	public void toWhenConsumerIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.map.from(() -> "").to(null))
				.withMessageContaining("Consumer must not be null");
	}

	@Test
	public void toShouldMapFromSupplier() {
		ExampleSource source = new ExampleSource("test");
		ExampleDest dest = new ExampleDest();
		this.map.from(source::getName).to(dest::setName);
		assertThat(dest.getName()).isEqualTo("test");
	}

	@Test
	public void asIntShouldAdaptSupplier() {
		Integer result = this.map.from(() -> "123").asInt(Long::valueOf)
				.toInstance(Integer::valueOf);
		assertThat(result).isEqualTo(123);
	}

	@Test
	public void asWhenAdapterIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.map.from(() -> "").as(null))
				.withMessageContaining("Adapter must not be null");
	}

	@Test
	public void asShouldAdaptSupplier() {
		ExampleDest dest = new ExampleDest();
		this.map.from(() -> 123).as(String::valueOf).to(dest::setName);
		assertThat(dest.getName()).isEqualTo("123");
	}

	@Test
	public void whenNonNullWhenSuppliedNullShouldNotMap() {
		this.map.from(() -> null).whenNonNull().as(String::valueOf).toCall(Assert::fail);
	}

	@Test
	public void whenNonNullWhenSuppliedThrowsNullPointerExceptionShouldNotMap() {
		this.map.from(() -> {
			throw new NullPointerException();
		}).whenNonNull().as(String::valueOf).toCall(Assert::fail);
	}

	@Test
	public void whenTrueWhenValueIsTrueShouldMap() {
		Boolean result = this.map.from(true).whenTrue().toInstance(Boolean::valueOf);
		assertThat(result).isTrue();
	}

	@Test
	public void whenTrueWhenValueIsFalseShouldNotMap() {
		this.map.from(false).whenTrue().toCall(Assert::fail);
	}

	@Test
	public void whenFalseWhenValueIsFalseShouldMap() {
		Boolean result = this.map.from(false).whenFalse().toInstance(Boolean::valueOf);
		assertThat(result).isFalse();
	}

	@Test
	public void whenFalseWhenValueIsTrueShouldNotMap() {
		this.map.from(true).whenFalse().toCall(Assert::fail);
	}

	@Test
	public void whenHasTextWhenValueIsNullShouldNotMap() {
		this.map.from(() -> null).whenHasText().toCall(Assert::fail);
	}

	@Test
	public void whenHasTextWhenValueIsEmptyShouldNotMap() {
		this.map.from("").whenHasText().toCall(Assert::fail);
	}

	@Test
	public void whenHasTextWhenValueHasTextShouldMap() {
		Integer result = this.map.from(123).whenHasText().toInstance(Integer::valueOf);
		assertThat(result).isEqualTo(123);
	}

	@Test
	public void whenEqualToWhenValueIsEqualShouldMatch() {
		String result = this.map.from("123").whenEqualTo("123").toInstance(String::new);
		assertThat(result).isEqualTo("123");
	}

	@Test
	public void whenEqualToWhenValueIsNotEqualShouldNotMatch() {
		this.map.from("123").whenEqualTo("321").toCall(Assert::fail);
	}

	@Test
	public void whenInstanceOfWhenValueIsTargetTypeShouldMatch() {
		Long result = this.map.from(123L).whenInstanceOf(Long.class)
				.toInstance((value) -> value + 1);
		assertThat(result).isEqualTo(124L);
	}

	@Test
	public void whenInstanceOfWhenValueIsNotTargetTypeShouldNotMatch() {
		Supplier<Number> supplier = () -> 123L;
		this.map.from(supplier).whenInstanceOf(Double.class).toCall(Assert::fail);
	}

	@Test
	public void whenWhenValueMatchesShouldMap() {
		String result = this.map.from("123").when("123"::equals).toInstance(String::new);
		assertThat(result).isEqualTo("123");
	}

	@Test
	public void whenWhenValueDoesNotMatchShouldNotMap() {
		this.map.from("123").when("321"::equals).toCall(Assert::fail);
	}

	@Test
	public void whenWhenCombinedWithAsUsesSourceValue() {
		Count<String> source = new Count<>(() -> "123");
		Long result = this.map.from(source).when("123"::equals).as(Integer::valueOf)
				.when((v) -> v == 123).as(Integer::longValue).toInstance(Long::valueOf);
		assertThat(result).isEqualTo(123);
		assertThat(source.getCount()).isOne();
	}

	@Test
	public void alwaysApplyingWhenNonNullShouldAlwaysApplyNonNullToSource() {
		this.map.alwaysApplyingWhenNonNull().from(() -> null).toCall(Assert::fail);
	}

	private static class Count<T> implements Supplier<T> {

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

		public int getCount() {
			return this.count;
		}

	}

	private static class ExampleSource {

		private final String name;

		ExampleSource(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

	}

	private static class ExampleDest {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

	}

}
