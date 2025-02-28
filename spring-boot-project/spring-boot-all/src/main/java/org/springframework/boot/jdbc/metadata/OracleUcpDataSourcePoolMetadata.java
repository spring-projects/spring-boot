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

	public OracleUcpDataSourcePoolMetadata(PoolDataSource dataSource) {
		super(dataSource);
	}

	@Override
	public Integer getActive() {
		try {
			return getDataSource().getBorrowedConnectionsCount();
		}
		catch (SQLException ex) {
			return null;
		}
	}

	@Override
	public Integer getIdle() {
		try {
			return getDataSource().getAvailableConnectionsCount();
		}
		catch (SQLException ex) {
			return null;
		}
	}

	@Override
	public Integer getMax() {
		return getDataSource().getMaxPoolSize();
	}

	@Override
	public Integer getMin() {
		return getDataSource().getMinPoolSize();
	}

	@Override
	public String getValidationQuery() {
		return getDataSource().getSQLForValidateConnection();
	}

	@Override
	public Boolean getDefaultAutoCommit() {
		String autoCommit = getDataSource().getConnectionProperty("autoCommit");
		return StringUtils.hasText(autoCommit) ? Boolean.valueOf(autoCommit) : null;
	}

}
