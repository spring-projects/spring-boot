/*
 * Copyright 2012-2024 the original author or authors.
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

	/**
     * Creates a new instance of DiscoveredJmxOperation.
     * 
     * @param endpointId the ID of the endpoint
     * @param operationMethod the discovered operation method
     * @param invoker the operation invoker
     */
    DiscoveredJmxOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod, OperationInvoker invoker) {
		super(operationMethod, invoker);
		Method method = operationMethod.getMethod();
		this.name = method.getName();
		this.outputType = JmxType.get(method.getReturnType());
		this.description = getDescription(method, () -> "Invoke " + this.name + " for endpoint " + endpointId);
		this.parameters = getParameters(operationMethod);
	}

	/**
     * Returns the description of the given method, if available. If the method is a managed operation and has a description
     * specified in the ManagedOperation annotation, that description is returned. Otherwise, the fallback supplier is used
     * to provide a default description.
     *
     * @param method   the method for which to retrieve the description
     * @param fallback a supplier that provides a default description if the method does not have a managed operation
     *                 annotation or if the annotation does not have a description
     * @return the description of the method, or the default description provided by the fallback supplier
     */
    private String getDescription(Method method, Supplier<String> fallback) {
		ManagedOperation managed = jmxAttributeSource.getManagedOperation(method);
		if (managed != null && StringUtils.hasText(managed.getDescription())) {
			return managed.getDescription();
		}
		return fallback.get();
	}

	/**
     * Retrieves the parameters for the given operation method.
     * 
     * @param operationMethod The operation method for which to retrieve the parameters.
     * @return The list of JmxOperationParameter objects representing the parameters of the operation method.
     */
    private List<JmxOperationParameter> getParameters(OperationMethod operationMethod) {
		if (!operationMethod.getParameters().hasParameters()) {
			return Collections.emptyList();
		}
		Method method = operationMethod.getMethod();
		ManagedOperationParameter[] managed = jmxAttributeSource.getManagedOperationParameters(method);
		if (managed.length == 0) {
			Stream<JmxOperationParameter> parameters = operationMethod.getParameters()
				.stream()
				.map(DiscoveredJmxOperationParameter::new);
			return parameters.toList();
		}
		return mergeParameters(operationMethod.getParameters(), managed);
	}

	/**
     * Merges the given operation parameters with the managed parameters and returns a list of merged JmxOperationParameter objects.
     * 
     * @param operationParameters The OperationParameters object containing the operation parameters.
     * @param managedParameters The array of ManagedOperationParameter objects representing the managed parameters.
     * @return A list of merged JmxOperationParameter objects.
     */
    private List<JmxOperationParameter> mergeParameters(OperationParameters operationParameters,
			ManagedOperationParameter[] managedParameters) {
		List<JmxOperationParameter> merged = new ArrayList<>(managedParameters.length);
		for (int i = 0; i < managedParameters.length; i++) {
			merged.add(new DiscoveredJmxOperationParameter(managedParameters[i], operationParameters.get(i)));
		}
		return Collections.unmodifiableList(merged);
	}

	/**
     * Returns the name of the DiscoveredJmxOperation.
     *
     * @return the name of the DiscoveredJmxOperation
     */
    @Override
	public String getName() {
		return this.name;
	}

	/**
     * Returns the output type of the JMX operation.
     *
     * @return the output type of the JMX operation
     */
    @Override
	public Class<?> getOutputType() {
		return this.outputType;
	}

	/**
     * Returns the description of the DiscoveredJmxOperation.
     *
     * @return the description of the DiscoveredJmxOperation
     */
    @Override
	public String getDescription() {
		return this.description;
	}

	/**
     * Returns the list of JmxOperationParameter objects associated with this DiscoveredJmxOperation.
     *
     * @return the list of JmxOperationParameter objects
     */
    @Override
	public List<JmxOperationParameter> getParameters() {
		return this.parameters;
	}

	/**
     * Appends the fields of the DiscoveredJmxOperation object to the ToStringCreator.
     * 
     * @param creator the ToStringCreator object to append the fields to
     */
    @Override
	protected void appendFields(ToStringCreator creator) {
		creator.append("name", this.name)
			.append("outputType", this.outputType)
			.append("description", this.description)
			.append("parameters", this.parameters);
	}

	/**
	 * A discovered {@link JmxOperationParameter}.
	 */
	private static class DiscoveredJmxOperationParameter implements JmxOperationParameter {

		private final String name;

		private final Class<?> type;

		private final String description;

		/**
         * Creates a new instance of DiscoveredJmxOperationParameter using the provided OperationParameter.
         * 
         * @param operationParameter the OperationParameter to be used for creating the DiscoveredJmxOperationParameter
         */
        DiscoveredJmxOperationParameter(OperationParameter operationParameter) {
			this.name = operationParameter.getName();
			this.type = JmxType.get(operationParameter.getType());
			this.description = null;
		}

		/**
         * Constructs a new DiscoveredJmxOperationParameter object based on the provided ManagedOperationParameter and OperationParameter.
         * 
         * @param managedParameter the ManagedOperationParameter object containing information about the parameter
         * @param operationParameter the OperationParameter object containing information about the parameter
         */
        DiscoveredJmxOperationParameter(ManagedOperationParameter managedParameter,
				OperationParameter operationParameter) {
			this.name = managedParameter.getName();
			this.type = JmxType.get(operationParameter.getType());
			this.description = managedParameter.getDescription();
		}

		/**
         * Returns the name of the DiscoveredJmxOperationParameter.
         *
         * @return the name of the DiscoveredJmxOperationParameter
         */
        @Override
		public String getName() {
			return this.name;
		}

		/**
         * Returns the type of the JMX operation parameter.
         * 
         * @return the type of the JMX operation parameter
         */
        @Override
		public Class<?> getType() {
			return this.type;
		}

		/**
         * Returns the description of the JMX operation parameter.
         *
         * @return the description of the JMX operation parameter
         */
        @Override
		public String getDescription() {
			return this.description;
		}

		/**
         * Returns a string representation of the DiscoveredJmxOperationParameter object.
         * 
         * @return a string representation of the object
         */
        @Override
		public String toString() {
			StringBuilder result = new StringBuilder(this.name);
			if (this.description != null) {
				result.append(" (").append(this.description).append(")");
			}
			result.append(":").append(this.type);
			return result.toString();
		}

	}

	/**
	 * Utility to convert to JMX supported types.
	 */
	private static final class JmxType {

		/**
         * Returns the class type based on the given source class.
         * 
         * @param source the source class
         * @return the class type
         */
        static Class<?> get(Class<?> source) {
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
