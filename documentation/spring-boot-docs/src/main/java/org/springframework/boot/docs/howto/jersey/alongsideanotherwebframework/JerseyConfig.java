/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.jersey.alongsideanotherwebframework;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletProperties;

import org.springframework.stereotype.Component;

@Component
public class JerseyConfig extends ResourceConfig {

	public JerseyConfig() {
		register(Endpoint.class);
		property(ServletProperties.FILTER_FORWARD_ON_404, true);
	}

}
