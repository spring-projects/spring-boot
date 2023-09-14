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
 */
class Neo4jEnvironmentTests {

	@Test
	void whenNeo4jAuthIsNullThenAuthTokenIsNull() {
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

}
