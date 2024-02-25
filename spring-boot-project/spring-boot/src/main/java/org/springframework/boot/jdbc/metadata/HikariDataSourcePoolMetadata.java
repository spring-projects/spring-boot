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

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;

import org.springframework.beans.DirectFieldAccessor;

/**
 * {@link DataSourcePoolMetadata} for a Hikari {@link DataSource}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class HikariDataSourcePoolMetadata extends AbstractDataSourcePoolMetadata<HikariDataSource> {

	/**
     * Constructs a new HikariDataSourcePoolMetadata object with the specified HikariDataSource.
     * 
     * @param dataSource the HikariDataSource object to be used for constructing the pool metadata
     */
    public HikariDataSourcePoolMetadata(HikariDataSource dataSource) {
		super(dataSource);
	}

	/**
     * Returns the number of active connections in the Hikari connection pool.
     * 
     * @return the number of active connections, or null if an exception occurs
     */
    @Override
	public Integer getActive() {
		try {
			return getHikariPool().getActiveConnections();
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
     * Returns the number of idle connections in the HikariCP pool.
     * 
     * @return the number of idle connections, or null if an exception occurs
     */
    @Override
	public Integer getIdle() {
		try {
			return getHikariPool().getIdleConnections();
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
     * Retrieves the HikariPool object associated with this HikariDataSourcePoolMetadata instance.
     *
     * @return the HikariPool object associated with this HikariDataSourcePoolMetadata instance.
     */
    private HikariPool getHikariPool() {
		return (HikariPool) new DirectFieldAccessor(getDataSource()).getPropertyValue("pool");
	}

	/**
     * Returns the maximum pool size of the data source.
     * 
     * @return the maximum pool size of the data source
     */
    @Override
	public Integer getMax() {
		return getDataSource().getMaximumPoolSize();
	}

	/**
     * Returns the minimum number of idle connections in the HikariCP data source pool.
     *
     * @return the minimum number of idle connections
     */
    @Override
	public Integer getMin() {
		return getDataSource().getMinimumIdle();
	}

	/**
     * Returns the validation query for the Hikari data source.
     * 
     * @return the validation query
     */
    @Override
	public String getValidationQuery() {
		return getDataSource().getConnectionTestQuery();
	}

	/**
     * Returns the default auto-commit mode of the underlying data source.
     * 
     * @return the default auto-commit mode of the underlying data source
     */
    @Override
	public Boolean getDefaultAutoCommit() {
		return getDataSource().isAutoCommit();
	}

}
