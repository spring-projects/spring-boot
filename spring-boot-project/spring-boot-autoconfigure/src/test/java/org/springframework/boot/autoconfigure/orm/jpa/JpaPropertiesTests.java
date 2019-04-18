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

package org.springframework.boot.autoconfigure.orm.jpa;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.junit.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.vendor.Database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link JpaProperties}.
 *
 * @author Stephane Nicoll
 */
public class JpaPropertiesTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class);

	@Test
	public void determineDatabaseNoCheckIfDatabaseIsSet() {
		this.contextRunner.withPropertyValues("spring.jpa.database=postgresql")
				.run(assertJpaProperties((properties) -> {
					DataSource dataSource = mockStandaloneDataSource();
					Database database = properties.determineDatabase(dataSource);
					assertThat(database).isEqualTo(Database.POSTGRESQL);
					try {
						verify(dataSource, never()).getConnection();
					}
					catch (SQLException ex) {
						throw new IllegalStateException("Should not happen", ex);
					}
				}));
	}

	@Test
	public void determineDatabaseWithKnownUrl() {
		this.contextRunner.run(assertJpaProperties((properties) -> {
			Database database = properties
					.determineDatabase(mockDataSource("jdbc:h2:mem:testdb"));
			assertThat(database).isEqualTo(Database.H2);
		}));
	}

	@Test
	public void determineDatabaseWithKnownUrlAndUserConfig() {
		this.contextRunner.withPropertyValues("spring.jpa.database=mysql")
				.run(assertJpaProperties((properties) -> {
					Database database = properties
							.determineDatabase(mockDataSource("jdbc:h2:mem:testdb"));
					assertThat(database).isEqualTo(Database.MYSQL);
				}));
	}

	@Test
	public void determineDatabaseWithUnknownUrl() {
		this.contextRunner.run(assertJpaProperties((properties) -> {
			Database database = properties
					.determineDatabase(mockDataSource("jdbc:unknown://localhost"));
			assertThat(database).isEqualTo(Database.DEFAULT);
		}));
	}

	private DataSource mockStandaloneDataSource() {
		try {
			DataSource ds = mock(DataSource.class);
			given(ds.getConnection()).willThrow(SQLException.class);
			return ds;
		}
		catch (SQLException ex) {
			throw new IllegalStateException("Should not happen", ex);
		}
	}

	private DataSource mockDataSource(String jdbcUrl) {
		DataSource ds = mock(DataSource.class);
		try {
			DatabaseMetaData metadata = mock(DatabaseMetaData.class);
			given(metadata.getURL()).willReturn(jdbcUrl);
			Connection connection = mock(Connection.class);
			given(connection.getMetaData()).willReturn(metadata);
			given(ds.getConnection()).willReturn(connection);
		}
		catch (SQLException ex) {
			// Do nothing
		}
		return ds;
	}

	private ContextConsumer<AssertableApplicationContext> assertJpaProperties(
			Consumer<JpaProperties> consumer) {
		return (context) -> {
			assertThat(context).hasSingleBean(JpaProperties.class);
			consumer.accept(context.getBean(JpaProperties.class));
		};
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(JpaProperties.class)
	static class TestConfiguration {

	}

}
