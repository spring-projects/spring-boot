/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link PostgresEnvironment}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Sidmar Theodoro
 * @author He Zean
 */
class PostgresEnvironmentTests {

	@Test
	void createWhenNoPostgresPasswordThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new PostgresEnvironment(Collections.emptyMap()))
			.withMessage("No PostgreSQL password found");
	}

	@Test
	void getUsernameWhenNoPostgresUser() {
		PostgresEnvironment environment = new PostgresEnvironment(Map.of("POSTGRES_PASSWORD", "secret"));
		assertThat(environment.getUsername()).isEqualTo("postgres");
	}

	@Test
	void getUsernameWhenNoPostgresqlUser() {
		PostgresEnvironment environment = new PostgresEnvironment(Map.of("POSTGRESQL_PASSWORD", "secret"));
		assertThat(environment.getUsername()).isEqualTo("postgres");
	}

	@Test
	void getUsernameWhenHasPostgresUser() {
		PostgresEnvironment environment = new PostgresEnvironment(
				Map.of("POSTGRES_USER", "me", "POSTGRES_PASSWORD", "secret"));
		assertThat(environment.getUsername()).isEqualTo("me");
	}

	@Test
	void getUsernameWhenHasPostgresqlUser() {
		PostgresEnvironment environment = new PostgresEnvironment(
				Map.of("POSTGRESQL_USER", "me", "POSTGRESQL_PASSWORD", "secret"));
		assertThat(environment.getUsername()).isEqualTo("me");
	}

	@Test
	void getUsernameWhenHasPostgresqlUsername() {
		PostgresEnvironment environment = new PostgresEnvironment(
				Map.of("POSTGRESQL_USERNAME", "me", "POSTGRESQL_PASSWORD", "secret"));
		assertThat(environment.getUsername()).isEqualTo("me");
	}

	@Test
	void getPasswordWhenHasPostgresPassword() {
		PostgresEnvironment environment = new PostgresEnvironment(Map.of("POSTGRES_PASSWORD", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordWhenHasPostgresqlPassword() {
		PostgresEnvironment environment = new PostgresEnvironment(Map.of("POSTGRESQL_PASSWORD", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordWhenHasTrustHostAuthMethod() {
		PostgresEnvironment environment = new PostgresEnvironment(Map.of("POSTGRES_HOST_AUTH_METHOD", "trust"));
		assertThat(environment.getPassword()).isNull();
	}

	@Test
	void getPasswordWhenHasNoPasswordAndAllowEmptyPassword() {
		PostgresEnvironment environment = new PostgresEnvironment(Map.of("ALLOW_EMPTY_PASSWORD", "yes"));
		assertThat(environment.getPassword()).isEmpty();
	}

	@Test
	void getDatabaseWhenNoPostgresDbOrPostgresUser() {
		PostgresEnvironment environment = new PostgresEnvironment(Map.of("POSTGRES_PASSWORD", "secret"));
		assertThat(environment.getDatabase()).isEqualTo("postgres");
	}

	@Test
	void getDatabaseWhenNoPostgresqlDbOrPostgresUser() {
		PostgresEnvironment environment = new PostgresEnvironment(Map.of("POSTGRESQL_PASSWORD", "secret"));
		assertThat(environment.getDatabase()).isEqualTo("postgres");
	}

	@Test
	void getDatabaseWhenNoPostgresDbAndPostgresUser() {
		PostgresEnvironment environment = new PostgresEnvironment(
				Map.of("POSTGRES_USER", "me", "POSTGRES_PASSWORD", "secret"));
		assertThat(environment.getDatabase()).isEqualTo("me");
	}

	@Test
	void getDatabaseWhenNoPostgresqlDbAndPostgresUser() {
		PostgresEnvironment environment = new PostgresEnvironment(
				Map.of("POSTGRESQL_USER", "me", "POSTGRESQL_PASSWORD", "secret"));
		assertThat(environment.getDatabase()).isEqualTo("me");
	}

	@Test
	void getDatabaseWhenNoPostgresqlDatabaseAndPostgresqlUsername() {
		PostgresEnvironment environment = new PostgresEnvironment(
				Map.of("POSTGRESQL_USERNAME", "me", "POSTGRESQL_PASSWORD", "secret"));
		assertThat(environment.getDatabase()).isEqualTo("me");
	}

	@Test
	void getDatabaseWhenHasPostgresDb() {
		PostgresEnvironment environment = new PostgresEnvironment(
				Map.of("POSTGRES_DB", "db", "POSTGRES_PASSWORD", "secret"));
		assertThat(environment.getDatabase()).isEqualTo("db");
	}

	@Test
	void getDatabaseWhenHasPostgresqlDb() {
		PostgresEnvironment environment = new PostgresEnvironment(
				Map.of("POSTGRESQL_DB", "db", "POSTGRESQL_PASSWORD", "secret"));
		assertThat(environment.getDatabase()).isEqualTo("db");
	}

	@Test
	void getDatabaseWhenHasPostgresqlDatabase() {
		PostgresEnvironment environment = new PostgresEnvironment(
				Map.of("POSTGRESQL_DATABASE", "db", "POSTGRESQL_PASSWORD", "secret"));
		assertThat(environment.getDatabase()).isEqualTo("db");
	}

}
