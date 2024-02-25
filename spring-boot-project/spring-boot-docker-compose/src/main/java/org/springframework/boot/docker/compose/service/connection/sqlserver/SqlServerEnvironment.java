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

package org.springframework.boot.docker.compose.service.connection.sqlserver;

import java.util.Map;

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

	/**
     * Constructs a new SqlServerEnvironment object with the provided environment variables.
     * 
     * @param env a map containing the environment variables
     */
    SqlServerEnvironment(Map<String, String> env) {
		this.password = extractPassword(env);
	}

	/**
     * Extracts the password from the environment variables.
     * 
     * @param env the map of environment variables
     * @return the extracted password
     * @throws IllegalStateException if no MSSQL password is found
     */
    private String extractPassword(Map<String, String> env) {
		String password = env.get("MSSQL_SA_PASSWORD");
		password = (password != null) ? password : env.get("SA_PASSWORD");
		Assert.state(StringUtils.hasLength(password), "No MSSQL password found");
		return password;
	}

	/**
     * Returns the username associated with the SqlServerEnvironment object.
     *
     * @return the username
     */
    String getUsername() {
		return this.username;
	}

	/**
     * Returns the password used for authentication in the SQL Server environment.
     *
     * @return the password used for authentication
     */
    String getPassword() {
		return this.password;
	}

}
