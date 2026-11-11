/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jersey;

import org.glassfish.jersey.server.ResourceConfig;

import org.springframework.stereotype.Component;

@Component
public class JerseyConfig extends ResourceConfig {

	public JerseyConfig() {
		register(Endpoint.class);
		register(ReverseEndpoint.class);
	}

}
