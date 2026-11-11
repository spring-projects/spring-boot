/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.metadata;

import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.jspecify.annotations.Nullable;

/**
 * {@link DataSourcePoolMetadata} for a Tomcat DataSource.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class TomcatDataSourcePoolMetadata extends AbstractDataSourcePoolMetadata<DataSource> {

	public TomcatDataSourcePoolMetadata(DataSource dataSource) {
		super(dataSource);
	}

	@Override
	public @Nullable Integer getActive() {
		ConnectionPool pool = getDataSource().getPool();
		return (pool != null) ? pool.getActive() : 0;
	}

	@Override
	public @Nullable Integer getIdle() {
		return getDataSource().getNumIdle();
	}

	@Override
	public @Nullable Integer getMax() {
		return getDataSource().getMaxActive();
	}

	@Override
	public @Nullable Integer getMin() {
		return getDataSource().getMinIdle();
	}

	@Override
	public @Nullable String getValidationQuery() {
		return getDataSource().getValidationQuery();
	}

	@Override
	public @Nullable Boolean getDefaultAutoCommit() {
		return getDataSource().isDefaultAutoCommit();
	}

}
