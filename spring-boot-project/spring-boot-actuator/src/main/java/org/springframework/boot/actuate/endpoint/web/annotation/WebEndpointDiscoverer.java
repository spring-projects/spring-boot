/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.util.Collection;

import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.annotation.DiscoveredOperationMethod;
import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.EndpointPathResolver;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;
import org.springframework.context.ApplicationContext;

/**
 * {@link EndpointDiscoverer} for {@link ExposableWebEndpoint web endpoints}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class WebEndpointDiscoverer
		extends EndpointDiscoverer<ExposableWebEndpoint, WebOperation>
		implements WebEndpointsSupplier {

	private final RequestPredicateFactory requestPredicateFactory;

	/**
	 * Create a new {@link WebEndpointDiscoverer} instance.
	 * @param applicationContext the source application context
	 * @param parameterValueMapper the parameter value mapper
	 * @param endpointMediaTypes the endpoint media types
	 * @param endpointPathResolver the endpoint path resolver
	 * @param invokerAdvisors invoker advisors to apply
	 * @param filters filters to apply
	 */
	public WebEndpointDiscoverer(ApplicationContext applicationContext,
			ParameterValueMapper parameterValueMapper,
			EndpointMediaTypes endpointMediaTypes,
			EndpointPathResolver endpointPathResolver,
			Collection<OperationInvokerAdvisor> invokerAdvisors,
			Collection<EndpointFilter<ExposableWebEndpoint>> filters) {
		super(applicationContext, parameterValueMapper, invokerAdvisors, filters);
		this.requestPredicateFactory = new RequestPredicateFactory(endpointMediaTypes,
				endpointPathResolver);
	}

	@Override
	protected ExposableWebEndpoint createEndpoint(String id, boolean enabledByDefault,
			Collection<WebOperation> operations) {
		return new DiscoveredWebEndpoint(this, id, enabledByDefault, operations);
	}

	@Override
	protected WebOperation createOperation(String endpointId,
			DiscoveredOperationMethod operationMethod, OperationInvoker invoker) {
		WebOperationRequestPredicate requestPredicate = this.requestPredicateFactory
				.getRequestPredicate(endpointId, operationMethod);
		return new DiscoveredWebOperation(endpointId, operationMethod, invoker,
				requestPredicate);
	}

	@Override
	protected OperationKey createOperationKey(WebOperation operation) {
		return new OperationKey(operation.getRequestPredicate(),
				() -> "web request predicate " + operation.getRequestPredicate());
	}

}
