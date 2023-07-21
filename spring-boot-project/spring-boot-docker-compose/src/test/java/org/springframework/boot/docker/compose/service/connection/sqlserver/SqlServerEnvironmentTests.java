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

package org.springframework.boot.docker.compose.service.connection.sqlserver;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SqlServerEnvironment}.
 *
 * @author Andy Wilkinson
 */
class SqlServerEnvironmentTests {

	@Test
	void createWhenHasNoPasswordThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new SqlServerEnvironment(Collections.emptyMap()))
			.withMessage("No MSSQL password found");
	}

	@Test
	void getUsernameWhenHasNoMsSqlUser() {
		SqlServerEnvironment environment = new SqlServerEnvironment(Map.of("MSSQL_SA_PASSWORD", "secret"));
		assertThat(environment.getUsername()).isEqualTo("SA");
	}

	@Test
	void getPasswordWhenHasMsSqlSaPassword() {
		SqlServerEnvironment environment = new SqlServerEnvironment(Map.of("MSSQL_SA_PASSWORD", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordWhenHasSaPassword() {
		SqlServerEnvironment environment = new SqlServerEnvironment(Map.of("SA_PASSWORD", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordWhenHasMsSqlSaPasswordAndSaPasswordPrefersMsSqlSaPassword() {
		SqlServerEnvironment environment = new SqlServerEnvironment(
				Map.of("MSSQL_SA_PASSWORD", "secret", "SA_PASSWORD", "not used"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

}
