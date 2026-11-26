/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jersey.jackson2;

import org.glassfish.jersey.server.ResourceConfig;

import org.springframework.stereotype.Component;

/**
 * Jersey configuration.
 *
 * @author Kristoffer Larsen Hopland
 */
@Component
public class JerseyConfig extends ResourceConfig {

	public JerseyConfig() {
		register(NameResource.class);
	}

}
