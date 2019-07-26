/*
 * Copyright 2012-2019 the original author or authors.
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
import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.exceptions.SessionExpiredException;
import org.neo4j.driver.v1.summary.ResultSummary;
import org.neo4j.driver.v1.summary.ServerInfo;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * {@link HealthIndicator} that tests the status of Neo4j by executing a Cypher statement
 * and extracting server and database information. This health indicator uses the native
 * bolt connection and an optimized query to check for a servers health in contrast to
 * {@link Neo4jHealthIndicator}.
 *
 * @author Michael J. Simons
 * @since 2.2.0
 */
public final class Neo4jNativeHealthIndicator extends AbstractHealthIndicator {

	private static final Log logger = LogFactory.getLog(Neo4jHealthIndicator.class);

	/**
	 * The Cypher statement used to verify Neo4j is up.
	 */
	static final String CYPHER = "RETURN 1 AS result";

	/**
	 * Message indicating that the health check failed.
	 */
	static final String MESSAGE_HEALTH_CHECK_FAILED = "Neo4j health check failed";

	/**
	 * Message logged before retrying a health check.
	 */
	static final String MESSAGE_SESSION_EXPIRED = "Neo4j session has expired, retrying one single time to retrieve server health.";

	/**
	 * The driver for this health indicator instance.
	 */
	private final Driver driver;

	public Neo4jNativeHealthIndicator(Driver driver) {
		super(MESSAGE_HEALTH_CHECK_FAILED);
		this.driver = driver;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) {

		try {
			ResultSummary resultSummary;
			// Retry one time when the session has been expired
			try {
				resultSummary = runHealthCheckQuery();
			}
			catch (SessionExpiredException sessionExpiredException) {
				logger.warn(MESSAGE_SESSION_EXPIRED);
				resultSummary = runHealthCheckQuery();
			}
			buildStatusUp(resultSummary, builder);
		}
		catch (Exception ex) {
			builder.down().withException(ex);
		}
	}

	/**
	 * Applies the given {@link ResultSummary} to the {@link Health.Builder builder}
	 * without actually calling {@code build}.
	 * @param resultSummary the result summary returned by the server
	 * @param builder the health builder to be modified
	 * @return the modified health builder
	 */
	static Health.Builder buildStatusUp(ResultSummary resultSummary, Health.Builder builder) {

		ServerInfo serverInfo = resultSummary.server();
		builder.up().withDetail("server", serverInfo.version() + "@" + serverInfo.address());

		return builder;
	}

	ResultSummary runHealthCheckQuery() {
		// We use WRITE here to make sure UP is returned for a server that supports
		// all possible workloads
		try (Session session = this.driver.session(AccessMode.WRITE)) {
			ResultSummary resultSummary = session.run(CYPHER).consume();
			return resultSummary;
		}
	}

}
