/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.util.StringUtils;

import oracle.ucp.jdbc.PoolDataSource;

/**
 * {@link DataSourcePoolMetadata} for a Oracle UCP {@link DataSource}.
 *
 * @author Fabio Grassi
 * @since 2.3.4
 */
public class UcpDataSourcePoolMetadata extends AbstractDataSourcePoolMetadata<PoolDataSource> {

	public UcpDataSourcePoolMetadata(PoolDataSource dataSource) {
		super(dataSource);
	}

	@Override
	public Integer getActive() {
		try {
			return getDataSource().getBorrowedConnectionsCount();
		}
		catch (SQLException se) {
			throw new InvalidDataAccessApiUsageException("Error while reading property borrowedConnectionsCount", se);
		}
	}

	@Override
	public Integer getIdle() {
		try {
			return getDataSource().getAvailableConnectionsCount();
		}
		catch (SQLException se) {
			throw new InvalidDataAccessApiUsageException("Error while reading property availableConnectionsCount", se);
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
		String ac = getDataSource().getConnectionProperty("autoCommit");
		return StringUtils.hasText(ac) ? Boolean.valueOf(ac) : null;
	}

}
