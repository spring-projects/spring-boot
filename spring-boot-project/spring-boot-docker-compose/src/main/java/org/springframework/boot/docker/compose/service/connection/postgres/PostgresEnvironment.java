/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.docker.compose.service.connection.postgres;

import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Postgres environment details.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Sidmar Theodoro
 * @author He Zean
 */
class PostgresEnvironment {

	private static final String[] USERNAME_KEYS = new String[] { "POSTGRES_USER", "POSTGRESQL_USER",
			"POSTGRESQL_USERNAME" };

	private static final String DEFAULT_USERNAME = "postgres";

	private static final String[] DATABASE_KEYS = new String[] { "POSTGRES_DB", "POSTGRESQL_DB",
			"POSTGRESQL_DATABASE" };

	private final String username;

	private final String password;

	private final String database;

	PostgresEnvironment(Map<String, String> env) {
		this.username = extract(env, USERNAME_KEYS, DEFAULT_USERNAME);
		this.password = extractPassword(env);
		this.database = extract(env, DATABASE_KEYS, this.username);
	}

	private String extract(Map<String, String> env, String[] keys, String defaultValue) {
		for (String key : keys) {
			if (env.containsKey(key)) {
				return env.get(key);
			}
		}
		return defaultValue;
	}

	private String extractPassword(Map<String, String> env) {
		if (isUsingTrustHostAuthMethod(env)) {
			return null;
		}
		String password = env.getOrDefault("POSTGRES_PASSWORD", env.get("POSTGRESQL_PASSWORD"));
		boolean allowEmpty = env.containsKey("ALLOW_EMPTY_PASSWORD");
		Assert.state(allowEmpty || StringUtils.hasLength(password), "No PostgreSQL password found");
		return (password != null) ? password : "";
	}

	private boolean isUsingTrustHostAuthMethod(Map<String, String> env) {
		String hostAuthMethod = env.get("POSTGRES_HOST_AUTH_METHOD");
		return "trust".equals(hostAuthMethod);
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
