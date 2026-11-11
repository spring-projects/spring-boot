/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mongodb.docker.compose;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * MongoDB environment details.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class MongoEnvironment {

	private final @Nullable String username;

	private final @Nullable String password;

	private final @Nullable String database;

	MongoEnvironment(Map<String, @Nullable String> env) {
		Assert.state(!env.containsKey("MONGO_INITDB_ROOT_USERNAME_FILE"),
				"MONGO_INITDB_ROOT_USERNAME_FILE is not supported");
		Assert.state(!env.containsKey("MONGO_INITDB_ROOT_PASSWORD_FILE"),
				"MONGO_INITDB_ROOT_PASSWORD_FILE is not supported");
		this.username = env.getOrDefault("MONGO_INITDB_ROOT_USERNAME", env.get("MONGODB_ROOT_USERNAME"));
		this.password = env.getOrDefault("MONGO_INITDB_ROOT_PASSWORD", env.get("MONGODB_ROOT_PASSWORD"));
		this.database = env.getOrDefault("MONGO_INITDB_DATABASE", env.get("MONGODB_DATABASE"));
	}

	@Nullable String getUsername() {
		return this.username;
	}

	@Nullable String getPassword() {
		return this.password;
	}

	@Nullable String getDatabase() {
		return this.database;
	}

}
