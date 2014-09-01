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

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.Assert.assertEquals;

/**
 * Abstract base class for {@link DataSourceMetadata} tests.
 *
 * @author Stephane Nicoll
 */
public abstract class AbstractDataSourceMetadataTests<D extends AbstractDataSourceMetadata<?>> {

	/**
	 * Return a data source metadata instance with a min size of 0 and max size of 2.
	 */
	protected abstract D getDataSourceMetadata();

	@Test
	public void getMaxPoolSize() {
		assertEquals(Integer.valueOf(2), getDataSourceMetadata().getMaxPoolSize());
	}

	@Test
	public void getMinPoolSize() {
		assertEquals(Integer.valueOf(0), getDataSourceMetadata().getMinPoolSize());
	}

	@Test
	public void getPoolSizeNoConnection() {
		// Make sure the pool is initialized
		JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSourceMetadata()
				.getDataSource());
		jdbcTemplate.execute(new ConnectionCallback<Void>() {
			@Override
			public Void doInConnection(Connection connection) throws SQLException,
					DataAccessException {
				return null;
			}
		});
		assertEquals(Integer.valueOf(0), getDataSourceMetadata().getPoolSize());
		assertEquals(Float.valueOf(0), getDataSourceMetadata().getPoolUsage());
	}

	@Test
	public void getPoolSizeOneConnection() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSourceMetadata()
				.getDataSource());
		jdbcTemplate.execute(new ConnectionCallback<Void>() {
			@Override
			public Void doInConnection(Connection connection) throws SQLException,
					DataAccessException {
				assertEquals(Integer.valueOf(1), getDataSourceMetadata().getPoolSize());
				assertEquals(Float.valueOf(0.5F), getDataSourceMetadata().getPoolUsage());
				return null;
			}
		});
	}

	@Test
	public void getPoolSizeTwoConnections() {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSourceMetadata()
				.getDataSource());
		jdbcTemplate.execute(new ConnectionCallback<Void>() {
			@Override
			public Void doInConnection(Connection connection) throws SQLException,
					DataAccessException {
				jdbcTemplate.execute(new ConnectionCallback<Void>() {
					@Override
					public Void doInConnection(Connection connection)
							throws SQLException, DataAccessException {
						assertEquals(Integer.valueOf(2), getDataSourceMetadata()
								.getPoolSize());
						assertEquals(Float.valueOf(1F), getDataSourceMetadata()
								.getPoolUsage());
						return null;
					}
				});
				return null;
			}
		});
	}

	@Test
	public abstract void getValidationQuery();

	protected DataSourceBuilder initializeBuilder() {
		return DataSourceBuilder.create().driverClassName("org.hsqldb.jdbc.JDBCDriver")
				.url("jdbc:hsqldb:mem:test").username("sa");
	}

}
