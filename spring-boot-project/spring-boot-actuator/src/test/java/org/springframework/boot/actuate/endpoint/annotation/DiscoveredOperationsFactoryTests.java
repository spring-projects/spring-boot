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

package org.springframework.boot.actuate.endpoint.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.OperationParameters;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoke.reflect.OperationMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DiscoveredOperationsFactory}.
 *
 * @author Phillip Webb
 */
public class DiscoveredOperationsFactoryTests {

	private TestDiscoveredOperationsFactory factory;

	private ParameterValueMapper parameterValueMapper;

	private List<OperationInvokerAdvisor> invokerAdvisors;

	@Before
	public void setup() {
		this.parameterValueMapper = (parameter, value) -> value.toString();
		this.invokerAdvisors = new ArrayList<>();
		this.factory = new TestDiscoveredOperationsFactory(this.parameterValueMapper,
				this.invokerAdvisors);
	}

	@Test
	public void createOperationsWhenHasReadMethodShouldCreateOperation() {
		Collection<TestOperation> operations = this.factory
				.createOperations(EndpointId.of("test"), new ExampleRead());
		assertThat(operations).hasSize(1);
		TestOperation operation = getFirst(operations);
		assertThat(operation.getType()).isEqualTo(OperationType.READ);
	}

	@Test
	public void createOperationsWhenHasWriteMethodShouldCreateOperation() {
		Collection<TestOperation> operations = this.factory
				.createOperations(EndpointId.of("test"), new ExampleWrite());
		assertThat(operations).hasSize(1);
		TestOperation operation = getFirst(operations);
		assertThat(operation.getType()).isEqualTo(OperationType.WRITE);
	}

	@Test
	public void createOperationsWhenHasDeleteMethodShouldCreateOperation() {
		Collection<TestOperation> operations = this.factory
				.createOperations(EndpointId.of("test"), new ExampleDelete());
		assertThat(operations).hasSize(1);
		TestOperation operation = getFirst(operations);
		assertThat(operation.getType()).isEqualTo(OperationType.DELETE);
	}

	@Test
	public void createOperationsWhenMultipleShouldReturnMultiple() {
		Collection<TestOperation> operations = this.factory
				.createOperations(EndpointId.of("test"), new ExampleMultiple());
		assertThat(operations).hasSize(2);
		assertThat(operations.stream().map(TestOperation::getType))
				.containsOnly(OperationType.READ, OperationType.WRITE);
	}

	@Test
	public void createOperationsShouldProvideOperationMethod() {
		TestOperation operation = getFirst(this.factory
				.createOperations(EndpointId.of("test"), new ExampleWithParams()));
		OperationMethod operationMethod = operation.getOperationMethod();
		assertThat(operationMethod.getMethod().getName()).isEqualTo("read");
		assertThat(operationMethod.getParameters().hasParameters()).isTrue();
	}

	@Test
	public void createOperationsShouldProviderInvoker() {
		TestOperation operation = getFirst(this.factory
				.createOperations(EndpointId.of("test"), new ExampleWithParams()));
		Map<String, Object> params = Collections.singletonMap("name", 123);
		Object result = operation
				.invoke(new InvocationContext(mock(SecurityContext.class), params));
		assertThat(result).isEqualTo("123");
	}

	@Test
	public void createOperationShouldApplyAdvisors() {
		TestOperationInvokerAdvisor advisor = new TestOperationInvokerAdvisor();
		this.invokerAdvisors.add(advisor);
		TestOperation operation = getFirst(
				this.factory.createOperations(EndpointId.of("test"), new ExampleRead()));
		operation.invoke(new InvocationContext(mock(SecurityContext.class),
				Collections.emptyMap()));
		assertThat(advisor.getEndpointId()).isEqualTo(EndpointId.of("test"));
		assertThat(advisor.getOperationType()).isEqualTo(OperationType.READ);
		assertThat(advisor.getParameters()).isEmpty();
	}

	private <T> T getFirst(Iterable<T> iterable) {
		return iterable.iterator().next();
	}

	static class ExampleRead {

		@ReadOperation
		public String read() {
			return "read";
		}

	}

	static class ExampleWrite {

		@WriteOperation
		public String write() {
			return "write";
		}

	}

	static class ExampleDelete {

		@DeleteOperation
		public String delete() {
			return "delete";
		}

	}

	static class ExampleMultiple {

		@ReadOperation
		public String read() {
			return "read";
		}

		@WriteOperation
		public String write() {
			return "write";
		}

	}

	static class ExampleWithParams {

		@ReadOperation
		public String read(String name) {
			return name;
		}

	}

	static class TestDiscoveredOperationsFactory
			extends DiscoveredOperationsFactory<TestOperation> {

		TestDiscoveredOperationsFactory(ParameterValueMapper parameterValueMapper,
				Collection<OperationInvokerAdvisor> invokerAdvisors) {
			super(parameterValueMapper, invokerAdvisors);
		}

		@Override
		protected TestOperation createOperation(EndpointId endpointId,
				DiscoveredOperationMethod operationMethod, OperationInvoker invoker) {
			return new TestOperation(endpointId, operationMethod, invoker);
		}

	}

	static class TestOperation extends AbstractDiscoveredOperation {

		TestOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod,
				OperationInvoker invoker) {
			super(operationMethod, invoker);
		}

	}

	static class TestOperationInvokerAdvisor implements OperationInvokerAdvisor {

		private EndpointId endpointId;

		private OperationType operationType;

		private OperationParameters parameters;

		@Override
		public OperationInvoker apply(EndpointId endpointId, OperationType operationType,
				OperationParameters parameters, OperationInvoker invoker) {
			this.endpointId = endpointId;
			this.operationType = operationType;
			this.parameters = parameters;
			return invoker;
		}

		public EndpointId getEndpointId() {
			return this.endpointId;
		}

		public OperationType getOperationType() {
			return this.operationType;
		}

		public OperationParameters getParameters() {
			return this.parameters;
		}

	}

}
