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

package org.springframework.boot.docker.compose.service.connection.oracle;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link OracleEnvironment}.
 *
 * @author Andy Wilkinson
 */
class OracleEnvironmentTests {

	@Test
	void getUsernameWhenHasAppUser() {
		OracleEnvironment environment = new OracleEnvironment(
				Map.of("APP_USER", "alice", "APP_USER_PASSWORD", "secret"));
		assertThat(environment.getUsername()).isEqualTo("alice");
	}

	@Test
	void getUsernameWhenHasNoAppUser() {
		OracleEnvironment environment = new OracleEnvironment(Map.of("ORACLE_PASSWORD", "secret"));
		assertThat(environment.getUsername()).isEqualTo("system");
	}

	@Test
	void getPasswordWhenHasAppPassword() {
		OracleEnvironment environment = new OracleEnvironment(
				Map.of("APP_USER", "alice", "APP_USER_PASSWORD", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordWhenHasOraclePassword() {
		OracleEnvironment environment = new OracleEnvironment(Map.of("ORACLE_PASSWORD", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void createWhenRandomPasswordAndAppPasswordDoesNotThrow() {
		assertThatNoException().isThrownBy(() -> new OracleEnvironment(
				Map.of("APP_USER", "alice", "APP_USER_PASSWORD", "secret", "ORACLE_RANDOM_PASSWORD", "true")));
	}

	@Test
	void createWhenRandomPasswordThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new OracleEnvironment(Map.of("ORACLE_RANDOM_PASSWORD", "true")))
			.withMessage("ORACLE_RANDOM_PASSWORD is not supported without APP_USER and APP_USER_PASSWORD");
	}

	@Test
	void createWhenAppUserAndNoAppPasswordThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new OracleEnvironment(Map.of("APP_USER", "alice")))
			.withMessage("No Oracle app password found");
	}

	@Test
	void createWhenAppUserAndEmptyAppPasswordThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new OracleEnvironment(Map.of("APP_USER", "alice", "APP_USER_PASSWORD", "")))
			.withMessage("No Oracle app password found");
	}

	@Test
	void createWhenHasNoPasswordThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new OracleEnvironment(Collections.emptyMap()))
			.withMessage("No Oracle password found");
	}

	@Test
	void createWhenHasEmptyPasswordThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new OracleEnvironment(Map.of("ORACLE_PASSWORD", "")))
			.withMessage("No Oracle password found");
	}

	@Test
	void getDatabaseWhenHasNoOracleDatabase() {
		OracleEnvironment environment = new OracleEnvironment(Map.of("ORACLE_PASSWORD", "secret"));
		assertThat(environment.getDatabase()).isEqualTo("xepdb1");
	}

	@Test
	void getDatabaseWhenHasOracleDatabase() {
		OracleEnvironment environment = new OracleEnvironment(
				Map.of("ORACLE_PASSWORD", "secret", "ORACLE_DATABASE", "db"));
		assertThat(environment.getDatabase()).isEqualTo("db");
	}

}
