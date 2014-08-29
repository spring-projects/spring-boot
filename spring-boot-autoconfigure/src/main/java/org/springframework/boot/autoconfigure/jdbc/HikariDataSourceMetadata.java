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

package org.springframework.boot.autoconfigure.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;

import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;

/**
 * A {@link DataSourceMetadata} implementation for the hikari
 * data source.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class HikariDataSourceMetadata extends AbstractDataSourceMetadata<HikariDataSource> {


	private final HikariPoolProvider hikariPoolProvider;

	public HikariDataSourceMetadata(HikariDataSource dataSource) {
		super(dataSource);
		this.hikariPoolProvider = new HikariPoolProvider(dataSource);
	}

	@Override
	public Integer getPoolSize() {
		HikariPool hikariPool = hikariPoolProvider.getHikariPool();
		if (hikariPool != null) {
			return hikariPool.getActiveConnections();
		}
		return null;
	}

	public Integer getMaxPoolSize() {
		return getDataSource().getMaximumPoolSize();
	}

	@Override
	public Integer getMinPoolSize() {
		return getDataSource().getMinimumIdle();
	}

	/**
	 * Provide the {@link HikariPool} instance managed internally by
	 * the {@link HikariDataSource} as there is no other way to retrieve
	 * that information except JMX access.
	 */
	private static class HikariPoolProvider {
		private final HikariDataSource dataSource;

		private boolean poolAvailable;

		private HikariPoolProvider(HikariDataSource dataSource) {
			this.dataSource = dataSource;
			this.poolAvailable = isHikariPoolAvailable();
		}

		public HikariPool getHikariPool() {
			if (!poolAvailable) {
				return null;
			}

			Object value = doGetValue();
			if (value instanceof HikariPool) {
				return (HikariPool) value;
			}
			return null;
		}

		private boolean isHikariPoolAvailable() {
			try {
				doGetValue();
				return true;
			}
			catch (BeansException e) { // No such field
				return false;
			}
			catch (SecurityException e) { // Security manager prevents to read the value
				return false;
			}
		}

		private Object doGetValue() {
			DirectFieldAccessor accessor = new DirectFieldAccessor(this.dataSource);
			return accessor.getPropertyValue("pool");
		}
	}

}
