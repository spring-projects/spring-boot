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

package org.springframework.boot.actuate.endpoint.reflect;

import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.OperationInvoker;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * An {@code OperationInvoker} that invokes an operation using reflection.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class ReflectiveOperationInvoker implements OperationInvoker {

	private final Object target;

	private final OperationMethodInfo methodInfo;

	private final ParameterMapper parameterMapper;

	/**
	 * Creates a new {code ReflectiveOperationInvoker} that will invoke the given
	 * {@code method} on the given {@code target}. The given {@code parameterMapper} will
	 * be used to map parameters to the required types and the given
	 * {@code parameterNameMapper} will be used map parameters by name.
	 * @param target the target of the reflective call
	 * @param methodInfo the method info
	 * @param parameterMapper the parameter mapper
	 */
	public ReflectiveOperationInvoker(Object target, OperationMethodInfo methodInfo,
			ParameterMapper parameterMapper) {
		Assert.notNull(target, "Target must not be null");
		Assert.notNull(methodInfo, "MethodInfo must not be null");
		Assert.notNull(parameterMapper, "ParameterMapper must not be null");
		ReflectionUtils.makeAccessible(methodInfo.getMethod());
		this.target = target;
		this.methodInfo = methodInfo;
		this.parameterMapper = parameterMapper;
	}

	@Override
	public Object invoke(Map<String, Object> arguments) {
		Map<String, Parameter> parameters = this.methodInfo.getParameters();
		validateRequiredParameters(parameters, arguments);
		return ReflectionUtils.invokeMethod(this.methodInfo.getMethod(), this.target,
				resolveArguments(parameters, arguments));
	}

	private void validateRequiredParameters(Map<String, Parameter> parameters,
			Map<String, Object> arguments) {
		Set<String> missingParameters = parameters.keySet().stream()
				.filter((n) -> isMissing(n, parameters.get(n), arguments))
				.collect(Collectors.toSet());
		if (!missingParameters.isEmpty()) {
			throw new ParametersMissingException(missingParameters);
		}
	}

	private boolean isMissing(String name, Parameter parameter,
			Map<String, Object> arguments) {
		Object resolved = arguments.get(name);
		return (resolved == null && !isExplicitNullable(parameter));
	}

	private boolean isExplicitNullable(Parameter parameter) {
		return (parameter.getAnnotationsByType(Nullable.class).length != 0);
	}

	private Object[] resolveArguments(Map<String, Parameter> parameters,
			Map<String, Object> arguments) {
		return parameters.keySet().stream()
				.map((name) -> resolveArgument(name, parameters.get(name), arguments))
				.collect(Collectors.collectingAndThen(Collectors.toList(),
						(list) -> list.toArray(new Object[list.size()])));
	}

	private Object resolveArgument(String name, Parameter parameter,
			Map<String, Object> arguments) {
		Object resolved = arguments.get(name);
		return this.parameterMapper.mapParameter(resolved, parameter.getType());
	}

}
