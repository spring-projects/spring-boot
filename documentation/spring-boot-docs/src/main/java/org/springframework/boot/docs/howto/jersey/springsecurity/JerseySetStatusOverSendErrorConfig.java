/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.jersey.springsecurity;

import java.util.Collections;

import org.glassfish.jersey.server.ResourceConfig;

import org.springframework.stereotype.Component;

@Component
public class JerseySetStatusOverSendErrorConfig extends ResourceConfig {

	public JerseySetStatusOverSendErrorConfig() {
		register(Endpoint.class);
		setProperties(Collections.singletonMap("jersey.config.server.response.setStatusOverSendError", true));
	}

}
