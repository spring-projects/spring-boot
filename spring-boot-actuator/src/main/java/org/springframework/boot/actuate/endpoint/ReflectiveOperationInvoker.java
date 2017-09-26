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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.util.ReflectionUtils;

/**
 * An {@code OperationInvoker} that invokes an operation using reflection.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class ReflectiveOperationInvoker implements OperationInvoker {

	private final OperationParameterMapper parameterMapper;

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
			Object target, Method method) {
		this.parameterMapper = parameterMapper;
		this.target = target;
		ReflectionUtils.makeAccessible(method);
		this.method = method;
	}

	@Override
	public Object invoke(Map<String, Object> arguments) {
		return ReflectionUtils.invokeMethod(this.method, this.target,
				resolveArguments(arguments));
	}

	private Object[] resolveArguments(Map<String, Object> arguments) {
		return Stream.of(this.method.getParameters())
				.map((parameter) -> resolveArgument(parameter, arguments))
				.collect(Collectors.collectingAndThen(Collectors.toList(),
						(list) -> list.toArray(new Object[list.size()])));
	}

	private Object resolveArgument(Parameter parameter, Map<String, Object> arguments) {
		Object resolved = arguments.get(parameter.getName());
		return this.parameterMapper.mapParameter(resolved, parameter.getType());
	}

}
