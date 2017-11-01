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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.EndpointExposure;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.ParameterMapper;
import org.springframework.boot.actuate.endpoint.ReflectiveOperationInvoker;
import org.springframework.boot.actuate.endpoint.annotation.AnnotationEndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.cache.CachingConfiguration;
import org.springframework.boot.actuate.endpoint.cache.CachingConfigurationFactory;
import org.springframework.boot.actuate.endpoint.cache.CachingOperationInvoker;
import org.springframework.boot.actuate.endpoint.jmx.JmxEndpointOperation;
import org.springframework.boot.actuate.endpoint.jmx.JmxEndpointOperationParameterInfo;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.metadata.ManagedOperation;
import org.springframework.jmx.export.metadata.ManagedOperationParameter;
import org.springframework.util.StringUtils;

/**
 * Discovers the {@link Endpoint endpoints} in an {@link ApplicationContext} with
 * {@link JmxEndpointExtension JMX extensions} applied to them.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class JmxAnnotationEndpointDiscoverer
		extends AnnotationEndpointDiscoverer<JmxEndpointOperation, String> {

	private static final AnnotationJmxAttributeSource jmxAttributeSource = new AnnotationJmxAttributeSource();

	/**
	 * Creates a new {@link JmxAnnotationEndpointDiscoverer} that will discover
	 * {@link Endpoint endpoints} and {@link JmxEndpointExtension jmx extensions} using
	 * the given {@link ApplicationContext}.
	 * @param applicationContext the application context
	 * @param parameterMapper the {@link ParameterMapper} used to convert arguments when
	 * an operation is invoked
	 * @param cachingConfigurationFactory the {@link CachingConfiguration} factory to use
	 */
	public JmxAnnotationEndpointDiscoverer(ApplicationContext applicationContext,
			ParameterMapper parameterMapper,
			CachingConfigurationFactory cachingConfigurationFactory) {
		super(applicationContext, new JmxEndpointOperationFactory(parameterMapper),
				JmxEndpointOperation::getOperationName, cachingConfigurationFactory);
	}

	@Override
	public Collection<EndpointInfo<JmxEndpointOperation>> discoverEndpoints() {
		Collection<EndpointInfoDescriptor<JmxEndpointOperation, String>> endpointDescriptors = discoverEndpoints(
				JmxEndpointExtension.class, EndpointExposure.JMX);
		verifyThatOperationsHaveDistinctName(endpointDescriptors);
		return endpointDescriptors.stream().map(EndpointInfoDescriptor::getEndpointInfo)
				.collect(Collectors.toList());
	}

	private void verifyThatOperationsHaveDistinctName(
			Collection<EndpointInfoDescriptor<JmxEndpointOperation, String>> endpointDescriptors) {
		List<List<JmxEndpointOperation>> clashes = new ArrayList<>();
		endpointDescriptors.forEach((descriptor) -> clashes
				.addAll(descriptor.findDuplicateOperations().values()));
		if (!clashes.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append(
					String.format("Found multiple JMX operations with the same name:%n"));
			clashes.forEach((clash) -> {
				message.append("    ").append(clash.get(0).getOperationName())
						.append(String.format(":%n"));
				clash.forEach((operation) -> message.append("        ")
						.append(String.format("%s%n", operation)));
			});
			throw new IllegalStateException(message.toString());
		}
	}

	private static class JmxEndpointOperationFactory
			implements EndpointOperationFactory<JmxEndpointOperation> {

		private final ParameterMapper parameterMapper;

		JmxEndpointOperationFactory(ParameterMapper parameterMapper) {
			this.parameterMapper = parameterMapper;
		}

		@Override
		public JmxEndpointOperation createOperation(String endpointId,
				AnnotationAttributes operationAttributes, Object target, Method method,
				OperationType type, long timeToLive) {
			ReflectiveOperationInvoker invoker = new ReflectiveOperationInvoker(target,
					method, this.parameterMapper);
			String operationName = method.getName();
			Class<?> outputType = getJmxType(method.getReturnType());
			String description = getDescription(method,
					() -> "Invoke " + operationName + " for endpoint " + endpointId);
			List<JmxEndpointOperationParameterInfo> parameters = getParameters(invoker);
			return new JmxEndpointOperation(type,
					CachingOperationInvoker.apply(invoker, timeToLive), operationName,
					outputType, description, parameters);
		}

		private String getDescription(Method method, Supplier<String> fallback) {
			ManagedOperation managedOperation = jmxAttributeSource
					.getManagedOperation(method);
			if (managedOperation != null
					&& StringUtils.hasText(managedOperation.getDescription())) {
				return managedOperation.getDescription();
			}
			return fallback.get();
		}

		private List<JmxEndpointOperationParameterInfo> getParameters(
				ReflectiveOperationInvoker invoker) {
			if (invoker.getMethod().getParameterCount() == 0) {
				return Collections.emptyList();
			}
			ManagedOperationParameter[] operationParameters = jmxAttributeSource
					.getManagedOperationParameters(invoker.getMethod());
			if (operationParameters.length == 0) {
				return invoker.getParameters(this::getParameter);
			}
			return mergeParameters(invoker.getMethod().getParameters(),
					operationParameters);
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

		private JmxEndpointOperationParameterInfo getParameter(String name,
				Parameter methodParameter) {
			return getParameter(name, methodParameter, null);
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

}
