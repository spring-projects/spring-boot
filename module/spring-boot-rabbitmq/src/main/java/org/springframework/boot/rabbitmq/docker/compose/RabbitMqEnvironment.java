/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.docker.compose;

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
class RabbitMqEnvironment {

	private final @Nullable String username;

	private final @Nullable String password;

	private final String virtualHost;

	RabbitMqEnvironment(Map<String, @Nullable String> env) {
		this.username = extract(env, "RABBITMQ_DEFAULT_USER", "RABBITMQ_USERNAME", "guest");
		this.password = extract(env, "RABBITMQ_DEFAULT_PASS", "RABBITMQ_PASSWORD", "guest");
		this.virtualHost = extract(env, "RABBITMQ_DEFAULT_VHOST", "RABBITMQ_VHOST", "/");
	}

	private static String extract(Map<String, @Nullable String> env, String key, String fallbackKey,
			String defaultValue) {
		String value = env.get(key);
		value = (value != null) ? value : env.get(fallbackKey);
		return (value != null) ? value : defaultValue;
	}

	@Nullable String getUsername() {
		return this.username;
	}

	@Nullable String getPassword() {
		return this.password;
	}

	String getVirtualHost() {
		return this.virtualHost;
	}

}
