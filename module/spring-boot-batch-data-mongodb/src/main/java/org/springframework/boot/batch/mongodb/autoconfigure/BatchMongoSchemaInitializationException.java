/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.batch.mongodb.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.DataAccessException;

/**
 * Thrown when the Spring Batch repository schema cannot be initialized.
 *
 * @author Stephane Nicoll
 * @since 4.1.0
 */
public class BatchMongoSchemaInitializationException extends DataAccessException {

	public BatchMongoSchemaInitializationException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
