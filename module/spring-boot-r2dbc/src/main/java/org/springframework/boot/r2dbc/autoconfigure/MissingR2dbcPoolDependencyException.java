/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure;

/**
 * Exception thrown when R2DBC connection pooling has been configured but the
 * {@code io.r2dbc:r2dbc-pool} dependency is missing.
 *
 * @author Andy Wilkinson
 */
class MissingR2dbcPoolDependencyException extends RuntimeException {

	MissingR2dbcPoolDependencyException() {
		super("R2DBC connection pooling has been configured but the io.r2dbc.pool.ConnectionPool class is not "
				+ "present.");
	}

}
