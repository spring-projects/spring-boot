/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure;

/**
 * Exception thrown when R2DBC connection pooling has been configured both by the URL
 * ({@code spring.r2dbc.url}) and the pool properties ({@code spring.r2dbc.pool.*}.
 *
 * @author Andy Wilkinson
 */
class MultipleConnectionPoolConfigurationsException extends RuntimeException {

	MultipleConnectionPoolConfigurationsException() {
		super("R2DBC connection pooling configuration should be provided by either the spring.r2dbc.pool.* "
				+ "properties or the spring.r2dbc.url property but both have been used.");
	}

}
