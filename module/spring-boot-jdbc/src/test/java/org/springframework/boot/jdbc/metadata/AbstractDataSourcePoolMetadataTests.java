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

import org.junit.jupiter.api.Test;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for {@link DataSourcePoolMetadata} tests.
 *
 * @param <D> the data source pool metadata type
 * @author Stephane Nicoll
 * @author Artsiom Yudovin
 */
abstract class AbstractDataSourcePoolMetadataTests<D extends AbstractDataSourcePoolMetadata<?>> {

	/**
	 * Return a data source metadata instance with a min size of 0 and max size of 2. Idle
	 * connections are not reclaimed immediately.
	 * @return the data source metadata
	 */
	protected abstract D getDataSourceMetadata();

	@Test
	void getMaxPoolSize() {
		assertThat(getDataSourceMetadata().getMax()).isEqualTo(2);
	}

	@Test
	void getMinPoolSize() {
		assertThat(getDataSourceMetadata().getMin()).isZero();
	}

	@Test
	void getPoolSizeNoConnection() {
		// Make sure the pool is initialized
		JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSourceMetadata().getDataSource());
		jdbcTemplate.execute((ConnectionCallback<Object>) (connection) -> new Object());
		assertThat(getDataSourceMetadata().getActive()).isZero();
		assertThat(getDataSourceMetadata().getUsage()).isZero();
	}

	@Test
	void getPoolSizeOneConnection() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSourceMetadata().getDataSource());
		jdbcTemplate.execute((ConnectionCallback<Object>) (connection) -> {
			assertThat(getDataSourceMetadata().getActive()).isOne();
			assertThat(getDataSourceMetadata().getUsage()).isEqualTo(0.5f);
			return new Object();
		});
	}

	@Test
	void getIdle() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSourceMetadata().getDataSource());
		jdbcTemplate.execute((ConnectionCallback<Object>) (connection) -> new Object());
		assertThat(getDataSourceMetadata().getIdle()).isOne();
	}

	@Test
	void getPoolSizeTwoConnections() {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSourceMetadata().getDataSource());
		jdbcTemplate.execute((ConnectionCallback<Object>) (connection) -> {
			jdbcTemplate.execute((ConnectionCallback<Object>) (connection1) -> {
				assertThat(getDataSourceMetadata().getActive()).isEqualTo(2);
				assertThat(getDataSourceMetadata().getUsage()).isOne();
				return new Object();
			});
			return new Object();
		});
	}

	@Test
	abstract void getValidationQuery() throws Exception;

	@Test
	abstract void getDefaultAutoCommit() throws Exception;

	protected DataSourceBuilder<?> initializeBuilder() {
		return DataSourceBuilder.create()
			.driverClassName("org.hsqldb.jdbc.JDBCDriver")
			.url("jdbc:hsqldb:mem:test")
			.username("sa");
	}

}
