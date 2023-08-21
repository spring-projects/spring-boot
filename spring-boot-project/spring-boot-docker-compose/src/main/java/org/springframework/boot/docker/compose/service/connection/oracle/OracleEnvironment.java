/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.oracle;

import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Oracle Database environment details.
 *
 * @author Andy Wilkinson
 */
class OracleEnvironment {

	private final String username;

	private final String password;

	private final String database;

	OracleEnvironment(Map<String, String> env) {
		this.username = env.getOrDefault("APP_USER", "system");
		this.password = extractPassword(env);
		this.database = env.getOrDefault("ORACLE_DATABASE", "xepdb1");
	}

	private String extractPassword(Map<String, String> env) {
		if (env.containsKey("APP_USER")) {
			String password = env.get("APP_USER_PASSWORD");
			Assert.state(StringUtils.hasLength(password), "No Oracle app password found");
			return password;
		}
		Assert.state(!env.containsKey("ORACLE_RANDOM_PASSWORD"),
				"ORACLE_RANDOM_PASSWORD is not supported without APP_USER and APP_USER_PASSWORD");
		String password = env.get("ORACLE_PASSWORD");
		Assert.state(StringUtils.hasLength(password), "No Oracle password found");
		return password;
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
