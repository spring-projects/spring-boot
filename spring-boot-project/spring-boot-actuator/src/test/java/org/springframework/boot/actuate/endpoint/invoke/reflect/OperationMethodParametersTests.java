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

package org.springframework.boot.actuate.endpoint.invoke.reflect;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.invoke.OperationParameter;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OperationMethodParameters}.
 *
 * @author Phillip Webb
 */
public class OperationMethodParametersTests {

	private Method exampleMethod = ReflectionUtils.findMethod(getClass(), "example",
			String.class);

	private Method exampleNoParamsMethod = ReflectionUtils.findMethod(getClass(),
			"exampleNoParams");

	@Test
	public void createWhenMethodIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new OperationMethodParameters(null,
						mock(ParameterNameDiscoverer.class)))
				.withMessageContaining("Method must not be null");
	}

	@Test
	public void createWhenParameterNameDiscovererIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new OperationMethodParameters(this.exampleMethod, null))
				.withMessageContaining("ParameterNameDiscoverer must not be null");
	}

	@Test
	public void createWhenParameterNameDiscovererReturnsNullShouldThrowException() {
		assertThatIllegalStateException()
				.isThrownBy(() -> new OperationMethodParameters(this.exampleMethod,
						mock(ParameterNameDiscoverer.class)))
				.withMessageContaining("Failed to extract parameter names");
	}

	@Test
	public void hasParametersWhenHasParametersShouldReturnTrue() {
		OperationMethodParameters parameters = new OperationMethodParameters(
				this.exampleMethod, new DefaultParameterNameDiscoverer());
		assertThat(parameters.hasParameters()).isTrue();
	}

	@Test
	public void hasParametersWhenHasNoParametersShouldReturnFalse() {
		OperationMethodParameters parameters = new OperationMethodParameters(
				this.exampleNoParamsMethod, new DefaultParameterNameDiscoverer());
		assertThat(parameters.hasParameters()).isFalse();
	}

	@Test
	public void getParameterCountShouldReturnParameterCount() {
		OperationMethodParameters parameters = new OperationMethodParameters(
				this.exampleMethod, new DefaultParameterNameDiscoverer());
		assertThat(parameters.getParameterCount()).isEqualTo(1);
	}

	@Test
	public void iteratorShouldIterateOperationParameters() {
		OperationMethodParameters parameters = new OperationMethodParameters(
				this.exampleMethod, new DefaultParameterNameDiscoverer());
		Iterator<OperationParameter> iterator = parameters.iterator();
		assertParameters(StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
				false));
	}

	@Test
	public void streamShouldStreamOperationParameters() {
		OperationMethodParameters parameters = new OperationMethodParameters(
				this.exampleMethod, new DefaultParameterNameDiscoverer());
		assertParameters(parameters.stream());
	}

	private void assertParameters(Stream<OperationParameter> stream) {
		List<OperationParameter> parameters = stream.collect(Collectors.toList());
		assertThat(parameters).hasSize(1);
		OperationParameter parameter = parameters.get(0);
		assertThat(parameter.getName()).isEqualTo("name");
		assertThat(parameter.getType()).isEqualTo(String.class);
	}

	String example(String name) {
		return name;
	}

	String exampleNoParams() {
		return "example";
	}

}
