/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.neo4j.health;

import org.neo4j.driver.summary.DatabaseInfo;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.ServerInfo;

import org.springframework.boot.health.contributor.Health;
import org.springframework.util.StringUtils;

/**
 * Handle health check details for a Neo4j server.
 *
 * @author Stephane Nicoll
 */
class Neo4jHealthDetailsHandler {

	/**
	 * Add health details for the specified {@link ResultSummary} and {@code edition}.
	 * @param builder the builder to use
	 * @param healthDetails the health details of the server
	 */
	void addHealthDetails(Health.Builder builder, Neo4jHealthDetails healthDetails) {
		ResultSummary summary = healthDetails.getSummary();
		ServerInfo serverInfo = summary.server();
		builder.up()
			.withDetail("server", healthDetails.getVersion() + "@" + serverInfo.address())
			.withDetail("edition", healthDetails.getEdition());
		DatabaseInfo databaseInfo = summary.database();
		if (StringUtils.hasText(databaseInfo.name())) {
			builder.withDetail("database", databaseInfo.name());
		}
	}

}
