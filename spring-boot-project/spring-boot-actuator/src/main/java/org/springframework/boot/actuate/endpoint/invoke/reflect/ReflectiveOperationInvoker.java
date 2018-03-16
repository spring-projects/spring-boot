/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.invoke.reflect;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.invoke.MissingParametersException;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.OperationParameter;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * An {@code OperationInvoker} that invokes an operation using reflection.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ReflectiveOperationInvoker implements OperationInvoker {

	private final Object target;

	private final OperationMethod operationMethod;

	private final ParameterValueMapper parameterValueMapper;

	/**
	 * Creates a new {code ReflectiveOperationInvoker} that will invoke the given
	 * {@code method} on the given {@code target}. The given {@code parameterMapper} will
	 * be used to map parameters to the required types and the given
	 * {@code parameterNameMapper} will be used map parameters by name.
	 * @param target the target of the reflective call
	 * @param operationMethod the method info
	 * @param parameterValueMapper the parameter mapper
	 */
	public ReflectiveOperationInvoker(Object target, OperationMethod operationMethod,
			ParameterValueMapper parameterValueMapper) {
		Assert.notNull(target, "Target must not be null");
		Assert.notNull(operationMethod, "OperationMethod must not be null");
		Assert.notNull(parameterValueMapper, "ParameterValueMapper must not be null");
		ReflectionUtils.makeAccessible(operationMethod.getMethod());
		this.target = target;
		this.operationMethod = operationMethod;
		this.parameterValueMapper = parameterValueMapper;
	}

	@Override
	public Object invoke(InvocationContext context) {
		validateRequiredParameters(context);
		Method method = this.operationMethod.getMethod();
		Object[] resolvedArguments = resolveArguments(context);
		ReflectionUtils.makeAccessible(method);
		return ReflectionUtils.invokeMethod(method, this.target, resolvedArguments);
	}

	private void validateRequiredParameters(InvocationContext context) {
		Set<OperationParameter> missing = this.operationMethod.getParameters().stream()
				.filter((parameter) -> isMissing(context, parameter))
				.collect(Collectors.toSet());
		if (!missing.isEmpty()) {
			throw new MissingParametersException(missing);
		}
	}

	private boolean isMissing(InvocationContext context, OperationParameter parameter) {
		if (!parameter.isMandatory()) {
			return false;
		}
		if (Principal.class.equals(parameter.getType())) {
			return context.getSecurityContext().getPrincipal() == null;
		}
		if (SecurityContext.class.equals(parameter.getType())) {
			return false;
		}
		return context.getArguments().get(parameter.getName()) == null;
	}

	private Object[] resolveArguments(InvocationContext context) {
		return this.operationMethod.getParameters().stream()
				.map((parameter) -> resolveArgument(parameter, context)).toArray();
	}

	private Object resolveArgument(OperationParameter parameter,
			InvocationContext context) {
		if (Principal.class.equals(parameter.getType())) {
			return context.getSecurityContext().getPrincipal();
		}
		if (SecurityContext.class.equals(parameter.getType())) {
			return context.getSecurityContext();
		}
		Object value = context.getArguments().get(parameter.getName());
		return this.parameterValueMapper.mapParameterValue(parameter, value);
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("target", this.target)
				.append("method", this.operationMethod).toString();
	}

}
