/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.DataSourceInitializationMode;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DataSourceInitializer}.
 *
 * @author Stephane Nicoll
 */
class DataSourceInitializerTests {

	@Test
	void initializeEmbeddedByDefault() {
		try (HikariDataSource dataSource = createDataSource()) {
			DataSourceInitializer initializer = new DataSourceInitializer(dataSource, new DataSourceProperties(), null);
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			initializer.initializeDataSource();
			assertNumberOfRows(jdbcTemplate, 1);
		}
	}

	@Test
	void initializeWithModeAlways() {
		try (HikariDataSource dataSource = createDataSource()) {
			DataSourceProperties properties = new DataSourceProperties();
			properties.setInitializationMode(DataSourceInitializationMode.ALWAYS);
			DataSourceInitializer initializer = new DataSourceInitializer(dataSource, properties, null);
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			initializer.initializeDataSource();
			assertNumberOfRows(jdbcTemplate, 1);
		}
	}

	private void assertNumberOfRows(JdbcTemplate jdbcTemplate, int count) {
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) from BAR", Integer.class)).isEqualTo(count);
	}

	@Test
	void initializeWithModeNever() {
		try (HikariDataSource dataSource = createDataSource()) {
			DataSourceProperties properties = new DataSourceProperties();
			properties.setInitializationMode(DataSourceInitializationMode.NEVER);
			DataSourceInitializer initializer = new DataSourceInitializer(dataSource, properties, null);
			assertThat(initializer.initializeDataSource()).isFalse();
		}
	}

	@Test
	void initializeOnlyEmbeddedByDefault() throws SQLException {
		DatabaseMetaData metadata = mock(DatabaseMetaData.class);
		given(metadata.getDatabaseProductName()).willReturn("MySQL");
		Connection connection = mock(Connection.class);
		given(connection.getMetaData()).willReturn(metadata);
		DataSource dataSource = mock(DataSource.class);
		given(dataSource.getConnection()).willReturn(connection);
		DataSourceInitializer initializer = new DataSourceInitializer(dataSource, new DataSourceProperties(), null);
		assertThat(initializer.initializeDataSource()).isFalse();
		verify(dataSource, times(2)).getConnection();
	}

	private HikariDataSource createDataSource() {
		return DataSourceBuilder.create().type(HikariDataSource.class).url("jdbc:h2:mem:" + UUID.randomUUID()).build();
	}

}
