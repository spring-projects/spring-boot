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

package org.springframework.boot.docker.compose.service.connection.mysql;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link MySqlEnvironment}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class MySqlEnvironmentTests {

	@Test
	void createWhenHasMysqlRandomRootPasswordThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new MySqlEnvironment(Map.of("MYSQL_RANDOM_ROOT_PASSWORD", "true")))
			.withMessage("MYSQL_RANDOM_ROOT_PASSWORD is not supported");
	}

	@Test
	void createWhenHasNoPasswordThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new MySqlEnvironment(Collections.emptyMap()))
			.withMessage("No MySQL password found");
	}

	@Test
	void createWhenHasNoDatabaseThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new MySqlEnvironment(Map.of("MYSQL_PASSWORD", "secret")))
			.withMessage("No MYSQL_DATABASE defined");
	}

	@Test
	void getUsernameWhenHasMySqlUser() {
		MySqlEnvironment environment = new MySqlEnvironment(
				Map.of("MYSQL_USER", "myself", "MYSQL_PASSWORD", "secret", "MYSQL_DATABASE", "db"));
		assertThat(environment.getUsername()).isEqualTo("myself");
	}

	@Test
	void getUsernameWhenHasNoMySqlUser() {
		MySqlEnvironment environment = new MySqlEnvironment(Map.of("MYSQL_PASSWORD", "secret", "MYSQL_DATABASE", "db"));
		assertThat(environment.getUsername()).isEqualTo("root");
	}

	@Test
	void getPasswordWhenHasMysqlPassword() {
		MySqlEnvironment environment = new MySqlEnvironment(Map.of("MYSQL_PASSWORD", "secret", "MYSQL_DATABASE", "db"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordWhenHasMysqlRootPassword() {
		MySqlEnvironment environment = new MySqlEnvironment(
				Map.of("MYSQL_ROOT_PASSWORD", "secret", "MYSQL_DATABASE", "db"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordWhenHasNoPasswordAndMysqlAllowEmptyPassword() {
		MySqlEnvironment environment = new MySqlEnvironment(
				Map.of("MYSQL_ALLOW_EMPTY_PASSWORD", "true", "MYSQL_DATABASE", "db"));
		assertThat(environment.getPassword()).isEmpty();
	}

	@Test
	void getDatabaseWhenHasMysqlDatabase() {
		MySqlEnvironment environment = new MySqlEnvironment(
				Map.of("MYSQL_ALLOW_EMPTY_PASSWORD", "true", "MYSQL_DATABASE", "db"));
		assertThat(environment.getDatabase()).isEqualTo("db");
	}

}
