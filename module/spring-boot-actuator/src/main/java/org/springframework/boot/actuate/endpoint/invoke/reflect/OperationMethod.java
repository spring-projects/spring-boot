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
import java.lang.reflect.Parameter;
import java.util.Locale;
import java.util.function.Predicate;

import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.invoke.OperationParameters;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.Assert;

/**
 * Information describing an operation method on an endpoint method.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @see ReflectiveOperationInvoker
 */
public class OperationMethod {

	private static final ParameterNameDiscoverer DEFAULT_PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

	private final Method method;

	private final OperationType operationType;

	private final OperationParameters operationParameters;

	/**
	 * Create a new {@link OperationMethod} instance.
	 * @param method the source method
	 * @param operationType the operation type
	 * @deprecated since 4.0.0 for removal in 4.2.0 in favor of
	 * {@link #OperationMethod(Method, OperationType, Predicate)}
	 */
	@Deprecated(since = "4.0.0", forRemoval = true)
	public OperationMethod(Method method, OperationType operationType) {
		this(method, operationType, (parameter) -> false);
	}

	/**
	 * Create a new {@link OperationMethod} instance.
	 * @param method the source method
	 * @param operationType the operation type
	 * @param optionalParameters predicate to test if a parameter is optional
	 * @since 4.0.0
	 */
	public OperationMethod(Method method, OperationType operationType, Predicate<Parameter> optionalParameters) {
		Assert.notNull(method, "'method' must not be null");
		Assert.notNull(operationType, "'operationType' must not be null");
		this.method = method;
		this.operationType = operationType;
		this.operationParameters = new OperationMethodParameters(method, DEFAULT_PARAMETER_NAME_DISCOVERER,
				optionalParameters);
	}

	/**
	 * Return the source Java method.
	 * @return the method
	 */
	public Method getMethod() {
		return this.method;
	}

	/**
	 * Return the operation type.
	 * @return the operation type
	 */
	public OperationType getOperationType() {
		return this.operationType;
	}

	/**
	 * Return the operation parameters.
	 * @return the operation parameters
	 */
	public OperationParameters getParameters() {
		return this.operationParameters;
	}

	@Override
	public String toString() {
		return "Operation " + this.operationType.name().toLowerCase(Locale.ENGLISH) + " method " + this.method;
	}

}
