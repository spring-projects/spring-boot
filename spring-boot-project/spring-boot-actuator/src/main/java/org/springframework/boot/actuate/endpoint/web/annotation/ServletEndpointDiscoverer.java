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

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.annotation.DiscoveredOperationMethod;
import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.ExposableServletEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointDiscoverer.ServletEndpointDiscovererRuntimeHints;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

/**
 * {@link EndpointDiscoverer} for {@link ExposableServletEndpoint servlet endpoints}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@ImportRuntimeHints(ServletEndpointDiscovererRuntimeHints.class)
public class ServletEndpointDiscoverer extends EndpointDiscoverer<ExposableServletEndpoint, Operation>
		implements ServletEndpointsSupplier {

	private final List<PathMapper> endpointPathMappers;

	/**
	 * Create a new {@link ServletEndpointDiscoverer} instance.
	 * @param applicationContext the source application context
	 * @param endpointPathMappers the endpoint path mappers
	 * @param filters filters to apply
	 */
	public ServletEndpointDiscoverer(ApplicationContext applicationContext, List<PathMapper> endpointPathMappers,
			Collection<EndpointFilter<ExposableServletEndpoint>> filters) {
		super(applicationContext, ParameterValueMapper.NONE, Collections.emptyList(), filters);
		this.endpointPathMappers = endpointPathMappers;
	}

	/**
     * Determines if the given bean type is exposed as an endpoint type.
     * 
     * @param beanType the bean type to check
     * @return {@code true} if the bean type is exposed as an endpoint type, {@code false} otherwise
     */
    @Override
	protected boolean isEndpointTypeExposed(Class<?> beanType) {
		return MergedAnnotations.from(beanType, SearchStrategy.SUPERCLASS).isPresent(ServletEndpoint.class);
	}

	/**
     * Creates a new ExposableServletEndpoint based on the provided parameters.
     * 
     * @param endpointBean the bean representing the endpoint
     * @param id the unique identifier for the endpoint
     * @param enabledByDefault whether the endpoint is enabled by default
     * @param operations the collection of operations supported by the endpoint
     * @return the created ExposableServletEndpoint
     */
    @Override
	protected ExposableServletEndpoint createEndpoint(Object endpointBean, EndpointId id, boolean enabledByDefault,
			Collection<Operation> operations) {
		String rootPath = PathMapper.getRootPath(this.endpointPathMappers, id);
		return new DiscoveredServletEndpoint(this, endpointBean, id, rootPath, enabledByDefault);
	}

	/**
     * Creates an operation for a ServletEndpoint.
     * 
     * @param endpointId The ID of the endpoint.
     * @param operationMethod The discovered operation method.
     * @param invoker The operation invoker.
     * @return The created operation.
     * @throws IllegalStateException if a ServletEndpoint declares operations.
     */
    @Override
	protected Operation createOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod,
			OperationInvoker invoker) {
		throw new IllegalStateException("ServletEndpoints must not declare operations");
	}

	/**
     * Creates an operation key for the given operation.
     * 
     * @param operation the operation to create the key for
     * @return the operation key
     * @throws IllegalStateException if a ServletEndpoint declares operations
     */
    @Override
	protected OperationKey createOperationKey(Operation operation) {
		throw new IllegalStateException("ServletEndpoints must not declare operations");
	}

	/**
     * ServletEndpointDiscovererRuntimeHints class.
     */
    static class ServletEndpointDiscovererRuntimeHints implements RuntimeHintsRegistrar {

		/**
         * Registers the runtime hints for the ServletEndpointDiscoverer.
         * 
         * @param hints the runtime hints to register
         * @param classLoader the class loader to use for reflection
         */
        @Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.reflection().registerType(ServletEndpointFilter.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		}

	}

}
