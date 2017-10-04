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
import org.springframework.boot.actuate.endpoint.OperationInvoker;
import org.springframework.boot.actuate.endpoint.OperationParameterMapper;
import org.springframework.boot.actuate.endpoint.OperationType;
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
	 * @param parameterMapper the {@link OperationParameterMapper} used to convert
	 * arguments when an operation is invoked
	 * @param cachingConfigurationFactory the {@link CachingConfiguration} factory to use
	 */
	public JmxAnnotationEndpointDiscoverer(ApplicationContext applicationContext,
			OperationParameterMapper parameterMapper,
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

		private final OperationParameterMapper parameterMapper;

		JmxEndpointOperationFactory(OperationParameterMapper parameterMapper) {
			this.parameterMapper = parameterMapper;
		}

		@Override
		public JmxEndpointOperation createOperation(String endpointId,
				AnnotationAttributes operationAttributes, Object target, Method method,
				OperationType type, long timeToLive) {
			String operationName = method.getName();
			Class<?> outputType = mapParameterType(method.getReturnType());
			String description = getDescription(method,
					() -> "Invoke " + operationName + " for endpoint " + endpointId);
			List<JmxEndpointOperationParameterInfo> parameters = getParameters(method);
			OperationInvoker invoker = new ReflectiveOperationInvoker(
					this.parameterMapper, target, method);
			if (timeToLive > 0) {
				invoker = new CachingOperationInvoker(invoker, timeToLive);
			}
			return new JmxEndpointOperation(type, invoker, operationName, outputType,
					description, parameters);
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

		private List<JmxEndpointOperationParameterInfo> getParameters(Method method) {
			Parameter[] methodParameters = method.getParameters();
			if (methodParameters.length == 0) {
				return Collections.emptyList();
			}
			ManagedOperationParameter[] managedOperationParameters = jmxAttributeSource
					.getManagedOperationParameters(method);
			if (managedOperationParameters.length == 0) {
				return getParametersInfo(methodParameters);
			}
			return getParametersInfo(methodParameters, managedOperationParameters);
		}

		private List<JmxEndpointOperationParameterInfo> getParametersInfo(
				Parameter[] methodParameters) {
			List<JmxEndpointOperationParameterInfo> parameters = new ArrayList<>();
			for (Parameter methodParameter : methodParameters) {
				String name = methodParameter.getName();
				Class<?> type = mapParameterType(methodParameter.getType());
				parameters.add(new JmxEndpointOperationParameterInfo(name, type, null));
			}
			return parameters;
		}

		private List<JmxEndpointOperationParameterInfo> getParametersInfo(
				Parameter[] methodParameters,
				ManagedOperationParameter[] managedOperationParameters) {
			List<JmxEndpointOperationParameterInfo> parameters = new ArrayList<>();
			for (int i = 0; i < managedOperationParameters.length; i++) {
				ManagedOperationParameter managedOperationParameter = managedOperationParameters[i];
				Parameter methodParameter = methodParameters[i];
				parameters.add(new JmxEndpointOperationParameterInfo(
						managedOperationParameter.getName(),
						mapParameterType(methodParameter.getType()),
						managedOperationParameter.getDescription()));
			}
			return parameters;
		}

		private Class<?> mapParameterType(Class<?> parameter) {
			if (parameter.isEnum()) {
				return String.class;
			}
			if (Date.class.isAssignableFrom(parameter)) {
				return String.class;
			}
			if (parameter.getName().startsWith("java.")) {
				return parameter;
			}
			if (parameter.equals(Void.TYPE)) {
				return parameter;
			}
			return Object.class;
		}

	}

}
