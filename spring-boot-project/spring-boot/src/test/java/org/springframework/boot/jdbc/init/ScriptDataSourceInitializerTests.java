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

import java.util.Arrays;
import java.util.UUID;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ScriptDataSourceInitializer}.
 *
 * @author Andy Wilkinson
 */
class ScriptDataSourceInitializerTests {

	private final HikariDataSource dataSource = DataSourceBuilder.create().type(HikariDataSource.class)
			.url("jdbc:h2:mem:" + UUID.randomUUID()).build();

	@AfterEach
	void closeDataSource() {
		this.dataSource.close();
	}

	@Test
	void whenDatabaseIsInitializedThenSchemaAndDataScriptsAreApplied() {
		DataSourceInitializationSettings settings = new DataSourceInitializationSettings();
		settings.setSchemaLocations(Arrays.asList("schema.sql"));
		settings.setDataLocations(Arrays.asList("data.sql"));
		ScriptDataSourceInitializer initializer = createInitializer(settings);
		assertThat(initializer.initializeDatabase()).isTrue();
		assertThat(numberOfRows("SELECT COUNT(*) FROM EXAMPLE")).isEqualTo(1);
	}

	@Test
	void whenContinueOnErrorIsFalseThenInitializationFailsOnError() {
		DataSourceInitializationSettings settings = new DataSourceInitializationSettings();
		settings.setDataLocations(Arrays.asList("data.sql"));
		ScriptDataSourceInitializer initializer = createInitializer(settings);
		assertThatExceptionOfType(DataAccessException.class).isThrownBy(() -> initializer.initializeDatabase());
	}

	@Test
	void whenContinueOnErrorIsTrueThenInitializationDoesNotFailOnError() {
		DataSourceInitializationSettings settings = new DataSourceInitializationSettings();
		settings.setContinueOnError(true);
		settings.setDataLocations(Arrays.asList("data.sql"));
		ScriptDataSourceInitializer initializer = createInitializer(settings);
		assertThat(initializer.initializeDatabase()).isTrue();
	}

	private ScriptDataSourceInitializer createInitializer(DataSourceInitializationSettings settings) {
		return new ScriptDataSourceInitializer(this.dataSource, settings);
	}

	private int numberOfRows(String sql) {
		return new JdbcTemplate(this.dataSource).queryForObject(sql, Integer.class);
	}

}
