/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.neo4j.health;

import org.neo4j.driver.Record;
import org.neo4j.driver.summary.ResultSummary;

/**
 * Health details for a Neo4j server.
 *
 * @author Andy Wilkinson
 */
class Neo4jHealthDetails {

	private final String version;

	private final String edition;

	private final ResultSummary summary;

	Neo4jHealthDetails(Record record, ResultSummary summary) {
		this.version = record.get("version").asString();
		this.edition = record.get("edition").asString();
		this.summary = summary;
	}

	String getVersion() {
		return this.version;
	}

	String getEdition() {
		return this.edition;
	}

	ResultSummary getSummary() {
		return this.summary;
	}

}
