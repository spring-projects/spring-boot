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

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;

/**
 * {@link DataSourcePoolMetadata} for an Apache Commons DBCP2 {@link DataSource}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class CommonsDbcp2DataSourcePoolMetadata extends AbstractDataSourcePoolMetadata<BasicDataSource> {

	/**
	 * Constructs a new CommonsDbcp2DataSourcePoolMetadata object with the specified
	 * BasicDataSource.
	 * @param dataSource the BasicDataSource object to be used for creating the pool
	 * metadata
	 */
	public CommonsDbcp2DataSourcePoolMetadata(BasicDataSource dataSource) {
		super(dataSource);
	}

	/**
	 * Returns the number of active connections in the data source.
	 * @return the number of active connections
	 */
	@Override
	public Integer getActive() {
		return getDataSource().getNumActive();
	}

	/**
	 * Returns the number of idle connections in the data source pool.
	 * @return the number of idle connections
	 */
	@Override
	public Integer getIdle() {
		return getDataSource().getNumIdle();
	}

	/**
	 * Returns the maximum number of active connections that can be allocated from this
	 * data source.
	 * @return the maximum number of active connections
	 */
	@Override
	public Integer getMax() {
		return getDataSource().getMaxTotal();
	}

	/**
	 * Returns the minimum number of idle connections that should be maintained in the
	 * connection pool.
	 * @return the minimum number of idle connections
	 */
	@Override
	public Integer getMin() {
		return getDataSource().getMinIdle();
	}

	/**
	 * Returns the validation query used by the data source.
	 * @return the validation query used by the data source
	 */
	@Override
	public String getValidationQuery() {
		return getDataSource().getValidationQuery();
	}

	/**
	 * Returns the default auto-commit behavior of the underlying data source.
	 * @return the default auto-commit behavior
	 */
	@Override
	public Boolean getDefaultAutoCommit() {
		return getDataSource().getDefaultAutoCommit();
	}

}
