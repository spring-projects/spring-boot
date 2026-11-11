/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.service.connection;

import org.testcontainers.containers.JdbcDatabaseContainer;

public class DatabaseContainerDatabaseConnectionDetails
		extends ContainerConnectionDetailsFactory<JdbcDatabaseContainer<?>, DatabaseConnectionDetails> {

	@Override
	protected DatabaseConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<JdbcDatabaseContainer<?>> source) {
		return new TestDatabaseConnectionDetails(source);
	}

}
