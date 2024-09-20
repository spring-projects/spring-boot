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

package org.springframework.boot.docker.compose.service.connection.db2;

import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * DB2 environment details.
 *
 * @author Yanming Zhou
 */
class Db2Environment {

	private final String username;

	private final String password;

	private final String database;

	Db2Environment(Map<String, String> env) {
		this.username = env.getOrDefault("DB2INSTANCE", "db2inst1");
		this.password = env.get("DB2INST1_PASSWORD");
		Assert.state(StringUtils.hasLength(this.password), "DB2 password must be provided");
		this.database = env.get("DBNAME");
		Assert.state(StringUtils.hasLength(this.database), "DB2 database must be provided");
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
