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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
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

	private static final ParameterNameDiscoverer DEFAULT_PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

	private final Object target;

	private final Method method;

	private final ParameterMapper parameterMapper;

	private final ParameterNameDiscoverer parameterNameDiscoverer;

	/**
	 * Creates a new {code ReflectiveOperationInvoker} that will invoke the given
	 * {@code method} on the given {@code target}. The given {@code parameterMapper} will
	 * be used to map parameters to the required types and the given
	 * {@code parameterNameMapper} will be used map parameters by name.
	 * @param target the target of the reflective call
	 * @param method the method to call
	 * @param parameterMapper the parameter mapper
	 */
	public ReflectiveOperationInvoker(Object target, Method method,
			ParameterMapper parameterMapper) {
		this(target, method, parameterMapper, DEFAULT_PARAMETER_NAME_DISCOVERER);
	}

	/**
	 * Creates a new {code ReflectiveOperationInvoker} that will invoke the given
	 * {@code method} on the given {@code target}. The given {@code parameterMapper} will
	 * be used to map parameters to the required types and the given
	 * {@code parameterNameMapper} will be used map parameters by name.
	 * @param target the target of the reflective call
	 * @param method the method to call
	 * @param parameterMapper the parameter mapper
	 * @param parameterNameDiscoverer the parameter name discoverer
	 */
	public ReflectiveOperationInvoker(Object target, Method method,
			ParameterMapper parameterMapper,
			ParameterNameDiscoverer parameterNameDiscoverer) {
		Assert.notNull(target, "Target must not be null");
		Assert.notNull(method, "Method must not be null");
		Assert.notNull(parameterMapper, "ParameterMapper must not be null");
		Assert.notNull(parameterNameDiscoverer,
				"ParameterNameDiscoverer must not be null");
		ReflectionUtils.makeAccessible(method);
		this.target = target;
		this.method = method;
		this.parameterMapper = parameterMapper;
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * Return the method that will be called on invocation.
	 * @return the method to be called
	 */
	public Method getMethod() {
		return this.method;
	}

	/**
	 * Return the parameters of the method mapped using the given function.
	 * @param mapper a mapper {@link BiFunction} taking the discovered name and parameter
	 * as input and returning the mapped type.
	 * @param <T> the mapped type
	 * @return a list of parameters mapped to the desired type
	 */
	public <T> List<T> getParameters(BiFunction<String, Parameter, T> mapper) {
		return getParameters().entrySet().stream()
				.map((entry) -> mapper.apply(entry.getKey(), entry.getValue()))
				.collect(Collectors.toCollection(ArrayList::new));
	}

	private Map<String, Parameter> getParameters() {
		Parameter[] parameters = this.method.getParameters();
		String[] names = this.parameterNameDiscoverer.getParameterNames(this.method);
		Assert.state(names != null,
				"Failed to extract parameter names for " + this.method);
		Map<String, Parameter> result = new LinkedHashMap<>();
		for (int i = 0; i < names.length; i++) {
			result.put(names[i], parameters[i]);
		}
		return result;
	}

	@Override
	public Object invoke(Map<String, Object> arguments) {
		Map<String, Parameter> parameters = getParameters();
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

	private Object resolveArgument(String name, Parameter parameter,
			Map<String, Object> arguments) {
		Object resolved = arguments.get(name);
		return this.parameterMapper.mapParameter(resolved, parameter.getType());
	}

}
