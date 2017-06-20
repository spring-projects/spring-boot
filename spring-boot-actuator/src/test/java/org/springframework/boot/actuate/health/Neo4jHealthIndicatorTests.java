/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Neo4jHealthIndicator}.
 *
 * @author Eric Spiegelberg
 */
public class Neo4jHealthIndicatorTests {

	public static final String NODE_COUNT = "nodeCount";
	public static final String RELATIONSHIP_COUNT = "relationshipCount";

	private SessionFactory sessionFactory;

	@Before
	public void before() {
		Configuration configuration = new Configuration.Builder().build();
		this.sessionFactory = new SessionFactory(configuration, "foo.bar");
	}

	@After
	public void after() {
		if (this.sessionFactory != null) {
			this.sessionFactory.close();
		}
	}

	@Test
	public void defaultValidationQuery() {
		Map<String, Object> parameters = new HashMap<>();
		Neo4jHealthIndicator neo4jHealthIndicator = new Neo4jHealthIndicator(
				this.sessionFactory);
		Session session = this.sessionFactory.openSession();
		Result result = session.query(Neo4jHealthIndicator.CYPHER_DEFUALT, parameters);
		long nodeCount = (long) result.iterator().next().get("nodeCount");
		Map<String, Object> expectedCypherDetails = new HashMap<>();
		expectedCypherDetails.put(NODE_COUNT, nodeCount);
		assertExpectedResults(neo4jHealthIndicator, Status.UP, expectedCypherDetails);
	}

	@Test
	public void customValidationQuery() {
		Map<String, Object> parameters = new HashMap<>();
		String customValidationQuery = "MATCH (n)-[r]-() RETURN count(n) as nodeCount, count(distinct(r)) as relationshipCount";
		Session session = this.sessionFactory.openSession();
		Result result = session.query(customValidationQuery, parameters);
		Iterator<Map<String, Object>> results = result.iterator();
		Map<String, Object> r = results.next();
		long nodeCount = (long) r.get("nodeCount");
		Map<String, Object> expectedCypherDetails = new HashMap<>();
		expectedCypherDetails.put(NODE_COUNT, nodeCount);
		long relationshipCount = (long) r.get("relationshipCount");
		expectedCypherDetails.put(RELATIONSHIP_COUNT, relationshipCount);
		Neo4jHealthIndicator neo4jHealthIndicator = new Neo4jHealthIndicator(
				this.sessionFactory);
		neo4jHealthIndicator.setCypher(customValidationQuery);
		assertExpectedResults(neo4jHealthIndicator, Status.UP, expectedCypherDetails);

		// Create 2 nodes and 1 relationship
		try (Transaction transaction = session.beginTransaction()) {
			session.query(
					"create (a:A { name: 'foo' })-[:CONNECTED]->(b:B {name : 'bar'})",
					parameters);
			transaction.commit();
		}

		expectedCypherDetails.put(NODE_COUNT, nodeCount + 2);
		expectedCypherDetails.put(RELATIONSHIP_COUNT, relationshipCount + 1);
		assertExpectedResults(neo4jHealthIndicator, Status.UP, expectedCypherDetails);
	}

	@Test
	public void invalidCustomValidationQuery() {
		String invalidCypher = "invalid cypher statement";
		Neo4jHealthIndicator neo4jHealthIndicator = new Neo4jHealthIndicator(
				this.sessionFactory);
		neo4jHealthIndicator.setCypher(invalidCypher);
		assertExpectedResults(neo4jHealthIndicator, Status.DOWN, null);

		neo4jHealthIndicator = new Neo4jHealthIndicator(this.sessionFactory,
				invalidCypher);
		assertExpectedResults(neo4jHealthIndicator, Status.DOWN, null);
	}

	@Test
	public void postConstruct_success() {
		Neo4jHealthIndicator neo4jHealthIndicator = new Neo4jHealthIndicator();
		neo4jHealthIndicator.setSessionFactory(this.sessionFactory);
		String cypher = "this is the cypher";
		neo4jHealthIndicator.setCypher(cypher);
		neo4jHealthIndicator.postConstruct();
		Assert.assertEquals(cypher, neo4jHealthIndicator.getCypher());
	}

	@Test(expected = IllegalArgumentException.class)
	public void postConstruct_null_session_factor() {
		Neo4jHealthIndicator neo4jHealthIndicator = new Neo4jHealthIndicator();
		neo4jHealthIndicator.postConstruct();
	}

	@Test(expected = IllegalArgumentException.class)
	public void postConstruct_null_cypher() {
		Neo4jHealthIndicator neo4jHealthIndicator = new Neo4jHealthIndicator();
		neo4jHealthIndicator.setSessionFactory(this.sessionFactory);
		neo4jHealthIndicator.setCypher(null);
		neo4jHealthIndicator.postConstruct();
	}

	protected void assertExpectedResults(Neo4jHealthIndicator neo4jHealthIndicator,
			Status expectedStatus, Map<String, Object> expectedCypherDetails) {
		Health health = neo4jHealthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(expectedStatus);

		if (expectedCypherDetails == null) {
			assertThat(health.getDetails().get(Neo4jHealthIndicator.CYPHER_RESULTS))
					.isNull();
		}
		else {
			Map<String, Object> details = health.getDetails();
			assertThat(health.getDetails().get(Neo4jHealthIndicator.CYPHER_RESULTS))
					.isNotNull();
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> cypherDetails = (List<Map<String, Object>>) details
					.get(Neo4jHealthIndicator.CYPHER_RESULTS);
			assertThat(cypherDetails).containsOnly(expectedCypherDetails);
		}
	}

}
