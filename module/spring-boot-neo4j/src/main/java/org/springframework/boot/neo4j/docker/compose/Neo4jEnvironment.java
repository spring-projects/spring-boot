/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.neo4j.docker.compose;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;

/**
 * Neo4j environment details.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class Neo4jEnvironment {

	private final @Nullable AuthToken authToken;

	Neo4jEnvironment(Map<String, @Nullable String> env) {
		AuthToken authToken = parse(env.get("NEO4J_AUTH"));
		if (authToken == null && env.containsKey("NEO4J_PASSWORD")) {
			authToken = parse("neo4j/" + env.get("NEO4J_PASSWORD"));
		}
		this.authToken = authToken;
	}

	private @Nullable AuthToken parse(@Nullable String neo4jAuth) {
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

	@Nullable AuthToken getAuthToken() {
		return this.authToken;
	}

}
