/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.neo4j.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Data Neo4j.
 *
 * @author Michael J. Simons
 * @since 4.0.0
 */
@ConfigurationProperties("spring.data.neo4j")
public class DataNeo4jProperties {

	/**
	 * Database name to use. By default, the server decides the default database to use.
	 */
	private @Nullable String database;

	public @Nullable String getDatabase() {
		return this.database;
	}

	public void setDatabase(@Nullable String database) {
		this.database = database;
	}

}
