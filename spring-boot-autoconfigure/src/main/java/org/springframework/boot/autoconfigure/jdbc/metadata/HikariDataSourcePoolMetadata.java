/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.jdbc.metadata;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;

import org.springframework.beans.DirectFieldAccessor;

/**
 * {@link DataSourcePoolMetadata} for a Hikari {@link DataSource}.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class HikariDataSourcePoolMetadata
		extends AbstractDataSourcePoolMetadata<HikariDataSource> {

	public HikariDataSourcePoolMetadata(HikariDataSource dataSource) {
		super(dataSource);
	}

	@Override
	public Integer getActive() {
		try {
			return getHikariPool().getActiveConnections();
		}
		catch (Exception ex) {
			return null;
		}
	}

	private HikariPool getHikariPool() {
		return (HikariPool) new DirectFieldAccessor(getDataSource())
				.getPropertyValue("pool");
	}

	@Override
	public Integer getMax() {
		return getDataSource().getMaximumPoolSize();
	}

	@Override
	public Integer getMin() {
		return getDataSource().getMinimumIdle();
	}

	@Override
	public String getValidationQuery() {
		return getDataSource().getConnectionTestQuery();
	}

}
