/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * Container definitions for {@link LoadTimeWeaverAwareConsumerImportTestcontainersTests}.
 *
 * @author Andy Wilkinson
 */
interface LoadTimeWeaverAwareConsumerContainers {

	@Container
	@ServiceConnection
	PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16.1");

}
