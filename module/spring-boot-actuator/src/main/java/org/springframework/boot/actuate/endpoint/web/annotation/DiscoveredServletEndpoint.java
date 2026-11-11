/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.util.Collections;
import java.util.function.Supplier;

import org.springframework.boot.actuate.endpoint.Access;
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
@SuppressWarnings("removal")
class DiscoveredServletEndpoint extends AbstractDiscoveredEndpoint<Operation> implements ExposableServletEndpoint {

	private final String rootPath;

	private final EndpointServlet endpointServlet;

	DiscoveredServletEndpoint(EndpointDiscoverer<?, ?> discoverer, Object endpointBean, EndpointId id, String rootPath,
			Access defaultAccess) {
		super(discoverer, endpointBean, id, defaultAccess, Collections.emptyList());
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
