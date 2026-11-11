/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.service.connection;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

public interface DatabaseConnectionDetails extends ConnectionDetails {

	String getJdbcUrl();

}
