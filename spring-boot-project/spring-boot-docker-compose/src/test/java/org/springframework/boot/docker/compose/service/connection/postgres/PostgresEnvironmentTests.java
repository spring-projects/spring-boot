/*
 * Copyright 2012-2023 the original author or authors.
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link PostgresEnvironment}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@Disabled
class PostgresEnvironmentTests {

	@Test
	void createWhenNoPostgresPasswordThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new PostgresEnvironment(Collections.emptyMap()))
			.withMessage("No POSTGRES_PASSWORD defined");
	}

	@Test
	void getUsernameWhenNoPostgresUser() {
		PostgresEnvironment environment = new PostgresEnvironment(Map.of("POSTGRES_PASSWORD", "secret"));
		assertThat(environment.getUsername()).isEqualTo("postgres");
	}

	@Test
	void getUsernameWhenHasPostgresUser() {
		PostgresEnvironment environment = new PostgresEnvironment(
				Map.of("POSTGRES_USER", "me", "POSTGRES_PASSWORD", "secret"));
		assertThat(environment.getUsername()).isEqualTo("me");
	}

	@Test
	void getPasswordWhenHasPostgresPassword() {
		PostgresEnvironment environment = new PostgresEnvironment(Map.of("POSTGRES_PASSWORD", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getDatabaseWhenNoPostgresDbOrPostgressUser() {
		PostgresEnvironment environment = new PostgresEnvironment(Map.of("POSTGRES_PASSWORD", "secret"));
		assertThat(environment.getDatabase()).isEqualTo("postgress");
	}

	@Test
	void getDatabaseWhenNoPostgresDbAndPostgressUser() {
		PostgresEnvironment environment = new PostgresEnvironment(
				Map.of("POSTGRES_USER", "me", "POSTGRES_PASSWORD", "secret"));
		assertThat(environment.getDatabase()).isEqualTo("me");
	}

	@Test
	void getDatabaseWhenHasPostgresDb() {
		PostgresEnvironment environment = new PostgresEnvironment(
				Map.of("POSTGRES_DB", "db", "POSTGRES_PASSWORD", "secret"));
		assertThat(environment.getDatabase()).isEqualTo("db");
	}

}
