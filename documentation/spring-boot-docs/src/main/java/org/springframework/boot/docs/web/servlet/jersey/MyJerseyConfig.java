/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.servlet.jersey;

import org.glassfish.jersey.server.ResourceConfig;

import org.springframework.stereotype.Component;

@Component
public class MyJerseyConfig extends ResourceConfig {

	public MyJerseyConfig() {
		register(MyEndpoint.class);
	}

}
