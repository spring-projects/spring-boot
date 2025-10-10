/*
 * Copyright 2012-present the original author or authors.
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
