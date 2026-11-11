/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.metadata;

import java.sql.SQLException;

import javax.sql.DataSource;

import oracle.ucp.jdbc.PoolDataSource;
import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;

/**
 * {@link DataSourcePoolMetadata} for an Oracle UCP {@link DataSource}.
 *
 * @author Fabio Grassi
 * @since 2.4.0
 */
public class OracleUcpDataSourcePoolMetadata extends AbstractDataSourcePoolMetadata<PoolDataSource> {

	public OracleUcpDataSourcePoolMetadata(PoolDataSource dataSource) {
		super(dataSource);
	}

	@Override
	public @Nullable Integer getActive() {
		try {
			return getDataSource().getBorrowedConnectionsCount();
		}
		catch (SQLException ex) {
			return null;
		}
	}

	@Override
	public @Nullable Integer getIdle() {
		try {
			return getDataSource().getAvailableConnectionsCount();
		}
		catch (SQLException ex) {
			return null;
		}
	}

	@Override
	public @Nullable Integer getMax() {
		return getDataSource().getMaxPoolSize();
	}

	@Override
	public @Nullable Integer getMin() {
		return getDataSource().getMinPoolSize();
	}

	@Override
	public @Nullable String getValidationQuery() {
		return getDataSource().getSQLForValidateConnection();
	}

	@Override
	public @Nullable Boolean getDefaultAutoCommit() {
		String autoCommit = getDataSource().getConnectionProperty("autoCommit");
		return StringUtils.hasText(autoCommit) ? Boolean.valueOf(autoCommit) : null;
	}

}
