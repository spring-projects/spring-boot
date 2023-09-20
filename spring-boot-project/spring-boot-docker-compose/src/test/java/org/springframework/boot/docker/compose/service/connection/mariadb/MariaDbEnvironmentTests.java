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

package org.springframework.boot.docker.compose.service.connection.mariadb;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link MariaDbEnvironment}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Jinseong Hwang
 */
class MariaDbEnvironmentTests {

	@Test
	void createWhenHasMariaDbRandomRootPasswordThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new MariaDbEnvironment(Map.of("MARIADB_RANDOM_ROOT_PASSWORD", "true")))
			.withMessage("MARIADB_RANDOM_ROOT_PASSWORD is not supported");
	}

	@Test
	void createWhenHasMySqlRandomRootPasswordThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new MariaDbEnvironment(Map.of("MYSQL_RANDOM_ROOT_PASSWORD", "true")))
			.withMessage("MYSQL_RANDOM_ROOT_PASSWORD is not supported");
	}

	@Test
	void createWhenHasMariaDbRootPasswordHashThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new MariaDbEnvironment(Map.of("MARIADB_ROOT_PASSWORD_HASH", "0FF")))
			.withMessage("MARIADB_ROOT_PASSWORD_HASH is not supported");
	}

	@Test
	void createWhenHasNoPasswordThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new MariaDbEnvironment(Collections.emptyMap()))
			.withMessage("No MariaDB password found");
	}

	@Test
	void createWhenHasNoDatabaseThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new MariaDbEnvironment(Map.of("MARIADB_PASSWORD", "secret")))
			.withMessage("No MARIADB_DATABASE defined");
	}

	@Test
	void getUsernameWhenHasMariaDbUser() {
		MariaDbEnvironment environment = new MariaDbEnvironment(
				Map.of("MARIADB_USER", "myself", "MARIADB_PASSWORD", "secret", "MARIADB_DATABASE", "db"));
		assertThat(environment.getUsername()).isEqualTo("myself");
	}

	@Test
	void getUsernameWhenHasMySqlUser() {
		MariaDbEnvironment environment = new MariaDbEnvironment(
				Map.of("MYSQL_USER", "myself", "MARIADB_PASSWORD", "secret", "MARIADB_DATABASE", "db"));
		assertThat(environment.getUsername()).isEqualTo("myself");
	}

	@Test
	void getUsernameWhenHasMariaDbUserAndMySqlUser() {
		MariaDbEnvironment environment = new MariaDbEnvironment(Map.of("MARIADB_USER", "myself", "MYSQL_USER", "me",
				"MARIADB_PASSWORD", "secret", "MARIADB_DATABASE", "db"));
		assertThat(environment.getUsername()).isEqualTo("myself");
	}

	@Test
	void getUsernameWhenHasNoMariaDbUserOrMySqlUser() {
		MariaDbEnvironment environment = new MariaDbEnvironment(
				Map.of("MARIADB_PASSWORD", "secret", "MARIADB_DATABASE", "db"));
		assertThat(environment.getUsername()).isEqualTo("root");
	}

	@Test
	void getPasswordWhenHasMariaDbPassword() {
		MariaDbEnvironment environment = new MariaDbEnvironment(
				Map.of("MARIADB_PASSWORD", "secret", "MARIADB_DATABASE", "db"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordWhenHasMysqlPassword() {
		MariaDbEnvironment environment = new MariaDbEnvironment(
				Map.of("MYSQL_PASSWORD", "secret", "MARIADB_DATABASE", "db"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordWhenHasMysqlRootPassword() {
		MariaDbEnvironment environment = new MariaDbEnvironment(
				Map.of("MYSQL_ROOT_PASSWORD", "secret", "MARIADB_DATABASE", "db"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordWhenHasMariaDbPasswordAndMysqlPassword() {
		MariaDbEnvironment environment = new MariaDbEnvironment(
				Map.of("MARIADB_PASSWORD", "secret", "MYSQL_PASSWORD", "donttell", "MARIADB_DATABASE", "db"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordWhenHasMariaDbPasswordAndMysqlRootPassword() {
		MariaDbEnvironment environment = new MariaDbEnvironment(
				Map.of("MARIADB_PASSWORD", "secret", "MYSQL_ROOT_PASSWORD", "donttell", "MARIADB_DATABASE", "db"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordWhenHasNoPasswordAndMariaDbAllowEmptyPassword() {
		MariaDbEnvironment environment = new MariaDbEnvironment(
				Map.of("MARIADB_ALLOW_EMPTY_PASSWORD", "true", "MARIADB_DATABASE", "db"));
		assertThat(environment.getPassword()).isEmpty();
	}

	@Test
	void getPasswordWhenHasNoPasswordAndMySqlAllowEmptyPassword() {
		MariaDbEnvironment environment = new MariaDbEnvironment(
				Map.of("MYSQL_ALLOW_EMPTY_PASSWORD", "true", "MARIADB_DATABASE", "db"));
		assertThat(environment.getPassword()).isEmpty();
	}

	@Test
	void getDatabaseWhenHasMariaDbDatabase() {
		MariaDbEnvironment environment = new MariaDbEnvironment(
				Map.of("MARIADB_ALLOW_EMPTY_PASSWORD", "true", "MARIADB_DATABASE", "db"));
		assertThat(environment.getDatabase()).isEqualTo("db");
	}

	@Test
	void getDatabaseWhenHasMySqlDatabase() {
		MariaDbEnvironment environment = new MariaDbEnvironment(
				Map.of("MARIADB_ALLOW_EMPTY_PASSWORD", "true", "MYSQL_DATABASE", "db"));
		assertThat(environment.getDatabase()).isEqualTo("db");
	}

	@Test
	void getDatabaseWhenHasMariaDbAndMySqlDatabase() {
		MariaDbEnvironment environment = new MariaDbEnvironment(
				Map.of("MARIADB_ALLOW_EMPTY_PASSWORD", "true", "MARIADB_DATABASE", "db", "MYSQL_DATABASE", "otherdb"));
		assertThat(environment.getDatabase()).isEqualTo("db");
	}

}
