/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.metadata;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.DirectFieldAccessor;

/**
 * {@link DataSourcePoolMetadata} for a Hikari {@link DataSource}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class HikariDataSourcePoolMetadata extends AbstractDataSourcePoolMetadata<HikariDataSource> {

	public HikariDataSourcePoolMetadata(HikariDataSource dataSource) {
		super(dataSource);
	}

	@Override
	public @Nullable Integer getActive() {
		try {
			HikariPool hikariPool = getHikariPool();
			return (hikariPool != null) ? hikariPool.getActiveConnections() : null;
		}
		catch (Exception ex) {
			return null;
		}
	}

	@Override
	public @Nullable Integer getIdle() {
		try {
			HikariPool hikariPool = getHikariPool();
			return (hikariPool != null) ? hikariPool.getIdleConnections() : null;
		}
		catch (Exception ex) {
			return null;
		}
	}

	private @Nullable HikariPool getHikariPool() {
		return (HikariPool) new DirectFieldAccessor(getDataSource()).getPropertyValue("pool");
	}

	@Override
	public @Nullable Integer getMax() {
		return getDataSource().getMaximumPoolSize();
	}

	@Override
	public @Nullable Integer getMin() {
		return getDataSource().getMinimumIdle();
	}

	@Override
	public @Nullable String getValidationQuery() {
		return getDataSource().getConnectionTestQuery();
	}

	@Override
	public @Nullable Boolean getDefaultAutoCommit() {
		return getDataSource().isAutoCommit();
	}

}
