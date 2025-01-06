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

package smoketest.data.r2dbc;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CityRepository}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@Testcontainers(disabledWithoutDocker = true)
@DataR2dbcTest
class CityRepositoryTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgresql = TestImage.container(PostgreSQLContainer.class)
		.withDatabaseName("test_flyway");

	@Autowired
	private CityRepository repository;

	@Test
	void databaseHasBeenInitialized() {
		StepVerifier.create(this.repository.findByState("DC").filter((city) -> city.getName().equals("Washington")))
			.consumeNextWith((city) -> assertThat(city.getId()).isNotNull())
			.expectComplete()
			.verify(Duration.ofSeconds(30));
	}

}
