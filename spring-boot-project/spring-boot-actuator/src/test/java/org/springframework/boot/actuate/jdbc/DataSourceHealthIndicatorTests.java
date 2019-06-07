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

package org.springframework.boot.actuate.jdbc;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DataSourceHealthIndicator}.
 *
 * @author Dave Syer
 */
public class DataSourceHealthIndicatorTests {

	private final DataSourceHealthIndicator indicator = new DataSourceHealthIndicator();

	private SingleConnectionDataSource dataSource;

	@Before
	public void init() {
		EmbeddedDatabaseConnection db = EmbeddedDatabaseConnection.HSQL;
		this.dataSource = new SingleConnectionDataSource(db.getUrl("testdb") + ";shutdown=true", "sa", "", false);
		this.dataSource.setDriverClassName(db.getDriverClassName());
	}

	@After
	public void close() {
		if (this.dataSource != null) {
			this.dataSource.destroy();
		}
	}

	@Test
	public void database() {
		this.indicator.setDataSource(this.dataSource);
		Health health = this.indicator.health();
		assertThat(health.getDetails().get("database")).isNotNull();
		assertThat(health.getDetails().get("hello")).isNotNull();
	}

	@Test
	public void customQuery() {
		this.indicator.setDataSource(this.dataSource);
		new JdbcTemplate(this.dataSource).execute("CREATE TABLE FOO (id INTEGER IDENTITY PRIMARY KEY)");
		this.indicator.setQuery("SELECT COUNT(*) from FOO");
		Health health = this.indicator.health();
		System.err.println(health);
		assertThat(health.getDetails().get("database")).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("hello")).isNotNull();
	}

	@Test
	public void error() {
		this.indicator.setDataSource(this.dataSource);
		this.indicator.setQuery("SELECT COUNT(*) from BAR");
		Health health = this.indicator.health();
		assertThat(health.getDetails().get("database")).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
	}

	@Test
	public void connectionClosed() throws Exception {
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		given(connection.getMetaData()).willReturn(this.dataSource.getConnection().getMetaData());
		given(dataSource.getConnection()).willReturn(connection);
		this.indicator.setDataSource(dataSource);
		Health health = this.indicator.health();
		assertThat(health.getDetails().get("database")).isNotNull();
		verify(connection, times(2)).close();
	}

}
