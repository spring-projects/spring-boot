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

import java.util.Collection;
import java.util.Collections;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.OperationFilter;
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
	 * @param endpointFilters endpoint filters to apply
	 * @deprecated since 3.4.0 for removal in 3.6.0 in favor of
	 * {@link #JmxEndpointDiscoverer(ApplicationContext, ParameterValueMapper, Collection, Collection, Collection)}
	 */
	@Deprecated(since = "3.4.0", forRemoval = true)
	public JmxEndpointDiscoverer(ApplicationContext applicationContext, ParameterValueMapper parameterValueMapper,
			Collection<OperationInvokerAdvisor> invokerAdvisors,
			Collection<EndpointFilter<ExposableJmxEndpoint>> endpointFilters) {
		this(applicationContext, parameterValueMapper, invokerAdvisors, endpointFilters, Collections.emptyList());
	}

	/**
	 * Create a new {@link JmxEndpointDiscoverer} instance.
	 * @param applicationContext the source application context
	 * @param parameterValueMapper the parameter value mapper
	 * @param invokerAdvisors invoker advisors to apply
	 * @param endpointFilters endpoint filters to apply
	 * @param operationFilters operation filters to apply
	 * @since 3.4.0
	 */
	public JmxEndpointDiscoverer(ApplicationContext applicationContext, ParameterValueMapper parameterValueMapper,
			Collection<OperationInvokerAdvisor> invokerAdvisors,
			Collection<EndpointFilter<ExposableJmxEndpoint>> endpointFilters,
			Collection<OperationFilter<JmxOperation>> operationFilters) {
		super(applicationContext, parameterValueMapper, invokerAdvisors, endpointFilters, operationFilters);
	}

	@Override
	protected ExposableJmxEndpoint createEndpoint(Object endpointBean, EndpointId id, Access defaultAccess,
			Collection<JmxOperation> operations) {
		return new DiscoveredJmxEndpoint(this, endpointBean, id, defaultAccess, operations);
	}

	@Override
	protected JmxOperation createOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod,
			OperationInvoker invoker) {
		return new DiscoveredJmxOperation(endpointId, operationMethod, invoker);
	}

	@Override
	protected OperationKey createOperationKey(JmxOperation operation) {
		return new OperationKey(operation.getName(), () -> "MBean call '" + operation.getName() + "'");
	}

	static class JmxEndpointDiscovererRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.reflection().registerType(JmxEndpointFilter.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		}

	}

}
