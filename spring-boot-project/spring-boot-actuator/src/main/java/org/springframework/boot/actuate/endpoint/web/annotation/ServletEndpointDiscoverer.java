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
import java.util.Collections;

import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.annotation.DiscoveredOperationMethod;
import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.ExposableServletEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;

/**
 * {@link EndpointDiscoverer} for {@link ExposableServletEndpoint servlet endpoints}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ServletEndpointDiscoverer
		extends EndpointDiscoverer<ExposableServletEndpoint, Operation>
		implements ServletEndpointsSupplier {

	private final PathMapper endpointPathMapper;

	/**
	 * Create a new {@link ServletEndpointDiscoverer} instance.
	 * @param applicationContext the source application context
	 * @param endpointPathMapper the endpoint path mapper
	 * @param filters filters to apply
	 */
	public ServletEndpointDiscoverer(ApplicationContext applicationContext,
			PathMapper endpointPathMapper,
			Collection<EndpointFilter<ExposableServletEndpoint>> filters) {
		super(applicationContext, ParameterValueMapper.NONE, Collections.emptyList(),
				filters);
		Assert.notNull(endpointPathMapper, "EndpointPathMapper must not be null");
		this.endpointPathMapper = endpointPathMapper;
	}

	@Override
	protected boolean isEndpointExposed(Object endpointBean) {
		Class<?> type = endpointBean.getClass();
		return AnnotatedElementUtils.isAnnotated(type, ServletEndpoint.class);
	}

	@Override
	protected ExposableServletEndpoint createEndpoint(Object endpointBean, String id,
			boolean enabledByDefault, Collection<Operation> operations) {
		String rootPath = this.endpointPathMapper.getRootPath(id);
		return new DiscoveredServletEndpoint(this, endpointBean, id, rootPath,
				enabledByDefault);
	}

	@Override
	protected Operation createOperation(String endpointId,
			DiscoveredOperationMethod operationMethod, OperationInvoker invoker) {
		throw new IllegalStateException("ServletEndpoints must not declare operations");
	}

	@Override
	protected OperationKey createOperationKey(Operation operation) {
		throw new IllegalStateException("ServletEndpoints must not declare operations");
	}

}
