/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc;

import javax.sql.DataSource;

/**
 * Strategy interface to determine the {@link SchemaManagement} of a {@link DataSource}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@FunctionalInterface
public interface SchemaManagementProvider {

	/**
	 * Return the {@link SchemaManagement} for the specified {@link DataSource}.
	 * @param dataSource the dataSource to handle
	 * @return the {@link SchemaManagement} for the {@link DataSource}.
	 */
	SchemaManagement getSchemaManagement(DataSource dataSource);

}
