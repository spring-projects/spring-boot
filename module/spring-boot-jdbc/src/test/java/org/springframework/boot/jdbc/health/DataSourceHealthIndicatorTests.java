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

package org.springframework.boot.jdbc.health;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link DataSourceHealthIndicator}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class DataSourceHealthIndicatorTests {

	private final DataSourceHealthIndicator indicator = new DataSourceHealthIndicator();

	private SingleConnectionDataSource dataSource;

	@BeforeEach
	void init() {
		EmbeddedDatabaseConnection db = EmbeddedDatabaseConnection.HSQLDB;
		this.dataSource = new SingleConnectionDataSource(db.getUrl("testdb") + ";shutdown=true", "sa", "", false);
		String driverClassName = db.getDriverClassName();
		assertThat(driverClassName).isNotNull();
		this.dataSource.setDriverClassName(driverClassName);
	}

	@AfterEach
	void close() {
		if (this.dataSource != null) {
			this.dataSource.destroy();
		}
	}

	@Test
	void healthIndicatorWithDefaultSettings() {
		this.indicator.setDataSource(this.dataSource);
		Health health = this.indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsOnly(entry("database", "HSQL Database Engine"),
				entry("validationQuery", "isValid()"));
	}

	@Test
	void healthIndicatorWithCustomValidationQuery() {
		String customValidationQuery = "SELECT COUNT(*) from FOO";
		new JdbcTemplate(this.dataSource).execute("CREATE TABLE FOO (id INTEGER IDENTITY PRIMARY KEY)");
		this.indicator.setDataSource(this.dataSource);
		this.indicator.setQuery(customValidationQuery);
		Health health = this.indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsOnly(entry("database", "HSQL Database Engine"), entry("result", 0L),
				entry("validationQuery", customValidationQuery));
	}

	@Test
	void healthIndicatorWithInvalidValidationQuery() {
		String invalidValidationQuery = "SELECT COUNT(*) from BAR";
		this.indicator.setDataSource(this.dataSource);
		this.indicator.setQuery(invalidValidationQuery);
		Health health = this.indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).contains(entry("database", "HSQL Database Engine"),
				entry("validationQuery", invalidValidationQuery));
		assertThat(health.getDetails()).containsOnlyKeys("database", "error", "validationQuery");
	}

	@Test
	void healthIndicatorCloseConnection() throws Exception {
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		given(connection.getMetaData()).willReturn(this.dataSource.getConnection().getMetaData());
		given(dataSource.getConnection()).willReturn(connection);
		this.indicator.setDataSource(dataSource);
		Health health = this.indicator.health();
		assertThat(health.getDetails()).containsKey("database");
		then(connection).should(times(2)).close();
	}

	@Test
	void healthIndicatorWithConnectionValidationFailure() throws SQLException {
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		given(connection.isValid(0)).willReturn(false);
		given(connection.getMetaData()).willReturn(this.dataSource.getConnection().getMetaData());
		given(dataSource.getConnection()).willReturn(connection);
		this.indicator.setDataSource(dataSource);
		Health health = this.indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsOnly(entry("database", "HSQL Database Engine"),
				entry("validationQuery", "isValid()"));
	}

}
