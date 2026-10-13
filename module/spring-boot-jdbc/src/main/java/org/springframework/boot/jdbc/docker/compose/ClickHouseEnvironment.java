/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.docker.compose;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * ClickHouse environment details.
 *
 * @author Stephane Nicoll
 */
class ClickHouseEnvironment {

	private final String username;

	private final String password;

	private final String database;

	ClickHouseEnvironment(Map<String, @Nullable String> env) {
		this.username = extractUsername(env);
		this.password = extractPassword(env);
		this.database = extractDatabase(env);
	}

	private static String extractDatabase(Map<String, @Nullable String> env) {
		String result = env.get("CLICKHOUSE_DB");
		return (result != null) ? result : "default";
	}

	private static String extractUsername(Map<String, @Nullable String> env) {
		String result = env.get("CLICKHOUSE_USER");
		return (result != null) ? result : "default";
	}

	private String extractPassword(Map<String, @Nullable String> env) {
		boolean allowEmpty = env.containsKey("ALLOW_EMPTY_PASSWORD");
		String password = env.get("CLICKHOUSE_PASSWORD");
		Assert.state(StringUtils.hasLength(password) || allowEmpty, "No ClickHouse password found");
		return (password != null) ? password : "";
	}

	String getUsername() {
		return this.username;
	}

	String getPassword() {
		return this.password;
	}

	String getDatabase() {
		return this.database;
	}

}
