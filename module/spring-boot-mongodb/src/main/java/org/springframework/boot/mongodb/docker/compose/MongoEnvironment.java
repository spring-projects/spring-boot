/*
 * Copyright 2012-present the original author or authors.
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
