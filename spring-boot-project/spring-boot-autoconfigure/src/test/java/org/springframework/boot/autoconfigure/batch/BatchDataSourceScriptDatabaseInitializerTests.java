/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.batch;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BatchDataSourceScriptDatabaseInitializer}.
 *
 * @author Stephane Nicoll
 */
class BatchDataSourceScriptDatabaseInitializerTests {

	@Test
	void getSettingsWithPlatformDoesNotTouchDataSource() {
		DataSource dataSource = mock(DataSource.class);
		BatchProperties properties = new BatchProperties();
		properties.getJdbc().setPlatform("test");
		DatabaseInitializationSettings settings = BatchDataSourceScriptDatabaseInitializer.getSettings(dataSource,
				properties.getJdbc());
		assertThat(settings.getSchemaLocations())
				.containsOnly("classpath:org/springframework/batch/core/schema-test.sql");
		then(dataSource).shouldHaveNoInteractions();
	}

	@ParameterizedTest
	@EnumSource(value = DatabaseDriver.class, mode = Mode.EXCLUDE,
			names = { "FIREBIRD", "INFORMIX", "JTDS", "PHOENIX", "REDSHIFT", "TERADATA", "TESTCONTAINERS", "UNKNOWN" })
	void batchSchemaCanBeLocated(DatabaseDriver driver) throws SQLException {
		DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
		BatchProperties properties = new BatchProperties();
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		given(dataSource.getConnection()).willReturn(connection);
		DatabaseMetaData metadata = mock(DatabaseMetaData.class);
		given(connection.getMetaData()).willReturn(metadata);
		String productName = (String) ReflectionTestUtils.getField(driver, "productName");
		given(metadata.getDatabaseProductName()).willReturn(productName);
		DatabaseInitializationSettings settings = BatchDataSourceScriptDatabaseInitializer.getSettings(dataSource,
				properties.getJdbc());
		List<String> schemaLocations = settings.getSchemaLocations();
		assertThat(schemaLocations)
				.allSatisfy((location) -> assertThat(resourceLoader.getResource(location).exists()).isTrue());
	}

	@Test
	void batchHasExpectedBuiltInSchemas() throws IOException {
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		List<String> schemaNames = Stream
				.of(resolver.getResources("classpath:org/springframework/batch/core/schema-*.sql"))
				.map((resource) -> resource.getFilename()).filter((resourceName) -> !resourceName.contains("-drop-"))
				.collect(Collectors.toList());
		assertThat(schemaNames).containsExactlyInAnyOrder("schema-derby.sql", "schema-sqlserver.sql",
				"schema-mysql.sql", "schema-sqlite.sql", "schema-postgresql.sql", "schema-hana.sql",
				"schema-oracle.sql", "schema-db2.sql", "schema-hsqldb.sql", "schema-sybase.sql", "schema-h2.sql");
	}

}
