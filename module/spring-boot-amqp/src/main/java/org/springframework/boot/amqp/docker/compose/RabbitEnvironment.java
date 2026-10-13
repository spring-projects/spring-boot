/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.docker.compose;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * RabbitMQ environment details.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class RabbitEnvironment {

	private final @Nullable String username;

	private final @Nullable String password;

	RabbitEnvironment(Map<String, @Nullable String> env) {
		this.username = extract(env, "RABBITMQ_DEFAULT_USER", "RABBITMQ_USERNAME");
		this.password = extract(env, "RABBITMQ_DEFAULT_PASS", "RABBITMQ_PASSWORD");
	}

	private static String extract(Map<String, @Nullable String> env, String key, String fallbackKey) {
		String value = env.get(key);
		value = (value != null) ? value : env.get(fallbackKey);
		return (value != null) ? value : "guest";
	}

	@Nullable String getUsername() {
		return this.username;
	}

	@Nullable String getPassword() {
		return this.password;
	}

}
