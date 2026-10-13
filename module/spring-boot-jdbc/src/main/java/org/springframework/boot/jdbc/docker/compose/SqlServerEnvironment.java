/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.docker.compose;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * MS SQL Server environment details.
 *
 * @author Andy Wilkinson
 */
class SqlServerEnvironment {

	private final String username = "SA";

	private final String password;

	SqlServerEnvironment(Map<String, @Nullable String> env) {
		this.password = extractPassword(env);
	}

	private String extractPassword(Map<String, @Nullable String> env) {
		String password = env.get("MSSQL_SA_PASSWORD");
		password = (password != null) ? password : env.get("SA_PASSWORD");
		Assert.state(StringUtils.hasLength(password), "No MSSQL password found");
		return password;
	}

	String getUsername() {
		return this.username;
	}

	String getPassword() {
		return this.password;
	}

}
