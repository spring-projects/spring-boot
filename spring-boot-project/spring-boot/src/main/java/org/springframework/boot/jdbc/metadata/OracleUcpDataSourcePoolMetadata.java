/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.jdbc.metadata;

import java.sql.SQLException;

import javax.sql.DataSource;

import oracle.ucp.jdbc.PoolDataSource;

import org.springframework.util.StringUtils;

/**
 * {@link DataSourcePoolMetadata} for an Oracle UCP {@link DataSource}.
 *
 * @author Fabio Grassi
 * @since 2.4.0
 */
public class OracleUcpDataSourcePoolMetadata extends AbstractDataSourcePoolMetadata<PoolDataSource> {

	/**
	 * Constructs a new OracleUcpDataSourcePoolMetadata object with the specified
	 * PoolDataSource.
	 * @param dataSource the PoolDataSource object to be used for creating the
	 * OracleUcpDataSourcePoolMetadata
	 */
	public OracleUcpDataSourcePoolMetadata(PoolDataSource dataSource) {
		super(dataSource);
	}

	/**
	 * Returns the number of active connections in the Oracle UCP data source pool.
	 * @return the number of active connections
	 * @throws SQLException if an error occurs while retrieving the number of active
	 * connections
	 */
	@Override
	public Integer getActive() {
		try {
			return getDataSource().getBorrowedConnectionsCount();
		}
		catch (SQLException ex) {
			return null;
		}
	}

	/**
	 * Returns the number of idle connections in the Oracle UCP data source pool.
	 * @return the number of idle connections
	 * @throws SQLException if an error occurs while retrieving the number of idle
	 * connections
	 */
	@Override
	public Integer getIdle() {
		try {
			return getDataSource().getAvailableConnectionsCount();
		}
		catch (SQLException ex) {
			return null;
		}
	}

	/**
	 * Returns the maximum pool size of the data source.
	 * @return the maximum pool size of the data source
	 */
	@Override
	public Integer getMax() {
		return getDataSource().getMaxPoolSize();
	}

	/**
	 * Returns the minimum pool size of the Oracle UCP data source.
	 * @return the minimum pool size of the Oracle UCP data source
	 */
	@Override
	public Integer getMin() {
		return getDataSource().getMinPoolSize();
	}

	/**
	 * Returns the validation query for the Oracle UCP data source.
	 * @return the validation query for the Oracle UCP data source
	 */
	@Override
	public String getValidationQuery() {
		return getDataSource().getSQLForValidateConnection();
	}

	/**
	 * Retrieves the default value for the autoCommit property of the underlying data
	 * source.
	 * @return the default value for the autoCommit property, or null if not set
	 */
	@Override
	public Boolean getDefaultAutoCommit() {
		String autoCommit = getDataSource().getConnectionProperty("autoCommit");
		return StringUtils.hasText(autoCommit) ? Boolean.valueOf(autoCommit) : null;
	}

}
