/*
 * Copyright 2012-2024 the original author or authors.
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
 */
class PostgresEnvironment {

	private final String username;

	private final String password;

	private final String database;

	/**
     * Constructs a new PostgresEnvironment object with the provided environment variables.
     * 
     * @param env a Map containing the environment variables
     */
    PostgresEnvironment(Map<String, String> env) {
		this.username = env.getOrDefault("POSTGRES_USER", env.getOrDefault("POSTGRESQL_USER", "postgres"));
		this.password = extractPassword(env);
		this.database = env.getOrDefault("POSTGRES_DB", env.getOrDefault("POSTGRESQL_DB", this.username));
	}

	/**
     * Extracts the password from the environment variables.
     * 
     * @param env the map of environment variables
     * @return the extracted password
     * @throws IllegalStateException if the PostgreSQL password is not provided
     */
    private String extractPassword(Map<String, String> env) {
		String password = env.getOrDefault("POSTGRES_PASSWORD", env.get("POSTGRESQL_PASSWORD"));
		Assert.state(StringUtils.hasLength(password), "PostgreSQL password must be provided");
		return password;
	}

	/**
     * Returns the username associated with the PostgresEnvironment object.
     *
     * @return the username
     */
    String getUsername() {
		return this.username;
	}

	/**
     * Retrieves the password associated with the PostgresEnvironment object.
     * 
     * @return the password as a String
     */
    String getPassword() {
		return this.password;
	}

	/**
     * Returns the name of the database.
     *
     * @return the name of the database
     */
    String getDatabase() {
		return this.database;
	}

}
