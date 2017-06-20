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
import java.util.Map;

import javax.annotation.PostConstruct;

import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;

/**
 * {@link HealthIndicator} that tests the status of a Neo4j by executing a Cypher
 * statement.
 *
 * @author Eric Spiegelberg
 */
public class Neo4jHealthIndicator extends AbstractHealthIndicator {

	/** The key used to store Cypher execution results within the Health.Builder. */
	public static final String CYPHER_RESULTS = "cypherResults";

	/** The default Cypher query. */
	public static final String CYPHER_DEFUALT = "match (n) return count(n) as nodeCount";

	private SessionFactory sessionFactory;

	private Map<String, Object> emptyParameters = new HashMap<>();

	/**
	 * The default Cypher statement.
	 */
	@Value("${neo4jHealthIndicator.cypher}")
	private String cypher = CYPHER_DEFUALT;

	/**
	 * Create a new {@link Neo4jHealthIndicator} instance.
	 */
	public Neo4jHealthIndicator() {
	}

	/**
	 * Create a new {@link Neo4jHealthIndicator} using the specified
	 * {@link SessionFactory}.
	 * @param sessionFactory the SessionFactory
	 */
	public Neo4jHealthIndicator(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Create a new {@link Neo4jHealthIndicator} using the specified
	 * {@link SessionFactory} and validation query.
	 * @param sessionFactory the SessionFactory
	 * @param cypher the validation cypher query to use
	 */
	public Neo4jHealthIndicator(SessionFactory sessionFactory, String cypher) {
		this.cypher = cypher;
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Ensure that the SessionFactory and Cypher validation query has been supplied.
	 */
	@PostConstruct
	public void postConstruct() {
		Assert.notNull(this.sessionFactory,
				"SessionFactory for Neo4jHealthIndicator must not be null");

		Assert.hasText(this.cypher, "The Cypher statement must be specified");
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		Session session = this.sessionFactory.openSession();

		Result result = session.query(this.cypher, this.emptyParameters);
		Iterable<Map<String, Object>> results = result.queryResults();

		builder.up().withDetail(CYPHER_RESULTS, results);
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

	/**
	 * Set the {@link SessionFactory} to use.
	 * @param sessionFactory The SessionFactory.
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

}
