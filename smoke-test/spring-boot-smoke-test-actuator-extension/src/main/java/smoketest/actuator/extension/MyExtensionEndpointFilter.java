/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator.extension;

import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.core.env.Environment;

class MyExtensionEndpointFilter extends IncludeExcludeEndpointFilter<ExposableWebEndpoint> {

	static final String PROPERTY_PREFIX = "management.endpoints.myextension.exposure";

	MyExtensionEndpointFilter(Environment environment) {
		super(ExposableWebEndpoint.class, environment, PROPERTY_PREFIX, "*");
	}

}
