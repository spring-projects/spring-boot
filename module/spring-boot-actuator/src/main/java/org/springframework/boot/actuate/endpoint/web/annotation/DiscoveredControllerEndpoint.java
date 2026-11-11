/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.util.Collections;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.annotation.AbstractDiscoveredEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;

/**
 * A discovered {@link ExposableControllerEndpoint controller endpoint}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("removal")
class DiscoveredControllerEndpoint extends AbstractDiscoveredEndpoint<Operation>
		implements ExposableControllerEndpoint {

	private final String rootPath;

	DiscoveredControllerEndpoint(EndpointDiscoverer<?, ?> discoverer, Object endpointBean, EndpointId id,
			String rootPath, Access defaultAccess) {
		super(discoverer, endpointBean, id, defaultAccess, Collections.emptyList());
		this.rootPath = rootPath;
	}

	@Override
	public Object getController() {
		return getEndpointBean();
	}

	@Override
	public String getRootPath() {
		return this.rootPath;
	}

}
