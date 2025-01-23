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

package org.springframework.boot.docker.compose.service.connection.clickhouse;

import java.util.Map;

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

	ClickHouseEnvironment(Map<String, String> env) {
		this.username = env.getOrDefault("CLICKHOUSE_USER", "default");
		this.password = extractPassword(env);
		this.database = env.getOrDefault("CLICKHOUSE_DB", "default");
	}

	private String extractPassword(Map<String, String> env) {
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
