/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.metadata;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.jspecify.annotations.Nullable;

/**
 * {@link DataSourcePoolMetadata} for an Apache Commons DBCP2 {@link DataSource}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class CommonsDbcp2DataSourcePoolMetadata extends AbstractDataSourcePoolMetadata<BasicDataSource> {

	public CommonsDbcp2DataSourcePoolMetadata(BasicDataSource dataSource) {
		super(dataSource);
	}

	@Override
	public @Nullable Integer getActive() {
		return getDataSource().getNumActive();
	}

	@Override
	public @Nullable Integer getIdle() {
		return getDataSource().getNumIdle();
	}

	@Override
	public @Nullable Integer getMax() {
		return getDataSource().getMaxTotal();
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
		return getDataSource().getDefaultAutoCommit();
	}

}
