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

package org.springframework.boot.actuate.endpoint.jmx.annotation;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.AbstractDiscoveredOperation;
import org.springframework.boot.actuate.endpoint.annotation.DiscoveredOperationMethod;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.OperationParameter;
import org.springframework.boot.actuate.endpoint.invoke.OperationParameters;
import org.springframework.boot.actuate.endpoint.invoke.reflect.OperationMethod;
import org.springframework.boot.actuate.endpoint.jmx.JmxOperation;
import org.springframework.boot.actuate.endpoint.jmx.JmxOperationParameter;
import org.springframework.core.style.ToStringCreator;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.metadata.JmxAttributeSource;
import org.springframework.jmx.export.metadata.ManagedOperation;
import org.springframework.jmx.export.metadata.ManagedOperationParameter;
import org.springframework.util.StringUtils;

/**
 * A discovered {@link JmxOperation JMX operation}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class DiscoveredJmxOperation extends AbstractDiscoveredOperation implements JmxOperation {

	private static final JmxAttributeSource jmxAttributeSource = new AnnotationJmxAttributeSource();

	private final String name;

	private final Class<?> outputType;

	private final String description;

	private final List<JmxOperationParameter> parameters;

	DiscoveredJmxOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod, OperationInvoker invoker) {
		super(operationMethod, invoker);
		Method method = operationMethod.getMethod();
		this.name = method.getName();
		this.outputType = JmxType.get(method.getReturnType());
		this.description = getDescription(method, () -> "Invoke " + this.name + " for endpoint " + endpointId);
		this.parameters = getParameters(operationMethod);
	}

	private String getDescription(Method method, Supplier<String> fallback) {
		ManagedOperation managed = jmxAttributeSource.getManagedOperation(method);
		if (managed != null && StringUtils.hasText(managed.getDescription())) {
			return managed.getDescription();
		}
		return fallback.get();
	}

	private List<JmxOperationParameter> getParameters(OperationMethod operationMethod) {
		if (!operationMethod.getParameters().hasParameters()) {
			return Collections.emptyList();
		}
		Method method = operationMethod.getMethod();
		ManagedOperationParameter[] managed = jmxAttributeSource.getManagedOperationParameters(method);
		if (managed.length == 0) {
			return asList(operationMethod.getParameters().stream().map(DiscoveredJmxOperationParameter::new));
		}
		return mergeParameters(operationMethod.getParameters(), managed);
	}

	private List<JmxOperationParameter> mergeParameters(OperationParameters operationParameters,
			ManagedOperationParameter[] managedParameters) {
		List<JmxOperationParameter> merged = new ArrayList<>(managedParameters.length);
		for (int i = 0; i < managedParameters.length; i++) {
			merged.add(new DiscoveredJmxOperationParameter(managedParameters[i], operationParameters.get(i)));
		}
		return Collections.unmodifiableList(merged);
	}

	private <T> List<T> asList(Stream<T> stream) {
		return stream.collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Class<?> getOutputType() {
		return this.outputType;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public List<JmxOperationParameter> getParameters() {
		return this.parameters;
	}

	@Override
	protected void appendFields(ToStringCreator creator) {
		creator.append("name", this.name).append("outputType", this.outputType).append("description", this.description)
				.append("parameters", this.parameters);
	}

	/**
	 * A discovered {@link JmxOperationParameter}.
	 */
	private static class DiscoveredJmxOperationParameter implements JmxOperationParameter {

		private final String name;

		private final Class<?> type;

		private final String description;

		DiscoveredJmxOperationParameter(OperationParameter operationParameter) {
			this.name = operationParameter.getName();
			this.type = JmxType.get(operationParameter.getType());
			this.description = null;
		}

		DiscoveredJmxOperationParameter(ManagedOperationParameter managedParameter,
				OperationParameter operationParameter) {
			this.name = managedParameter.getName();
			this.type = JmxType.get(operationParameter.getType());
			this.description = managedParameter.getDescription();
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public Class<?> getType() {
			return this.type;
		}

		@Override
		public String getDescription() {
			return this.description;
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder(this.name);
			if (this.description != null) {
				result.append(" (" + this.description + ")");
			}
			result.append(":" + this.type);
			return result.toString();
		}

	}

	/**
	 * Utility to convert to JMX supported types.
	 */
	private static class JmxType {

		public static Class<?> get(Class<?> source) {
			if (source.isEnum()) {
				return String.class;
			}
			if (Date.class.isAssignableFrom(source) || Instant.class.isAssignableFrom(source)) {
				return String.class;
			}
			if (source.getName().startsWith("java.")) {
				return source;
			}
			if (source.equals(Void.TYPE)) {
				return source;
			}
			return Object.class;
		}

	}

}
