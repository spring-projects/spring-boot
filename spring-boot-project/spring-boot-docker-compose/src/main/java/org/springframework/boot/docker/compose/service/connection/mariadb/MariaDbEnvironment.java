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

package org.springframework.boot.docker.compose.service.connection.mariadb;

import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * MariaDB environment details.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class MariaDbEnvironment {

	private final String username;

	private final String password;

	private final String database;

	/**
     * Constructs a new MariaDbEnvironment object with the provided environment variables.
     * 
     * @param env a map containing the environment variables
     * @throws IllegalArgumentException if any of the required environment variables are missing or empty
     */
    MariaDbEnvironment(Map<String, String> env) {
		this.username = extractUsername(env);
		this.password = extractPassword(env);
		this.database = extractDatabase(env);
	}

	/**
     * Extracts the username from the given environment map.
     * 
     * @param env the environment map containing the username
     * @return the extracted username
     */
    private String extractUsername(Map<String, String> env) {
		String user = env.get("MARIADB_USER");
		return (user != null) ? user : env.getOrDefault("MYSQL_USER", "root");
	}

	/**
     * Extracts the password from the given environment variables.
     * 
     * @param env the map of environment variables
     * @return the extracted password
     * @throws IllegalStateException if any unsupported environment variable is found or if no password is found and empty passwords are not allowed
     */
    private String extractPassword(Map<String, String> env) {
		Assert.state(!env.containsKey("MARIADB_RANDOM_ROOT_PASSWORD"), "MARIADB_RANDOM_ROOT_PASSWORD is not supported");
		Assert.state(!env.containsKey("MYSQL_RANDOM_ROOT_PASSWORD"), "MYSQL_RANDOM_ROOT_PASSWORD is not supported");
		Assert.state(!env.containsKey("MARIADB_ROOT_PASSWORD_HASH"), "MARIADB_ROOT_PASSWORD_HASH is not supported");
		boolean allowEmpty = env.containsKey("MARIADB_ALLOW_EMPTY_PASSWORD")
				|| env.containsKey("MYSQL_ALLOW_EMPTY_PASSWORD") || env.containsKey("ALLOW_EMPTY_PASSWORD");
		String password = env.get("MARIADB_PASSWORD");
		password = (password != null) ? password : env.get("MYSQL_PASSWORD");
		password = (password != null) ? password : env.get("MARIADB_ROOT_PASSWORD");
		password = (password != null) ? password : env.get("MYSQL_ROOT_PASSWORD");
		Assert.state(StringUtils.hasLength(password) || allowEmpty, "No MariaDB password found");
		return (password != null) ? password : "";
	}

	/**
     * Extracts the database name from the environment variables.
     * 
     * @param env the map of environment variables
     * @return the database name
     * @throws IllegalStateException if no database name is defined
     */
    private String extractDatabase(Map<String, String> env) {
		String database = env.get("MARIADB_DATABASE");
		database = (database != null) ? database : env.get("MYSQL_DATABASE");
		Assert.state(database != null, "No MARIADB_DATABASE defined");
		return database;
	}

	/**
     * Returns the username associated with the MariaDbEnvironment object.
     *
     * @return the username
     */
    String getUsername() {
		return this.username;
	}

	/**
     * Retrieves the password used for authentication.
     *
     * @return the password used for authentication
     */
    String getPassword() {
		return this.password;
	}

	/**
     * Returns the name of the database associated with this MariaDbEnvironment object.
     *
     * @return the name of the database
     */
    String getDatabase() {
		return this.database;
	}

}
