/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.health;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.exception.CypherException;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Neo4jHealthIndicator}.
 *
 * @author Eric Spiegelberg
 */
public class Neo4jHealthIndicatorTests {

	private Result result;
	private Session session;
	private SessionFactory sessionFactory;

	private Neo4jHealthIndicator neo4jHealthIndicator;

	private Map<String, Object> emptyParameters = new HashMap<>();

	@Before
	public void before() {
		this.result = mock(Result.class);
		this.session = mock(Session.class);
		this.sessionFactory = mock(SessionFactory.class);

		given(this.sessionFactory.openSession()).willReturn(this.session);

		this.neo4jHealthIndicator = new Neo4jHealthIndicator(this.sessionFactory);
	}

	@Test
	public void defaultValidationQueryNeo4jUp() {
		String cypher = this.neo4jHealthIndicator.getCypher();
		given(this.session.query(cypher, this.emptyParameters)).willReturn(this.result);

		Map<String, Object> expectedCypherDetails = new HashMap<>();
		expectedCypherDetails.put("nodeCount", 500);

		List<Map<String, Object>> queryResults = new ArrayList<>();
		queryResults.add(expectedCypherDetails);

		given(this.result.queryResults()).willReturn(queryResults);

		assertExpectedResults(this.neo4jHealthIndicator, Status.UP,
				expectedCypherDetails);
	}

	@Test
	public void invalidCypherNeo4jDown() {
		String invalidCypher = "invalid cypher";
		this.neo4jHealthIndicator.setCypher(invalidCypher);
		CypherException cypherException = new CypherException("Error executing Cypher",
				"Neo.ClientError.Statement.SyntaxError",
				"Unable to execute invalid Cypher");

		given(this.session.query(invalidCypher, this.emptyParameters))
				.willThrow(cypherException);

		assertExpectedResults(this.neo4jHealthIndicator, Status.DOWN, null);
	}

	protected void assertExpectedResults(Neo4jHealthIndicator neo4jHealthIndicator,
			Status expectedStatus, Map<String, Object> expectedCypherDetails) {
		Health health = neo4jHealthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(expectedStatus);

		Map<String, Object> details = health.getDetails();

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> cypherDetails = (List<Map<String, Object>>) details
				.get(Neo4jHealthIndicator.NEO4J);

		if (expectedCypherDetails == null) {
			assertThat(cypherDetails).isNull();
		}
		else {
			assertThat(cypherDetails).containsOnly(expectedCypherDetails);
		}
	}

}
