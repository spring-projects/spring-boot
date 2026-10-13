/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.util.Assert;

/**
 * Details required to establish a connection to an SQL service using JDBC.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface JdbcConnectionDetails extends ConnectionDetails {

	/**
	 * Username for the database.
	 * @return the username for the database
	 */
	@Nullable String getUsername();

	/**
	 * Password for the database.
	 * @return the password for the database
	 */
	@Nullable String getPassword();

	/**
	 * JDBC url for the database.
	 * @return the JDBC url for the database
	 */
	String getJdbcUrl();

	/**
	 * The name of the JDBC driver class. Defaults to the class name of the driver
	 * specified in the JDBC URL.
	 * @return the JDBC driver class name
	 * @see #getJdbcUrl()
	 * @see DatabaseDriver#fromJdbcUrl(String)
	 * @see DatabaseDriver#getDriverClassName()
	 */
	default String getDriverClassName() {
		String driverClassName = DatabaseDriver.fromJdbcUrl(getJdbcUrl()).getDriverClassName();
		Assert.state(driverClassName != null, "'driverClassName' must not be null");
		return driverClassName;
	}

	/**
	 * Returns the name of the XA DataSource class. Defaults to the class name from the
	 * driver specified in the JDBC URL.
	 * @return the XA DataSource class name
	 * @see #getJdbcUrl()
	 * @see DatabaseDriver#fromJdbcUrl(String)
	 * @see DatabaseDriver#getXaDataSourceClassName()
	 */
	default @Nullable String getXaDataSourceClassName() {
		return DatabaseDriver.fromJdbcUrl(getJdbcUrl()).getXaDataSourceClassName();
	}

}
