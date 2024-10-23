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

package org.springframework.boot.docker.compose.service.connection.clickhouse;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ClickHouseEnvironment}.
 *
 * @author Dmytro Nosan
 */
class ClickHouseEnvironmentTests {

	@Test
	void getUsernameWhenClickHouseUserEnvIsPresent() {
		ClickHouseEnvironment environment = createEnvironment(Map.of("CLICKHOUSE_USER", "test"));
		assertThat(environment.getUsername()).isEqualTo("test");
	}

	@Test
	void getUsernameWhenClickHouseUserEnvDoesNotExist() {
		ClickHouseEnvironment environment = createEnvironment(Collections.emptyMap());
		assertThat(environment.getUsername()).isEqualTo("default");
	}

	@Test
	void getPasswordWhenClickHousePasswordEnvIsPresent() {
		ClickHouseEnvironment environment = createEnvironment(Map.of("CLICKHOUSE_PASSWORD", "test"));
		assertThat(environment.getPassword()).isEqualTo("test");
	}

	@Test
	void getPasswordWhenClickHousePasswordEnvDoesNotExist() {
		ClickHouseEnvironment environment = createEnvironment(Collections.emptyMap());
		assertThat(environment.getPassword()).isEqualTo("");
	}

	@Test
	void getDatabaseWhenClickHouseDbEnvIsPresent() {
		ClickHouseEnvironment environment = createEnvironment(Map.of("CLICKHOUSE_DB", "test"));
		assertThat(environment.getDatabase()).isEqualTo("test");
	}

	@Test
	void getDatabaseWhenClickHouseDbEnvDoesNotExist() {
		ClickHouseEnvironment environment = createEnvironment(Collections.emptyMap());
		assertThat(environment.getDatabase()).isEqualTo("default");
	}

	private ClickHouseEnvironment createEnvironment(Map<String, String> env) {
		return new ClickHouseEnvironment(env);
	}

}
