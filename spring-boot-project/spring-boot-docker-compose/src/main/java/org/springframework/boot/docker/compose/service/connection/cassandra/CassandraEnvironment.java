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

package org.springframework.boot.docker.compose.service.connection.cassandra;

import java.util.Map;

/**
 * Cassandra environment details.
 *
 * @author Scott Frederick
 */
class CassandraEnvironment {

	private final String datacenter;

	/**
	 * Constructs a new CassandraEnvironment object with the given environment variables.
	 * @param env a map containing the environment variables
	 * @throws NullPointerException if the env parameter is null
	 */
	CassandraEnvironment(Map<String, String> env) {
		this.datacenter = env.getOrDefault("CASSANDRA_DC", env.getOrDefault("CASSANDRA_DATACENTER", "datacenter1"));
	}

	/**
	 * Returns the datacenter associated with the Cassandra environment.
	 * @return the datacenter
	 */
	String getDatacenter() {
		return this.datacenter;
	}

}
