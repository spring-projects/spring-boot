/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.metadata;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

/**
 * Provide a {@link DataSourcePoolMetadata} based on a {@link DataSource}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@FunctionalInterface
public interface DataSourcePoolMetadataProvider {

	/**
	 * Return the {@link DataSourcePoolMetadata} instance able to manage the specified
	 * {@link DataSource} or {@code null} if the given data source could not be handled.
	 * @param dataSource the data source
	 * @return the data source pool metadata
	 */
	@Nullable DataSourcePoolMetadata getDataSourcePoolMetadata(DataSource dataSource);

}
