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
import java.util.List;

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
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer.WebEndpointDiscovererRuntimeHints;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * {@link EndpointDiscoverer} for {@link ExposableWebEndpoint web endpoints}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@ImportRuntimeHints(WebEndpointDiscovererRuntimeHints.class)
public class WebEndpointDiscoverer extends EndpointDiscoverer<ExposableWebEndpoint, WebOperation>
		implements WebEndpointsSupplier {

	private final List<PathMapper> endpointPathMappers;

	private final RequestPredicateFactory requestPredicateFactory;

	/**
	 * Create a new {@link WebEndpointDiscoverer} instance.
	 * @param applicationContext the source application context
	 * @param parameterValueMapper the parameter value mapper
	 * @param endpointMediaTypes the endpoint media types
	 * @param endpointPathMappers the endpoint path mappers
	 * @param invokerAdvisors invoker advisors to apply
	 * @param filters filters to apply
	 */
	public WebEndpointDiscoverer(ApplicationContext applicationContext, ParameterValueMapper parameterValueMapper,
			EndpointMediaTypes endpointMediaTypes, List<PathMapper> endpointPathMappers,
			Collection<OperationInvokerAdvisor> invokerAdvisors,
			Collection<EndpointFilter<ExposableWebEndpoint>> filters) {
		super(applicationContext, parameterValueMapper, invokerAdvisors, filters);
		this.endpointPathMappers = endpointPathMappers;
		this.requestPredicateFactory = new RequestPredicateFactory(endpointMediaTypes);
	}

	/**
	 * Creates a new {@link ExposableWebEndpoint} based on the provided parameters.
	 * @param endpointBean the bean representing the endpoint
	 * @param id the unique identifier for the endpoint
	 * @param enabledByDefault whether the endpoint is enabled by default
	 * @param operations the collection of operations supported by the endpoint
	 * @return the created {@link ExposableWebEndpoint}
	 */
	@Override
	protected ExposableWebEndpoint createEndpoint(Object endpointBean, EndpointId id, boolean enabledByDefault,
			Collection<WebOperation> operations) {
		String rootPath = PathMapper.getRootPath(this.endpointPathMappers, id);
		return new DiscoveredWebEndpoint(this, endpointBean, id, rootPath, enabledByDefault, operations);
	}

	/**
	 * Creates a new WebOperation object based on the provided endpointId,
	 * operationMethod, and invoker.
	 * @param endpointId The identifier of the endpoint.
	 * @param operationMethod The method representing the discovered operation.
	 * @param invoker The invoker responsible for invoking the operation.
	 * @return The created WebOperation object.
	 */
	@Override
	protected WebOperation createOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod,
			OperationInvoker invoker) {
		String rootPath = PathMapper.getRootPath(this.endpointPathMappers, endpointId);
		WebOperationRequestPredicate requestPredicate = this.requestPredicateFactory.getRequestPredicate(rootPath,
				operationMethod);
		return new DiscoveredWebOperation(endpointId, operationMethod, invoker, requestPredicate);
	}

	/**
	 * Creates an operation key for the given web operation.
	 * @param operation the web operation
	 * @return the operation key
	 */
	@Override
	protected OperationKey createOperationKey(WebOperation operation) {
		return new OperationKey(operation.getRequestPredicate(),
				() -> "web request predicate " + operation.getRequestPredicate());
	}

	/**
	 * WebEndpointDiscovererRuntimeHints class.
	 */
	static class WebEndpointDiscovererRuntimeHints implements RuntimeHintsRegistrar {

		/**
		 * Registers the runtime hints for the WebEndpointDiscoverer class.
		 * @param hints the runtime hints to register
		 * @param classLoader the class loader to use for reflection
		 */
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.reflection().registerType(WebEndpointFilter.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		}

	}

}
