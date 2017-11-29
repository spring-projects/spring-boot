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

package org.springframework.boot.actuate.neo4j;

import java.util.Collections;

import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * {@link HealthIndicator} that tests the status of a Neo4j by executing a Cypher
 * statement.
 *
 * @author Eric Spiegelberg
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class Neo4jHealthIndicator extends AbstractHealthIndicator {

	/**
	 * The Cypher statement used to verify Neo4j is up.
	 */
	static final String CYPHER = "match (n) return count(n) as nodes";

	private final SessionFactory sessionFactory;

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
		Result result = session.query(CYPHER, Collections.emptyMap());
		builder.up().withDetail("nodes",
				result.queryResults().iterator().next().get("nodes"));
	}

}
