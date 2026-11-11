/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure;

import io.r2dbc.spi.ConnectionFactoryOptions;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Details required to establish a connection to an SQL service using R2DBC.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface R2dbcConnectionDetails extends ConnectionDetails {

	/**
	 * Connection factory options for connecting to the database.
	 * @return the connection factory options
	 */
	ConnectionFactoryOptions getConnectionFactoryOptions();

}
