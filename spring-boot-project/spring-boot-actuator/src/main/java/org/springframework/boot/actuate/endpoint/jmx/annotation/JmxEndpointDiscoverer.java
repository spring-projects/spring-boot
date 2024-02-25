/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.Collection;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.DiscoveredOperationMethod;
import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.jmx.ExposableJmxEndpoint;
import org.springframework.boot.actuate.endpoint.jmx.JmxEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.jmx.JmxOperation;
import org.springframework.boot.actuate.endpoint.jmx.annotation.JmxEndpointDiscoverer.JmxEndpointDiscovererRuntimeHints;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * {@link EndpointDiscoverer} for {@link ExposableJmxEndpoint JMX endpoints}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@ImportRuntimeHints(JmxEndpointDiscovererRuntimeHints.class)
public class JmxEndpointDiscoverer extends EndpointDiscoverer<ExposableJmxEndpoint, JmxOperation>
		implements JmxEndpointsSupplier {

	/**
	 * Create a new {@link JmxEndpointDiscoverer} instance.
	 * @param applicationContext the source application context
	 * @param parameterValueMapper the parameter value mapper
	 * @param invokerAdvisors invoker advisors to apply
	 * @param filters filters to apply
	 */
	public JmxEndpointDiscoverer(ApplicationContext applicationContext, ParameterValueMapper parameterValueMapper,
			Collection<OperationInvokerAdvisor> invokerAdvisors,
			Collection<EndpointFilter<ExposableJmxEndpoint>> filters) {
		super(applicationContext, parameterValueMapper, invokerAdvisors, filters);
	}

	/**
	 * Creates a new ExposableJmxEndpoint instance based on the provided parameters.
	 * @param endpointBean the bean representing the endpoint
	 * @param id the unique identifier for the endpoint
	 * @param enabledByDefault indicates if the endpoint is enabled by default
	 * @param operations the collection of JmxOperations supported by the endpoint
	 * @return the newly created ExposableJmxEndpoint instance
	 */
	@Override
	protected ExposableJmxEndpoint createEndpoint(Object endpointBean, EndpointId id, boolean enabledByDefault,
			Collection<JmxOperation> operations) {
		return new DiscoveredJmxEndpoint(this, endpointBean, id, enabledByDefault, operations);
	}

	/**
	 * Creates a new JmxOperation object based on the provided endpointId,
	 * operationMethod, and invoker.
	 * @param endpointId The ID of the endpoint.
	 * @param operationMethod The discovered operation method.
	 * @param invoker The operation invoker.
	 * @return The created JmxOperation object.
	 */
	@Override
	protected JmxOperation createOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod,
			OperationInvoker invoker) {
		return new DiscoveredJmxOperation(endpointId, operationMethod, invoker);
	}

	/**
	 * Creates an operation key for the given JmxOperation.
	 * @param operation the JmxOperation for which to create the operation key
	 * @return the created OperationKey
	 */
	@Override
	protected OperationKey createOperationKey(JmxOperation operation) {
		return new OperationKey(operation.getName(), () -> "MBean call '" + operation.getName() + "'");
	}

	/**
	 * JmxEndpointDiscovererRuntimeHints class.
	 */
	static class JmxEndpointDiscovererRuntimeHints implements RuntimeHintsRegistrar {

		/**
		 * Registers the runtime hints for JmxEndpointDiscoverer.
		 * @param hints the runtime hints to register
		 * @param classLoader the class loader to use for reflection
		 */
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.reflection().registerType(JmxEndpointFilter.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		}

	}

}
