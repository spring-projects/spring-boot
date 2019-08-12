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

package org.springframework.boot.actuate.endpoint.jmx.annotation;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.annotation.DiscoveredOperationMethod;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.jmx.JmxOperationParameter;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DiscoveredJmxOperation}.
 *
 * @author Phillip Webb
 */
class DiscoveredJmxOperationTests {

	@Test
	void getNameShouldReturnMethodName() {
		DiscoveredJmxOperation operation = getOperation("getEnum");
		assertThat(operation.getName()).isEqualTo("getEnum");
	}

	@Test
	void getOutputTypeShouldReturnJmxType() {
		assertThat(getOperation("getEnum").getOutputType()).isEqualTo(String.class);
		assertThat(getOperation("getDate").getOutputType()).isEqualTo(String.class);
		assertThat(getOperation("getInstant").getOutputType()).isEqualTo(String.class);
		assertThat(getOperation("getInteger").getOutputType()).isEqualTo(Integer.class);
		assertThat(getOperation("getVoid").getOutputType()).isEqualTo(void.class);
		assertThat(getOperation("getApplicationContext").getOutputType()).isEqualTo(Object.class);
	}

	@Test
	void getDescriptionWhenHasManagedOperationDescriptionShouldUseValueFromAnnotation() {
		DiscoveredJmxOperation operation = getOperation("withManagedOperationDescription");
		assertThat(operation.getDescription()).isEqualTo("fromannotation");
	}

	@Test
	void getDescriptionWhenHasNoManagedOperationShouldGenerateDescription() {
		DiscoveredJmxOperation operation = getOperation("getEnum");
		assertThat(operation.getDescription()).isEqualTo("Invoke getEnum for endpoint test");
	}

	@Test
	void getParametersWhenHasNoParametersShouldReturnEmptyList() {
		DiscoveredJmxOperation operation = getOperation("getEnum");
		assertThat(operation.getParameters()).isEmpty();
	}

	@Test
	void getParametersShouldReturnJmxTypes() {
		DiscoveredJmxOperation operation = getOperation("params");
		List<JmxOperationParameter> parameters = operation.getParameters();
		assertThat(parameters.get(0).getType()).isEqualTo(String.class);
		assertThat(parameters.get(1).getType()).isEqualTo(String.class);
		assertThat(parameters.get(2).getType()).isEqualTo(String.class);
		assertThat(parameters.get(3).getType()).isEqualTo(Integer.class);
		assertThat(parameters.get(4).getType()).isEqualTo(Object.class);
	}

	@Test
	void getParametersWhenHasManagedOperationParameterShouldUseValuesFromAnnotation() {
		DiscoveredJmxOperation operation = getOperation("withManagedOperationParameters");
		List<JmxOperationParameter> parameters = operation.getParameters();
		assertThat(parameters.get(0).getName()).isEqualTo("a1");
		assertThat(parameters.get(1).getName()).isEqualTo("a2");
		assertThat(parameters.get(0).getDescription()).isEqualTo("d1");
		assertThat(parameters.get(1).getDescription()).isEqualTo("d2");
	}

	@Test
	void getParametersWhenHasNoManagedOperationParameterShouldDeducedValuesName() {
		DiscoveredJmxOperation operation = getOperation("params");
		List<JmxOperationParameter> parameters = operation.getParameters();
		assertThat(parameters.get(0).getName()).isEqualTo("enumParam");
		assertThat(parameters.get(1).getName()).isEqualTo("dateParam");
		assertThat(parameters.get(2).getName()).isEqualTo("instantParam");
		assertThat(parameters.get(3).getName()).isEqualTo("integerParam");
		assertThat(parameters.get(4).getName()).isEqualTo("applicationContextParam");
		assertThat(parameters.get(0).getDescription()).isNull();
		assertThat(parameters.get(1).getDescription()).isNull();
		assertThat(parameters.get(2).getDescription()).isNull();
		assertThat(parameters.get(3).getDescription()).isNull();
		assertThat(parameters.get(4).getDescription()).isNull();
	}

	private DiscoveredJmxOperation getOperation(String methodName) {
		Method method = findMethod(methodName);
		AnnotationAttributes annotationAttributes = new AnnotationAttributes();
		annotationAttributes.put("produces", "application/xml");
		DiscoveredOperationMethod operationMethod = new DiscoveredOperationMethod(method, OperationType.READ,
				annotationAttributes);
		DiscoveredJmxOperation operation = new DiscoveredJmxOperation(EndpointId.of("test"), operationMethod,
				mock(OperationInvoker.class));
		return operation;
	}

	private Method findMethod(String methodName) {
		Map<String, Method> methods = new HashMap<>();
		ReflectionUtils.doWithMethods(Example.class, (method) -> methods.put(method.getName(), method));
		return methods.get(methodName);
	}

	interface Example {

		OperationType getEnum();

		Date getDate();

		Instant getInstant();

		Integer getInteger();

		void getVoid();

		ApplicationContext getApplicationContext();

		Object params(OperationType enumParam, Date dateParam, Instant instantParam, Integer integerParam,
				ApplicationContext applicationContextParam);

		@ManagedOperation(description = "fromannotation")
		Object withManagedOperationDescription();

		@ManagedOperationParameters({ @ManagedOperationParameter(name = "a1", description = "d1"),
				@ManagedOperationParameter(name = "a2", description = "d2") })
		Object withManagedOperationParameters(Object one, Object two);

	}

}
