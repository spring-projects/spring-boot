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

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.util.Collections;
import java.util.function.Supplier;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.annotation.AbstractDiscoveredEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.web.EndpointServlet;
import org.springframework.boot.actuate.endpoint.web.ExposableServletEndpoint;
import org.springframework.util.Assert;

/**
 * A discovered {@link ExposableServletEndpoint servlet endpoint}.
 *
 * @author Phillip Webb
 */
class DiscoveredServletEndpoint extends AbstractDiscoveredEndpoint<Operation> implements ExposableServletEndpoint {

	private final String rootPath;

	private final EndpointServlet endpointServlet;

	DiscoveredServletEndpoint(EndpointDiscoverer<?, ?> discoverer, Object endpointBean, EndpointId id, String rootPath,
			boolean enabledByDefault) {
		super(discoverer, endpointBean, id, enabledByDefault, Collections.emptyList());
		String beanType = endpointBean.getClass().getName();
		Assert.state(endpointBean instanceof Supplier,
				() -> "ServletEndpoint bean " + beanType + " must be a supplier");
		Object supplied = ((Supplier<?>) endpointBean).get();
		Assert.state(supplied != null, () -> "ServletEndpoint bean " + beanType + " must not supply null");
		Assert.state(supplied instanceof EndpointServlet,
				() -> "ServletEndpoint bean " + beanType + " must supply an EndpointServlet");
		this.endpointServlet = (EndpointServlet) supplied;
		this.rootPath = rootPath;
	}

	@Override
	public String getRootPath() {
		return this.rootPath;
	}

	@Override
	public EndpointServlet getEndpointServlet() {
		return this.endpointServlet;
	}

}
