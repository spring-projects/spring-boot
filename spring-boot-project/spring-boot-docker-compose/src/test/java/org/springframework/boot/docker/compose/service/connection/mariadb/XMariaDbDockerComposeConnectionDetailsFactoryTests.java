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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link XMariaDbDockerComposeConnectionDetailsFactoryTests}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@Disabled
class XMariaDbDockerComposeConnectionDetailsFactoryTests {

	@Test
	void test() {
		fail("Not yet implemented");
	}

	// @formatter:off

	/*


	@Test
	void usernameUsesMariaDbVariables() {
		RunningService service = createService(Map.of("MARIADB_USER", "user-1"));
		MariaDbService mariaDbService = new MariaDbService(service);
		assertThat(mariaDbService.getUsername()).isEqualTo("user-1");
	}

	@Test
	void usernameUsesMysqlVariables() {
		RunningService service = createService(Map.of("MYSQL_USER", "user-1"));
		MariaDbService mariaDbService = new MariaDbService(service);
		assertThat(mariaDbService.getUsername()).isEqualTo("user-1");
	}

	@Test
	void usernameDefaultsToRoot() {
		RunningService service = createService(Collections.emptyMap());
		MariaDbService mariaDbService = new MariaDbService(service);
		assertThat(mariaDbService.getUsername()).isEqualTo("root");
	}

	@Test
	void passwordUsesMariaDbVariables() {
		RunningService service = createService(Map.of("MARIADB_PASSWORD", "password-1"));
		MariaDbService mariaDbService = new MariaDbService(service);
		assertThat(mariaDbService.getPassword()).isEqualTo("password-1");
	}

	@Test
	void passwordUsesMysqlVariables() {
		RunningService service = createService(Map.of("MYSQL_PASSWORD", "password-1"));
		MariaDbService mariaDbService = new MariaDbService(service);
		assertThat(mariaDbService.getPassword()).isEqualTo("password-1");
	}

	@Test
	void passwordUsesMariaDbRootVariables() {
		RunningService service = createService(Map.of("MARIADB_ROOT_PASSWORD", "root-password-1"));
		MariaDbService mariaDbService = new MariaDbService(service);
		assertThat(mariaDbService.getPassword()).isEqualTo("root-password-1");
	}

	@Test
	void passwordUsesMysqlRootVariables() {
		RunningService service = createService(Map.of("MYSQL_ROOT_PASSWORD", "root-password-1"));
		MariaDbService mariaDbService = new MariaDbService(service);
		assertThat(mariaDbService.getPassword()).isEqualTo("root-password-1");
	}

	@Test
	void passwordSupportsEmptyRootPasswordMariaDb() {
		RunningService service = createService(Map.of("MARIADB_ALLOW_EMPTY_ROOT_PASSWORD", ""));
		MariaDbService mariaDbService = new MariaDbService(service);
		assertThat(mariaDbService.getPassword()).isEqualTo("");
	}

	@Test
	void passwordDoesNotSupportRootPasswordHash() {
		RunningService service = createService(Map.of("MARIADB_ROOT_PASSWORD_HASH", "true"));
		MariaDbService mariaDbService = new MariaDbService(service);
		assertThatThrownBy(mariaDbService::getPassword).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("MARIADB_ROOT_PASSWORD_HASH");
	}

	@Test
	void passwordDoesNotSupportRandomRootPasswordMariaDb() {
		RunningService service = createService(Map.of("MARIADB_RANDOM_ROOT_PASSWORD", "true"));
		MariaDbService mariaDbService = new MariaDbService(service);
		assertThatThrownBy(mariaDbService::getPassword).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("MARIADB_RANDOM_ROOT_PASSWORD");
	}

	@Test
	void passwordDoesNotSupportRandomRootPasswordMysql() {
		RunningService service = createService(Map.of("MYSQL_RANDOM_ROOT_PASSWORD", "true"));
		MariaDbService mariaDbService = new MariaDbService(service);
		assertThatThrownBy(mariaDbService::getPassword).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("MYSQL_RANDOM_ROOT_PASSWORD");
	}

	@Test
	void passwordHasNoFallback() {
		RunningService service = createService(Collections.emptyMap());
		MariaDbService mariaDbService = new MariaDbService(service);
		assertThatThrownBy(mariaDbService::getPassword).isInstanceOf(IllegalStateException.class)
			.hasMessage("Can't find password for user");
	}

	@Test
	void passwordSupportsEmptyRootPasswordMysql() {
		RunningService service = createService(Map.of("MYSQL_ALLOW_EMPTY_PASSWORD", ""));
		MariaDbService mariaDbService = new MariaDbService(service);
		assertThat(mariaDbService.getPassword()).isEqualTo("");
	}

	@Test
	void databaseUsesMariaDbVariables() {
		RunningService service = createService(Map.of("MARIADB_DATABASE", "database-1"));
		MariaDbService mariaDbService = new MariaDbService(service);
		assertThat(mariaDbService.getDatabase()).isEqualTo("database-1");
	}

	@Test
	void databaseUsesMysqlVariables() {
		RunningService service = createService(Map.of("MYSQL_DATABASE", "database-1"));
		MariaDbService mariaDbService = new MariaDbService(service);
		assertThat(mariaDbService.getDatabase()).isEqualTo("database-1");
	}

	@Test
	void databaseHasNoFallback() {
		RunningService service = createService(Collections.emptyMap());
		MariaDbService mariaDbService = new MariaDbService(service);
		assertThatThrownBy(mariaDbService::getDatabase).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("MARIADB_DATABASE")
			.hasMessageContaining("MYSQL_DATABASE");
	}

	@Test
	void getPort() {
		RunningService service = createService(Collections.emptyMap());
		MariaDbService mariaDbService = new MariaDbService(service);
		assertThat(mariaDbService.getPort()).isEqualTo(33060);
	}

	@Test
	void matches() {
		assertThat(MariaDbService.matches(createService(Collections.emptyMap()))).isTrue();
		assertThat(MariaDbService.matches(createService(ImageReference.parse("redis:7.1"), Collections.emptyMap())))
			.isFalse();
	}

	private RunningService createService(Map<String, String> env) {
		return createService(ImageReference.parse("mariadb:10.10"), env);
	}

	private RunningService createService(ImageReference image, Map<String, String> env) {
		return RunningServiceBuilder.create("service-1", image).addTcpPort(3306, 33060).env(env).build();
	}



	 */

	// @formatter:on

}
