/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.neo4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.ogm.exception.CypherException;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Neo4jHealthIndicator}.
 *
 * @author Eric Spiegelberg
 * @author Stephane Nicoll
 * @author Michael Simons
 */
class Neo4jHealthIndicatorTests {

	private Session session;

	private Neo4jHealthIndicator neo4jHealthIndicator;

	@BeforeEach
	void before() {
		this.session = mock(Session.class);
		SessionFactory sessionFactory = mock(SessionFactory.class);
		given(sessionFactory.openSession()).willReturn(this.session);
		this.neo4jHealthIndicator = new Neo4jHealthIndicator(sessionFactory);
	}

	@Test
	void neo4jUp() {
		Result result = mock(Result.class);
		given(this.session.query(Neo4jHealthIndicator.CYPHER, Collections.emptyMap())).willReturn(result);
		Map<String, Object> expectedCypherDetails = new HashMap<>();
		String edition = "community";
		String version = "4.0.0";
		expectedCypherDetails.put("edition", edition);
		expectedCypherDetails.put("version", version);
		List<Map<String, Object>> queryResults = new ArrayList<>();
		queryResults.add(expectedCypherDetails);
		given(result.queryResults()).willReturn(queryResults);
		Health health = this.neo4jHealthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		Map<String, Object> details = health.getDetails();
		String editionFromDetails = details.get("edition").toString();
		String versionFromDetails = details.get("version").toString();
		assertThat(editionFromDetails).isEqualTo(edition);
		assertThat(versionFromDetails).isEqualTo(version);
	}

	@Test
	void neo4jDown() {
		CypherException cypherException = new CypherException("Neo.ClientError.Statement.SyntaxError",
				"Error executing Cypher");
		given(this.session.query(Neo4jHealthIndicator.CYPHER, Collections.emptyMap())).willThrow(cypherException);
		Health health = this.neo4jHealthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
	}

}
