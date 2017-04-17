/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
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

	@Rule
	public ExpectedException thrown = ExpectedException.none();

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
	public void getWhenHasValueShouldReturnValue() throws Exception {
		BindResult<String> result = BindResult.of("foo");
		assertThat(result.get()).isEqualTo("foo");
	}

	@Test
	public void getWhenHasNoValueShouldThrowException() throws Exception {
		BindResult<String> result = BindResult.of(null);
		this.thrown.expect(NoSuchElementException.class);
		this.thrown.expectMessage("No value bound");
		result.get();
	}

	@Test
	public void isBoundWhenHasValueShouldReturnTrue() throws Exception {
		BindResult<String> result = BindResult.of("foo");
		assertThat(result.isBound()).isTrue();
	}

	@Test
	public void isBoundWhenHasNoValueShouldFalse() throws Exception {
		BindResult<String> result = BindResult.of(null);
		assertThat(result.isBound()).isFalse();
	}

	@Test
	public void ifBoundWhenConsumerIsNullShouldThrowException() throws Exception {
		BindResult<String> result = BindResult.of("foo");
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Consumer must not be null");
		result.ifBound(null);
	}

	@Test
	public void ifBoundWhenHasValueShouldCallConsumer() throws Exception {
		BindResult<String> result = BindResult.of("foo");
		result.ifBound(this.consumer);
		verify(this.consumer).accept("foo");
	}

	@Test
	public void ifBoundWhenHasNoValueShouldNotCallConsumer() throws Exception {
		BindResult<String> result = BindResult.of(null);
		result.ifBound(this.consumer);
		verifyZeroInteractions(this.consumer);
	}

	@Test
	public void mapWhenMapperIsNullShouldThrowException() throws Exception {
		BindResult<String> result = BindResult.of("foo");
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Mapper must not be null");
		result.map(null);
	}

	@Test
	public void mapWhenHasValueShouldCallMapper() throws Exception {
		BindResult<String> result = BindResult.of("foo");
		given(this.mapper.apply("foo")).willReturn("bar");
		assertThat(result.map(this.mapper).get()).isEqualTo("bar");
	}

	@Test
	public void mapWhenHasNoValueShouldNotCallMapper() throws Exception {
		BindResult<String> result = BindResult.of(null);
		result.map(this.mapper);
		verifyZeroInteractions(this.mapper);
	}

	@Test
	public void orElseWhenHasValueShouldReturnValue() throws Exception {
		BindResult<String> result = BindResult.of("foo");
		assertThat(result.orElse("bar")).isEqualTo("foo");
	}

	@Test
	public void orElseWhenHasValueNoShouldReturnOther() throws Exception {
		BindResult<String> result = BindResult.of(null);
		assertThat(result.orElse("bar")).isEqualTo("bar");
	}

	@Test
	public void orElseGetWhenHasValueShouldReturnValue() throws Exception {
		BindResult<String> result = BindResult.of("foo");
		assertThat(result.orElseGet(this.supplier)).isEqualTo("foo");
		verifyZeroInteractions(this.supplier);
	}

	@Test
	public void orElseGetWhenHasValueNoShouldReturnOther() throws Exception {
		BindResult<String> result = BindResult.of(null);
		given(this.supplier.get()).willReturn("bar");
		assertThat(result.orElseGet(this.supplier)).isEqualTo("bar");
	}

	@Test
	public void orElseCreateWhenTypeIsNullShouldThrowException() throws Exception {
		BindResult<String> result = BindResult.of("foo");
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Type must not be null");
		result.orElseCreate(null);
	}

	@Test
	public void orElseCreateWhenHasValueShouldReturnValue() throws Exception {
		BindResult<ExampleBean> result = BindResult.of(new ExampleBean("foo"));
		assertThat(result.orElseCreate(ExampleBean.class).getValue()).isEqualTo("foo");
	}

	@Test
	public void orElseCreateWhenHasValueNoShouldReturnCreatedValue() throws Exception {
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
		this.thrown.expect(IOException.class);
		result.orElseThrow(IOException::new);
	}

	@Test
	public void hashCodeAndEquals() throws Exception {
		BindResult<?> result1 = BindResult.of("foo");
		BindResult<?> result2 = BindResult.of("foo");
		BindResult<?> result3 = BindResult.of("bar");
		BindResult<?> result4 = BindResult.of(null);
		assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
		assertThat(result1).isEqualTo(result1).isEqualTo(result2).isNotEqualTo(result3)
				.isNotEqualTo(result4);
	}

	@Test
	public void ofWhenHasValueShouldReturnBoundResultOfValue() throws Exception {
		BindResult<Object> result = BindResult.of("foo");
		assertThat(result.isBound()).isTrue();
		assertThat(result.get()).isEqualTo("foo");
	}

	@Test
	public void ofWhenValueIsNullShouldReturnUnbound() throws Exception {
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
