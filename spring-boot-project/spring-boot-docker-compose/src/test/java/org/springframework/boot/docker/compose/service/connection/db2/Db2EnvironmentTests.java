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

package org.springframework.boot.docker.compose.service.connection.db2;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link Db2Environment}.
 *
 * @author Yanming Zhou
 */
class Db2EnvironmentTests {

	@Test
	void createWhenNoDb2InstancePasswordThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new Db2Environment(Collections.emptyMap()))
			.withMessage("DB2 password must be provided");
	}

	@Test
	void createWhenNoDb2InstanceDatabaseThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new Db2Environment(Map.of("DB2INST1_PASSWORD", "secret")))
			.withMessage("DB2 database must be provided");
	}

	@Test
	void getUsernameWhenNoDb2Instance() {
		Db2Environment environment = new Db2Environment(Map.of("DB2INST1_PASSWORD", "secret", "DBNAME", "testdb"));
		assertThat(environment.getUsername()).isEqualTo("db2inst1");
	}

	@Test
	void getUsernameWhenHasDb2Instance() {
		Db2Environment environment = new Db2Environment(
				Map.of("DB2INSTANCE", "db2inst2", "DB2INST1_PASSWORD", "secret", "DBNAME", "testdb"));
		assertThat(environment.getUsername()).isEqualTo("db2inst2");
	}

	@Test
	void getPasswordWhenHasDb2InstancePassword() {
		Db2Environment environment = new Db2Environment(Map.of("DB2INST1_PASSWORD", "secret", "DBNAME", "testdb"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getDatabaseWhenHasDbName() {
		Db2Environment environment = new Db2Environment(Map.of("DB2INST1_PASSWORD", "secret", "DBNAME", "testdb"));
		assertThat(environment.getDatabase()).isEqualTo("testdb");
	}

}
