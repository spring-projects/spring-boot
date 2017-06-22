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

import java.util.HashMap;
import java.util.Map;

import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

/**
 * {@link HealthIndicator} that tests the status of a Neo4j by executing a Cypher
 * statement.
 *
 * @author Eric Spiegelberg
 */
public class Neo4jHealthIndicator extends AbstractHealthIndicator {

	/** The key used to store Cypher execution results within the Health.Builder. */
	public static final String NEO4J = "neo4j";

	private SessionFactory sessionFactory;

	private Map<String, Object> emptyParameters = new HashMap<>();

	/**
	 * The default Cypher statement.
	 */
	private String cypher = "match (n) return count(n) as nodeCount";

	/**
	 * Create a new {@link Neo4jHealthIndicator} using the specified
	 * {@link SessionFactory}.
	 * @param sessionFactory the SessionFactory
	 */
	public Neo4jHealthIndicator(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		Session session = this.sessionFactory.openSession();

		Result result = session.query(this.cypher, this.emptyParameters);
		Iterable<Map<String, Object>> results = result.queryResults();

		builder.up().withDetail(NEO4J, results);
	}

	/**
	 * Return the validation Cypher query or {@code null}.
	 * @return The query or {@code null}.
	 */
	public String getCypher() {
		return this.cypher;
	}

	/**
	 * Set a specific validation Cypher query to use to validate a connection. If none is
	 * set, the default query is used.
	 * @param cypher The cypher query used to evaluate the status of Neo4j.
	 */
	public void setCypher(String cypher) {
		this.cypher = cypher;
	}

}
