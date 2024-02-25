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

package org.springframework.boot.actuate.endpoint.invoke.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

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
		Assert.notNull(method, "Method must not be null");
		Assert.notNull(parameterNameDiscoverer, "ParameterNameDiscoverer must not be null");
		String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
		Parameter[] parameters = method.getParameters();
		Assert.state(parameterNames != null, () -> "Failed to extract parameter names for " + method);
		this.operationParameters = getOperationParameters(parameters, parameterNames);
	}

	/**
	 * Returns a list of operation parameters based on the given parameters and names.
	 * @param parameters the array of parameters
	 * @param names the array of names
	 * @return a list of operation parameters
	 */
	private List<OperationParameter> getOperationParameters(Parameter[] parameters, String[] names) {
		List<OperationParameter> operationParameters = new ArrayList<>(parameters.length);
		for (int i = 0; i < names.length; i++) {
			operationParameters.add(new OperationMethodParameter(names[i], parameters[i]));
		}
		return Collections.unmodifiableList(operationParameters);
	}

	/**
	 * Returns the number of parameters in this operation method.
	 * @return the number of parameters
	 */
	@Override
	public int getParameterCount() {
		return this.operationParameters.size();
	}

	/**
	 * Returns the OperationParameter at the specified index in this
	 * OperationMethodParameters.
	 * @param index the index of the OperationParameter to be returned
	 * @return the OperationParameter at the specified index
	 */
	@Override
	public OperationParameter get(int index) {
		return this.operationParameters.get(index);
	}

	/**
	 * Returns an iterator over the operation parameters in this OperationMethodParameters
	 * object.
	 * @return an iterator over the operation parameters in this OperationMethodParameters
	 * object
	 */
	@Override
	public Iterator<OperationParameter> iterator() {
		return this.operationParameters.iterator();
	}

	/**
	 * Returns a sequential Stream with the operation parameters of this
	 * OperationMethodParameters object as its source.
	 * @return a sequential Stream of OperationParameter objects representing the
	 * operation parameters of this OperationMethodParameters object
	 */
	@Override
	public Stream<OperationParameter> stream() {
		return this.operationParameters.stream();
	}

}
