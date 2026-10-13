/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.neo4j.docker.compose;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link Neo4jEnvironment}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class Neo4jEnvironmentTests {

	@Test
	void whenNeo4jAuthAndPasswordAreNullThenAuthTokenIsNull() {
		Neo4jEnvironment environment = new Neo4jEnvironment(Collections.emptyMap());
		assertThat(environment.getAuthToken()).isNull();
	}

	@Test
	void whenNeo4jAuthIsNoneThenAuthTokenIsNone() {
		Neo4jEnvironment environment = new Neo4jEnvironment(Map.of("NEO4J_AUTH", "none"));
		assertThat(environment.getAuthToken()).isEqualTo(AuthTokens.none());
	}

	@Test
	void whenNeo4jAuthIsNeo4jSlashPasswordThenAuthTokenIsBasic() {
		Neo4jEnvironment environment = new Neo4jEnvironment(Map.of("NEO4J_AUTH", "neo4j/custom-password"));
		assertThat(environment.getAuthToken()).isEqualTo(AuthTokens.basic("neo4j", "custom-password"));
	}

	@Test
	void whenNeo4jAuthIsNeitherNoneNorNeo4jSlashPasswordEnvironmentCreationThrows() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new Neo4jEnvironment(Map.of("NEO4J_AUTH", "graphdb/custom-password")));
	}

	@Test
	void whenNeo4jPasswordIsProvidedThenAuthTokenIsBasic() {
		Neo4jEnvironment environment = new Neo4jEnvironment(Map.of("NEO4J_PASSWORD", "custom-password"));
		assertThat(environment.getAuthToken()).isEqualTo(AuthTokens.basic("neo4j", "custom-password"));
	}

}
