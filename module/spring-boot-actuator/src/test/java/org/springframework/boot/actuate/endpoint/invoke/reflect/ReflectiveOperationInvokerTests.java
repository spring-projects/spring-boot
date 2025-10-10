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

package org.springframework.boot.actuate.endpoint.invoke.reflect;

import java.lang.reflect.Method;
import java.util.Collections;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.invoke.MissingParametersException;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReflectiveOperationInvoker}.
 *
 * @author Phillip Webb
 */
class ReflectiveOperationInvokerTests {

	private Example target;

	private OperationMethod operationMethod;

	private ParameterValueMapper parameterValueMapper;

	@BeforeEach
	void setup() {
		this.target = new Example();
		this.operationMethod = new OperationMethod(
				findMethod("reverse", ApiVersion.class, SecurityContext.class, String.class), OperationType.READ);
		this.parameterValueMapper = (parameter, value) -> (value != null) ? value.toString() : null;
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenTargetIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new ReflectiveOperationInvoker(null, this.operationMethod, this.parameterValueMapper))
			.withMessageContaining("'target' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenOperationMethodIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new ReflectiveOperationInvoker(this.target, null, this.parameterValueMapper))
			.withMessageContaining("'operationMethod' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenParameterValueMapperIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new ReflectiveOperationInvoker(this.target, this.operationMethod, null))
			.withMessageContaining("'parameterValueMapper' must not be null");
	}

	@Test
	void invokeShouldInvokeMethod() {
		ReflectiveOperationInvoker invoker = new ReflectiveOperationInvoker(this.target, this.operationMethod,
				this.parameterValueMapper);
		Object result = invoker
			.invoke(new InvocationContext(mock(SecurityContext.class), Collections.singletonMap("name", "boot")));
		assertThat(result).isEqualTo("toob");
	}

	@Test
	void invokeWhenMissingNonOptionalArgumentShouldThrowException() {
		ReflectiveOperationInvoker invoker = new ReflectiveOperationInvoker(this.target, this.operationMethod,
				this.parameterValueMapper);
		assertThatExceptionOfType(MissingParametersException.class).isThrownBy(() -> invoker
			.invoke(new InvocationContext(mock(SecurityContext.class), Collections.singletonMap("name", null))));
	}

	@Test
	void invokeWhenMissingOptionalArgumentShouldInvoke() {
		OperationMethod operationMethod = new OperationMethod(
				findMethod("reverseOptional", ApiVersion.class, SecurityContext.class, String.class),
				OperationType.READ);
		ReflectiveOperationInvoker invoker = new ReflectiveOperationInvoker(this.target, operationMethod,
				this.parameterValueMapper);
		Object result = invoker
			.invoke(new InvocationContext(mock(SecurityContext.class), Collections.singletonMap("name", null)));
		assertThat(result).isEqualTo("llun");
	}

	@Test
	void invokeShouldResolveParameters() {
		ReflectiveOperationInvoker invoker = new ReflectiveOperationInvoker(this.target, this.operationMethod,
				this.parameterValueMapper);
		Object result = invoker
			.invoke(new InvocationContext(mock(SecurityContext.class), Collections.singletonMap("name", 1234)));
		assertThat(result).isEqualTo("4321");
	}

	private Method findMethod(String name, Class<?>... parameters) {
		Method method = ReflectionUtils.findMethod(Example.class, name, parameters);
		assertThat(method).as("Method '%s'", name).isNotNull();
		return method;
	}

	static class Example {

		String reverse(ApiVersion apiVersion, SecurityContext securityContext, String name) {
			assertThat(apiVersion).isEqualTo(ApiVersion.LATEST);
			assertThat(securityContext).isNotNull();
			return new StringBuilder(name).reverse().toString();
		}

		String reverseOptional(ApiVersion apiVersion, SecurityContext securityContext, @Nullable String name) {
			assertThat(apiVersion).isEqualTo(ApiVersion.LATEST);
			assertThat(securityContext).isNotNull();
			return new StringBuilder(String.valueOf(name)).reverse().toString();
		}

	}

}
