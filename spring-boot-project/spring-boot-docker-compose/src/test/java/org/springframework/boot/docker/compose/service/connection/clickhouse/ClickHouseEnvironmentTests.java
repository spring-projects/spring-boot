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

package org.springframework.boot.docker.compose.service.connection.clickhouse;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ClickHouseEnvironment}.
 *
 * @author Stephane Nicoll
 */
class ClickHouseEnvironmentTests {

	@Test
	void createWhenNoPasswordThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new ClickHouseEnvironment(Collections.emptyMap()))
			.withMessage("No ClickHouse password found");
	}

	@Test
	void getPasswordWhenHasPassword() {
		ClickHouseEnvironment environment = new ClickHouseEnvironment(Map.of("CLICKHOUSE_PASSWORD", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordWhenHasNoPasswordAndAllowEmptyPassword() {
		ClickHouseEnvironment environment = new ClickHouseEnvironment(Map.of("ALLOW_EMPTY_PASSWORD", "true"));
		assertThat(environment.getPassword()).isEmpty();
	}

	@Test
	void getPasswordWhenHasNoPasswordAndAllowEmptyPasswordIsYes() {
		ClickHouseEnvironment environment = new ClickHouseEnvironment(Map.of("ALLOW_EMPTY_PASSWORD", "yes"));
		assertThat(environment.getPassword()).isEmpty();
	}

	@Test
	void getUsernameWhenNoUser() {
		ClickHouseEnvironment environment = new ClickHouseEnvironment(Map.of("CLICKHOUSE_PASSWORD", "secret"));
		assertThat(environment.getUsername()).isEqualTo("default");
	}

	@Test
	void getUsernameWhenHasUser() {
		ClickHouseEnvironment environment = new ClickHouseEnvironment(
				Map.of("CLICKHOUSE_USER", "me", "CLICKHOUSE_PASSWORD", "secret"));
		assertThat(environment.getUsername()).isEqualTo("me");
	}

	@Test
	void getDatabaseWhenNoDatabase() {
		ClickHouseEnvironment environment = new ClickHouseEnvironment(Map.of("CLICKHOUSE_PASSWORD", "secret"));
		assertThat(environment.getDatabase()).isEqualTo("default");
	}

	@Test
	void getDatabaseWhenHasDatabase() {
		ClickHouseEnvironment environment = new ClickHouseEnvironment(
				Map.of("CLICKHOUSE_DB", "db", "CLICKHOUSE_PASSWORD", "secret"));
		assertThat(environment.getDatabase()).isEqualTo("db");
	}

}
