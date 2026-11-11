/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.artemis.docker.compose;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Artemis environment details.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 */
class ArtemisEnvironment {

	private final @Nullable String user;

	private final @Nullable String password;

	ArtemisEnvironment(Map<String, @Nullable String> env) {
		this.user = env.get("ARTEMIS_USER");
		this.password = env.get("ARTEMIS_PASSWORD");
	}

	@Nullable String getUser() {
		return this.user;
	}

	@Nullable String getPassword() {
		return this.password;
	}

}
