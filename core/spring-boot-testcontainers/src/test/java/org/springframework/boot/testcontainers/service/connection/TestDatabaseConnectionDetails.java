/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.service.connection;

import org.testcontainers.containers.JdbcDatabaseContainer;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory.ContainerConnectionDetails;

class TestDatabaseConnectionDetails extends ContainerConnectionDetails<JdbcDatabaseContainer<?>>
		implements DatabaseConnectionDetails {

	TestDatabaseConnectionDetails(ContainerConnectionSource<JdbcDatabaseContainer<?>> source) {
		super(source);
	}

	@Override
	public String getJdbcUrl() {
		return getContainer().getJdbcUrl();
	}

	JdbcDatabaseContainer<?> callGetContainer() {
		return super.getContainer();
	}

}
