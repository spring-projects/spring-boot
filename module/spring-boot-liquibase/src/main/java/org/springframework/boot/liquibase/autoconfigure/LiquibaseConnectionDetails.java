/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.liquibase.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.jdbc.DatabaseDriver;

/**
 * Details required for Liquibase to establish a connection to an SQL service using JDBC.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public interface LiquibaseConnectionDetails extends ConnectionDetails {

	/**
	 * Username for the database or {@code null} if no Liquibase-specific configuration is
	 * required.
	 * @return the username for the database or {@code null}
	 */
	@Nullable String getUsername();

	/**
	 * Password for the database or {@code null} if no Liquibase-specific configuration is
	 * required.
	 * @return the password for the database or {@code null}
	 */
	@Nullable String getPassword();

	/**
	 * JDBC URL for the database or {@code null} if no Liquibase-specific configuration is
	 * required.
	 * @return the JDBC URL for the database or {@code null}
	 */
	@Nullable String getJdbcUrl();

	/**
	 * The name of the JDBC driver class. Defaults to the class name of the driver
	 * specified in the JDBC URL or {@code null} when no JDBC URL is configured.
	 * @return the JDBC driver class name or {@code null}
	 * @see #getJdbcUrl()
	 * @see DatabaseDriver#fromJdbcUrl(String)
	 * @see DatabaseDriver#getDriverClassName()
	 */
	default @Nullable String getDriverClassName() {
		String jdbcUrl = getJdbcUrl();
		return (jdbcUrl != null) ? DatabaseDriver.fromJdbcUrl(jdbcUrl).getDriverClassName() : null;
	}

}
