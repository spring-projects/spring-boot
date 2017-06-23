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

import org.junit.Assert;
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
	public void neo4jUp() {
		given(this.session.query(Neo4jHealthIndicator.CYPHER, this.emptyParameters))
				.willReturn(this.result);

		int nodeCount = 500;
		Map<String, Object> expectedCypherDetails = new HashMap<>();
		expectedCypherDetails.put("nodes", nodeCount);

		List<Map<String, Object>> queryResults = new ArrayList<>();
		queryResults.add(expectedCypherDetails);

		given(this.result.queryResults()).willReturn(queryResults);

		Health health = this.neo4jHealthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);

		Map<String, Object> details = health.getDetails();
		int nodeCountFromDetails = (int) details.get("nodes");

		Assert.assertEquals(nodeCount, nodeCountFromDetails);

	}

	@Test
	public void neo4jDown() {

		CypherException cypherException = new CypherException("Error executing Cypher",
				"Neo.ClientError.Statement.SyntaxError",
				"Unable to execute invalid Cypher");

		given(this.session.query(Neo4jHealthIndicator.CYPHER, this.emptyParameters))
				.willThrow(cypherException);

		Health health = this.neo4jHealthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
	}

}
