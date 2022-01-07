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

package org.springframework.boot.jdbc.init;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.jdbc.DatabaseDriver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PlatformPlaceholderDatabaseDriverResolver}
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
class PlatformPlaceholderDatabaseDriverResolverTests {

	@Test
	void resolveAllWithPlatformWhenThereAreNoValuesShouldReturnEmptyList() {
		assertThat(new PlatformPlaceholderDatabaseDriverResolver().resolveAll("test")).isEmpty();
	}

	@Test
	void resolveAllWithPlatformWhenValueDoesNotContainPlaceholderShouldReturnValueUnchanged() {
		assertThat(new PlatformPlaceholderDatabaseDriverResolver().resolveAll("test", "schema.sql"))
				.containsExactly("schema.sql");
	}

	@Test
	void resolveAllWithPlatformWhenValuesContainPlaceholdersShouldReturnValuesWithPlaceholdersReplaced() {
		assertThat(new PlatformPlaceholderDatabaseDriverResolver().resolveAll("postgresql", "schema.sql",
				"schema-@@platform@@.sql", "data-@@platform@@.sql")).containsExactly("schema.sql",
						"schema-postgresql.sql", "data-postgresql.sql");
	}

	@Test
	void resolveAllWithDataSourceWhenThereAreNoValuesShouldReturnEmptyList() {
		assertThat(new PlatformPlaceholderDatabaseDriverResolver().resolveAll(mock(DataSource.class))).isEmpty();
	}

	@Test
	void resolveAllWithDataSourceWhenValueDoesNotContainPlaceholderShouldReturnValueUnchanged() {
		assertThat(new PlatformPlaceholderDatabaseDriverResolver().resolveAll(mock(DataSource.class), "schema.sql"))
				.containsExactly("schema.sql");
	}

	@Test
	void resolveAllWithDataSourceWhenValuesContainPlaceholdersShouldReturnValuesWithPlaceholdersReplaced()
			throws SQLException {
		assertThat(new PlatformPlaceholderDatabaseDriverResolver().resolveAll(dataSourceWithProductName("PostgreSQL"),
				"schema.sql", "schema-@@platform@@.sql", "data-@@platform@@.sql")).containsExactly("schema.sql",
						"schema-postgresql.sql", "data-postgresql.sql");
	}

	@Test
	void resolveAllWithDataSourceWhenDriverMappingsAreCustomizedShouldResolvePlaceholderUsingCustomMapping()
			throws SQLException {
		assertThat(new PlatformPlaceholderDatabaseDriverResolver()
				.withDriverPlatform(DatabaseDriver.POSTGRESQL, "postgres")
				.resolveAll(dataSourceWithProductName("PostgreSQL"), "schema-@@platform@@.sql"))
						.containsExactly("schema-postgres.sql");
	}

	@Test
	void resolveAllWithDataSourceWhenValueIsAnEmptyStringShouldReturnValueUnchanged() {
		assertThat(new PlatformPlaceholderDatabaseDriverResolver().resolveAll(mock(DataSource.class), ""))
				.containsExactly("");
	}

	@Test
	void resolveAllWithDataSourceWhenDriverIsUnknownShouldThrow() {
		assertThatIllegalStateException().isThrownBy(() -> new PlatformPlaceholderDatabaseDriverResolver()
				.resolveAll(dataSourceWithProductName("CustomDB"), "schema-@@platform@@.sql"));
	}

	@Test
	void resolveAllWithDataSourceWhenPlaceholderIsCustomizedShouldResolvePlaceholders() throws SQLException {
		assertThat(new PlatformPlaceholderDatabaseDriverResolver("##platform##").resolveAll(
				dataSourceWithProductName("PostgreSQL"), "schema-##platform##.sql", "schema-@@platform@@.sql"))
						.containsExactly("schema-postgresql.sql", "schema-@@platform@@.sql");
	}

	private DataSource dataSourceWithProductName(String productName) throws SQLException {
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		given(dataSource.getConnection()).willReturn(connection);
		DatabaseMetaData metadata = mock(DatabaseMetaData.class);
		given(connection.getMetaData()).willReturn(metadata);
		given(metadata.getDatabaseProductName()).willReturn(productName);
		return dataSource;
	}

}
