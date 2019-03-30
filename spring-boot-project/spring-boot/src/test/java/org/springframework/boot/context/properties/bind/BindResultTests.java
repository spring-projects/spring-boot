/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.context.properties.bind;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link BindResult}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class BindResultTests {

	@Mock
	private Consumer<String> consumer;

	@Mock
	private Function<String, String> mapper;

	@Mock
	private Supplier<String> supplier;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void getWhenHasValueShouldReturnValue() {
		BindResult<String> result = BindResult.of("foo");
		assertThat(result.get()).isEqualTo("foo");
	}

	@Test
	public void getWhenHasNoValueShouldThrowException() {
		BindResult<String> result = BindResult.of(null);
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(result::get)
				.withMessageContaining("No value bound");
	}

	@Test
	public void isBoundWhenHasValueShouldReturnTrue() {
		BindResult<String> result = BindResult.of("foo");
		assertThat(result.isBound()).isTrue();
	}

	@Test
	public void isBoundWhenHasNoValueShouldFalse() {
		BindResult<String> result = BindResult.of(null);
		assertThat(result.isBound()).isFalse();
	}

	@Test
	public void ifBoundWhenConsumerIsNullShouldThrowException() {
		BindResult<String> result = BindResult.of("foo");
		assertThatIllegalArgumentException().isThrownBy(() -> result.ifBound(null))
				.withMessageContaining("Consumer must not be null");
	}

	@Test
	public void ifBoundWhenHasValueShouldCallConsumer() {
		BindResult<String> result = BindResult.of("foo");
		result.ifBound(this.consumer);
		verify(this.consumer).accept("foo");
	}

	@Test
	public void ifBoundWhenHasNoValueShouldNotCallConsumer() {
		BindResult<String> result = BindResult.of(null);
		result.ifBound(this.consumer);
		verifyZeroInteractions(this.consumer);
	}

	@Test
	public void mapWhenMapperIsNullShouldThrowException() {
		BindResult<String> result = BindResult.of("foo");
		assertThatIllegalArgumentException().isThrownBy(() -> result.map(null))
				.withMessageContaining("Mapper must not be null");
	}

	@Test
	public void mapWhenHasValueShouldCallMapper() {
		BindResult<String> result = BindResult.of("foo");
		given(this.mapper.apply("foo")).willReturn("bar");
		assertThat(result.map(this.mapper).get()).isEqualTo("bar");
	}

	@Test
	public void mapWhenHasNoValueShouldNotCallMapper() {
		BindResult<String> result = BindResult.of(null);
		result.map(this.mapper);
		verifyZeroInteractions(this.mapper);
	}

	@Test
	public void orElseWhenHasValueShouldReturnValue() {
		BindResult<String> result = BindResult.of("foo");
		assertThat(result.orElse("bar")).isEqualTo("foo");
	}

	@Test
	public void orElseWhenHasValueNoShouldReturnOther() {
		BindResult<String> result = BindResult.of(null);
		assertThat(result.orElse("bar")).isEqualTo("bar");
	}

	@Test
	public void orElseGetWhenHasValueShouldReturnValue() {
		BindResult<String> result = BindResult.of("foo");
		assertThat(result.orElseGet(this.supplier)).isEqualTo("foo");
		verifyZeroInteractions(this.supplier);
	}

	@Test
	public void orElseGetWhenHasValueNoShouldReturnOther() {
		BindResult<String> result = BindResult.of(null);
		given(this.supplier.get()).willReturn("bar");
		assertThat(result.orElseGet(this.supplier)).isEqualTo("bar");
	}

	@Test
	public void orElseCreateWhenTypeIsNullShouldThrowException() {
		BindResult<String> result = BindResult.of("foo");
		assertThatIllegalArgumentException().isThrownBy(() -> result.orElseCreate(null))
				.withMessageContaining("Type must not be null");
	}

	@Test
	public void orElseCreateWhenHasValueShouldReturnValue() {
		BindResult<ExampleBean> result = BindResult.of(new ExampleBean("foo"));
		assertThat(result.orElseCreate(ExampleBean.class).getValue()).isEqualTo("foo");
	}

	@Test
	public void orElseCreateWhenHasValueNoShouldReturnCreatedValue() {
		BindResult<ExampleBean> result = BindResult.of(null);
		assertThat(result.orElseCreate(ExampleBean.class).getValue()).isEqualTo("new");
	}

	@Test
	public void orElseThrowWhenHasValueShouldReturnValue() throws Exception {
		BindResult<String> result = BindResult.of("foo");
		assertThat(result.orElseThrow(IOException::new)).isEqualTo("foo");
	}

	@Test
	public void orElseThrowWhenHasNoValueShouldThrowException() throws Exception {
		BindResult<String> result = BindResult.of(null);
		assertThatIOException().isThrownBy(() -> result.orElseThrow(IOException::new));
	}

	@Test
	public void hashCodeAndEquals() {
		BindResult<?> result1 = BindResult.of("foo");
		BindResult<?> result2 = BindResult.of("foo");
		BindResult<?> result3 = BindResult.of("bar");
		BindResult<?> result4 = BindResult.of(null);
		assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
		assertThat(result1).isEqualTo(result1).isEqualTo(result2).isNotEqualTo(result3)
				.isNotEqualTo(result4);
	}

	@Test
	public void ofWhenHasValueShouldReturnBoundResultOfValue() {
		BindResult<Object> result = BindResult.of("foo");
		assertThat(result.isBound()).isTrue();
		assertThat(result.get()).isEqualTo("foo");
	}

	@Test
	public void ofWhenValueIsNullShouldReturnUnbound() {
		BindResult<Object> result = BindResult.of(null);
		assertThat(result.isBound()).isFalse();
		assertThat(result).isSameAs(BindResult.of(null));
	}

	static class ExampleBean {

		private final String value;

		ExampleBean() {
			this.value = "new";
		}

		ExampleBean(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}

	}

}
