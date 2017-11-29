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

package org.springframework.boot.actuate.endpoint.jmx.annotation;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.OperationInvoker;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.annotation.OperationFactory;
import org.springframework.boot.actuate.endpoint.jmx.JmxEndpointOperationParameterInfo;
import org.springframework.boot.actuate.endpoint.jmx.JmxOperation;
import org.springframework.boot.actuate.endpoint.reflect.OperationMethodInfo;
import org.springframework.jmx.export.metadata.ManagedOperation;
import org.springframework.jmx.export.metadata.ManagedOperationParameter;
import org.springframework.util.StringUtils;

/**
 * {@link OperationFactory} for {@link JmxOperation JMX operations}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class JmxEndpointOperationFactory implements OperationFactory<JmxOperation> {

	@Override
	public JmxOperation createOperation(String endpointId, OperationMethodInfo methodInfo,
			Object target, OperationInvoker invoker) {
		Method method = methodInfo.getMethod();
		String name = method.getName();
		OperationType operationType = methodInfo.getOperationType();
		Class<?> outputType = getJmxType(method.getReturnType());
		String description = getDescription(method,
				() -> "Invoke " + name + " for endpoint " + endpointId);
		return new JmxOperation(operationType, invoker, name, outputType, description,
				getParameters(methodInfo));
	}

	private String getDescription(Method method, Supplier<String> fallback) {
		ManagedOperation managedOperation = JmxAnnotationEndpointDiscoverer.jmxAttributeSource
				.getManagedOperation(method);
		if (managedOperation != null
				&& StringUtils.hasText(managedOperation.getDescription())) {
			return managedOperation.getDescription();
		}
		return fallback.get();
	}

	private List<JmxEndpointOperationParameterInfo> getParameters(
			OperationMethodInfo methodInfo) {
		if (methodInfo.getParameters().isEmpty()) {
			return Collections.emptyList();
		}
		Method method = methodInfo.getMethod();
		ManagedOperationParameter[] operationParameters = JmxAnnotationEndpointDiscoverer.jmxAttributeSource
				.getManagedOperationParameters(method);
		if (operationParameters.length == 0) {
			return methodInfo.getParameters().entrySet().stream().map(this::getParameter)
					.collect(Collectors.toCollection(ArrayList::new));
		}
		return mergeParameters(method.getParameters(), operationParameters);
	}

	private List<JmxEndpointOperationParameterInfo> mergeParameters(
			Parameter[] methodParameters,
			ManagedOperationParameter[] operationParameters) {
		List<JmxEndpointOperationParameterInfo> parameters = new ArrayList<>();
		for (int i = 0; i < operationParameters.length; i++) {
			ManagedOperationParameter operationParameter = operationParameters[i];
			Parameter methodParameter = methodParameters[i];
			JmxEndpointOperationParameterInfo parameter = getParameter(
					operationParameter.getName(), methodParameter,
					operationParameter.getDescription());
			parameters.add(parameter);
		}
		return parameters;
	}

	private JmxEndpointOperationParameterInfo getParameter(
			Map.Entry<String, Parameter> entry) {
		return getParameter(entry.getKey(), entry.getValue(), null);
	}

	private JmxEndpointOperationParameterInfo getParameter(String name,
			Parameter methodParameter, String description) {
		return new JmxEndpointOperationParameterInfo(name,
				getJmxType(methodParameter.getType()), description);
	}

	private Class<?> getJmxType(Class<?> type) {
		if (type.isEnum()) {
			return String.class;
		}
		if (Date.class.isAssignableFrom(type)) {
			return String.class;
		}
		if (type.getName().startsWith("java.")) {
			return type;
		}
		if (type.equals(Void.TYPE)) {
			return type;
		}
		return Object.class;
	}

}
