/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.postgres;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PostgresJdbcUrl}.
 *
 * @author Dmytro Nosan
 */
class PostgresJdbcUrlTests {

	@Test
	void shouldReturnUrlWithNoParameters() {
		String jdbcUrl = "jdbc:postgresql://localhost:30001/database";
		PostgresJdbcUrl postgresJdbcUrl = new PostgresJdbcUrl(jdbcUrl);
		assertThat(postgresJdbcUrl.toString()).isEqualTo(jdbcUrl);
	}

	@Test
	void shouldNotAddParameterWhenParameterExistsWithValue() {
		String jdbcUrl = "jdbc:postgresql://localhost:30001/database?ApplicationName=spring-boot&foo=bar";
		PostgresJdbcUrl postgresJdbcUrl = new PostgresJdbcUrl(jdbcUrl);
		postgresJdbcUrl.addParameterIfDoesNotExist("ApplicationName", "my-app");
		assertThat(postgresJdbcUrl.toString()).isEqualTo(jdbcUrl);
	}

	@Test
	void shouldNotAddParameterWhenParameterExistsWithNoValue() {
		String jdbcUrl = "jdbc:postgresql://localhost:30001/database?foo=bar&ApplicationName";
		PostgresJdbcUrl postgresJdbcUrl = new PostgresJdbcUrl(jdbcUrl);
		postgresJdbcUrl.addParameterIfDoesNotExist("ApplicationName", "my-app");
		assertThat(postgresJdbcUrl.toString()).isEqualTo(jdbcUrl);
	}

	@Test
	void shouldAddParameterWhenParameterDoesNotExist() {
		String jdbcUrl = "jdbc:postgresql://localhost:30001/database";
		PostgresJdbcUrl postgresJdbcUrl = new PostgresJdbcUrl(jdbcUrl);
		postgresJdbcUrl.addParameterIfDoesNotExist("ApplicationName", "my-app");
		assertThat(postgresJdbcUrl.toString()).isEqualTo(jdbcUrl + "?ApplicationName=my-app");
	}

	@Test
	void shouldAddParameterToOtherParametersWhenParameterDoesNotExist() {
		String jdbcUrl = "jdbc:postgresql://localhost:30001/database?foo=bar&foo1=&foo2&=foo3&=";
		PostgresJdbcUrl postgresJdbcUrl = new PostgresJdbcUrl(jdbcUrl);
		postgresJdbcUrl.addParameterIfDoesNotExist("ApplicationName", "my-app");
		assertThat(postgresJdbcUrl.toString()).isEqualTo(jdbcUrl + "&ApplicationName=my-app");
	}

}
