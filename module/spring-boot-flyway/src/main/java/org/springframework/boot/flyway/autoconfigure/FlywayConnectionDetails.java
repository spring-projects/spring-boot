/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.flyway.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.jdbc.DatabaseDriver;

/**
 * Details required for Flyway to establish a connection to a database.
 *
 * @author Andy Wilkinson
 * @author YeongJae Min
 * @since 4.0.0
 */
public interface FlywayConnectionDetails extends ConnectionDetails {

	/**
	 * Username for the database or {@code null} if no Flyway-specific configuration is
	 * required.
	 * @return the username for the database or {@code null}
	 */
	@Nullable String getUsername();

	/**
	 * Password for the database or {@code null} if no Flyway-specific configuration is
	 * required.
	 * @return the password for the database or {@code null}
	 */
	@Nullable String getPassword();

	/**
	 * URL for the database or {@code null} if no Flyway-specific configuration is
	 * required. Defaults to the JDBC URL. Implementations for non-JDBC connections should
	 * override this method and return {@code null} from {@link #getJdbcUrl()}.
	 * @return the URL for the database or {@code null}
	 * @since 4.2.0
	 */
	default @Nullable String getUrl() {
		return getJdbcUrl();
	}

	/**
	 * JDBC URL for the database or {@code null} if no Flyway-specific configuration is
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
