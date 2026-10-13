/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.jdbc.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Data JDBC.
 *
 * @author Jens Schauder
 * @since 4.0.0
 */
@ConfigurationProperties("spring.data.jdbc")
public class DataJdbcProperties {

	/**
	 * Dialect to use. By default, the dialect is determined by inspecting the database
	 * connection.
	 */
	private @Nullable DataJdbcDatabaseDialect dialect;

	public @Nullable DataJdbcDatabaseDialect getDialect() {
		return this.dialect;
	}

	public void setDialect(@Nullable DataJdbcDatabaseDialect dialect) {
		this.dialect = dialect;
	}

}
