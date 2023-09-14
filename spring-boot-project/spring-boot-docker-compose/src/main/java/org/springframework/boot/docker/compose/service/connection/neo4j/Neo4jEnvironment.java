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

package org.springframework.boot.docker.compose.service.connection.neo4j;

import java.util.Map;

import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;

/**
 * Neo4j environment details.
 *
 * @author Andy Wilkinson
 */
class Neo4jEnvironment {

	private final AuthToken authToken;

	Neo4jEnvironment(Map<String, String> env) {
		this.authToken = parse(env.get("NEO4J_AUTH"));
	}

	private AuthToken parse(String neo4jAuth) {
		if (neo4jAuth == null) {
			return null;
		}
		if ("none".equals(neo4jAuth)) {
			return AuthTokens.none();
		}
		if (neo4jAuth.startsWith("neo4j/")) {
			return AuthTokens.basic("neo4j", neo4jAuth.substring(6));
		}
		throw new IllegalStateException(
				"Cannot extract auth token from NEO4J_AUTH environment variable with value '" + neo4jAuth + "'."
						+ " Value should be 'none' to disable authentication or start with 'neo4j/' to specify"
						+ " the neo4j user's password");
	}

	AuthToken getAuthToken() {
		return this.authToken;
	}

}
