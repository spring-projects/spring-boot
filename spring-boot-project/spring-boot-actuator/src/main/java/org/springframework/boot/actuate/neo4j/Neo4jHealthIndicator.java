/*
 * Copyright 2012-2021 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.exceptions.SessionExpiredException;
import org.neo4j.driver.summary.ResultSummary;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * {@link HealthIndicator} that tests the status of a Neo4j by executing a Cypher
 * statement and extracting server and database information.
 *
 * @author Eric Spiegelberg
 * @author Stephane Nicoll
 * @author Michael J. Simons
 * @since 2.0.0
 */
public class Neo4jHealthIndicator extends AbstractHealthIndicator {

	private static final Log logger = LogFactory.getLog(Neo4jHealthIndicator.class);

	/**
	 * The Cypher statement used to verify Neo4j is up.
	 */
	static final String CYPHER = "CALL dbms.components() YIELD versions, name, edition WHERE name = 'Neo4j Kernel' RETURN edition, versions[0] as version";

	/**
	 * Message logged before retrying a health check.
	 */
	static final String MESSAGE_SESSION_EXPIRED = "Neo4j session has expired, retrying one single time to retrieve server health.";

	/**
	 * The default session config to use while connecting.
	 */
	static final SessionConfig DEFAULT_SESSION_CONFIG = SessionConfig.builder().withDefaultAccessMode(AccessMode.WRITE)
			.build();

	private final Driver driver;

	private final Neo4jHealthDetailsHandler healthDetailsHandler;

	public Neo4jHealthIndicator(Driver driver) {
		super("Neo4j health check failed");
		this.driver = driver;
		this.healthDetailsHandler = new Neo4jHealthDetailsHandler();
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) {
		try {
			try {
				runHealthCheckQuery(builder);
			}
			catch (SessionExpiredException ex) {
				// Retry one time when the session has been expired
				logger.warn(MESSAGE_SESSION_EXPIRED);
				runHealthCheckQuery(builder);
			}
		}
		catch (Exception ex) {
			builder.down().withException(ex);
		}
	}

	private void runHealthCheckQuery(Health.Builder builder) {
		// We use WRITE here to make sure UP is returned for a server that supports
		// all possible workloads
		try (Session session = this.driver.session(DEFAULT_SESSION_CONFIG)) {
			Result result = session.run(CYPHER);
			Record record = result.single();
			ResultSummary resultSummary = result.consume();
			this.healthDetailsHandler.addHealthDetails(builder, new Neo4jHealthDetails(record, resultSummary));
		}
	}

}
