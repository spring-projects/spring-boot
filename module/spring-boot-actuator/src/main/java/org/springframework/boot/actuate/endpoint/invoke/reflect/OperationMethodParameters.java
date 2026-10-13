/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.invoke.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.invoke.OperationParameter;
import org.springframework.boot.actuate.endpoint.invoke.OperationParameters;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.Assert;

/**
 * {@link OperationParameters} created from an {@link OperationMethod}.
 *
 * @author Phillip Webb
 */
class OperationMethodParameters implements OperationParameters {

	private final List<OperationParameter> operationParameters;

	/**
	 * Create a new {@link OperationMethodParameters} instance.
	 * @param method the source method
	 * @param parameterNameDiscoverer the parameter name discoverer
	 */
	OperationMethodParameters(Method method, ParameterNameDiscoverer parameterNameDiscoverer) {
		Assert.notNull(method, "'method' must not be null");
		Assert.notNull(parameterNameDiscoverer, "'parameterNameDiscoverer' must not be null");
		@Nullable String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
		Parameter[] parameters = method.getParameters();
		Assert.state(parameterNames != null, () -> "Failed to extract parameter names for " + method);
		this.operationParameters = getOperationParameters(parameters, parameterNames);
	}

	private List<OperationParameter> getOperationParameters(Parameter[] parameters, @Nullable String[] names) {
		List<OperationParameter> operationParameters = new ArrayList<>(parameters.length);
		for (int i = 0; i < names.length; i++) {
			String name = names[i];
			Assert.state(name != null, "'name' must not be null");
			operationParameters.add(new OperationMethodParameter(name, parameters[i]));
		}
		return Collections.unmodifiableList(operationParameters);
	}

	@Override
	public int getParameterCount() {
		return this.operationParameters.size();
	}

	@Override
	public OperationParameter get(int index) {
		return this.operationParameters.get(index);
	}

	@Override
	public Iterator<OperationParameter> iterator() {
		return this.operationParameters.iterator();
	}

	@Override
	public Stream<OperationParameter> stream() {
		return this.operationParameters.stream();
	}

}
