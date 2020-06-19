/*
 * Copyright 2012-2020 the original author or authors.
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

package smoketest.data.r2dbc;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CityRepository}.
 */
@Testcontainers(disabledWithoutDocker = true)
@DataR2dbcTest
class CityRepositoryTests {

	@Container
	static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>().withDatabaseName("test_liquibase");

	@DynamicPropertySource
	static void postgresqlProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.r2dbc.url", CityRepositoryTests::r2dbcUrl);
		registry.add("spring.r2dbc.username", postgresql::getUsername);
		registry.add("spring.r2dbc.password", postgresql::getPassword);

		// configure liquibase to use the same database
		registry.add("spring.liquibase.url", postgresql::getJdbcUrl);
		registry.add("spring.liquibase.user", postgresql::getUsername);
		registry.add("spring.liquibase.password", postgresql::getPassword);
	}

	@Autowired
	private CityRepository repository;

	@Test
	void databaseHasBeenInitialized() {
		StepVerifier.create(this.repository.findByState("DC").filter((city) -> city.getName().equals("Washington")))
				.consumeNextWith((city) -> assertThat(city.getId()).isNotNull()).verifyComplete();
	}

	private static String r2dbcUrl() {
		return String.format("r2dbc:postgresql://%s:%s/%s", postgresql.getHost(),
				postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT), postgresql.getDatabaseName());
	}

}
