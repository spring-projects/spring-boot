/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.activemq.docker.compose;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * ActiveMQ environment details.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
class ActiveMQClassicEnvironment {

	private final @Nullable String user;

	private final @Nullable String password;

	ActiveMQClassicEnvironment(Map<String, @Nullable String> env) {
		this.user = env.get("ACTIVEMQ_CONNECTION_USER");
		this.password = env.get("ACTIVEMQ_CONNECTION_PASSWORD");
	}

	@Nullable String getUser() {
		return this.user;
	}

	@Nullable String getPassword() {
		return this.password;
	}

}
