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

package org.springframework.boot.actuate.endpoint;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * An {@code OperationInvoker} that invokes an operation using reflection.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class ReflectiveOperationInvoker implements OperationInvoker {

	private final OperationParameterMapper parameterMapper;

	private final Function<Method, Map<String, Parameter>> parameterNameMapper;

	private final Object target;

	private final Method method;

	/**
	 * Creates a new {code ReflectiveOperationInvoker} that will invoke the given
	 * {@code method} on the given {@code target}. The given {@code parameterMapper} will
	 * be used to map parameters to the required types.
	 * @param parameterMapper the parameter mapper
	 * @param target the target of the reflective call
	 * @param method the method to call
	 */
	public ReflectiveOperationInvoker(OperationParameterMapper parameterMapper,
			Function<Method, Map<String, Parameter>> parameterNameMapper,
			Object target, Method method) {
		this.parameterMapper = parameterMapper;
		this.parameterNameMapper = parameterNameMapper;
		this.target = target;
		ReflectionUtils.makeAccessible(method);
		this.method = method;
	}

	@Override
	public Object invoke(Map<String, Object> arguments) {
		Map<String, Parameter> parameters = this.parameterNameMapper.apply(this.method);
		validateRequiredParameters(parameters, arguments);
		return ReflectionUtils.invokeMethod(this.method, this.target,
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

	private Object resolveArgument(String name, Parameter parameter, Map<String, Object> arguments) {
		Object resolved = arguments.get(name);
		return this.parameterMapper.mapParameter(resolved, parameter.getType());
	}

}
