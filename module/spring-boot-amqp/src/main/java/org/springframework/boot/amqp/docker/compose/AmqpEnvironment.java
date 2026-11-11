/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.docker.compose;

import org.jspecify.annotations.Nullable;

/**
 * Generic AMQP environment details.
 *
 * @author Stephane Nicoll
 */
class AmqpEnvironment {

	private final @Nullable String username;

	private final @Nullable String password;

	AmqpEnvironment(@Nullable String username, @Nullable String password) {
		this.username = username;
		this.password = password;
	}

	@Nullable String getUsername() {
		return this.username;
	}

	@Nullable String getPassword() {
		return this.password;
	}

}
