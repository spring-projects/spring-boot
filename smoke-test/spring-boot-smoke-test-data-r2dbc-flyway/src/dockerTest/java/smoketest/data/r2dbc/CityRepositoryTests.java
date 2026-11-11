/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.r2dbc;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
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
	static PostgreSQLContainer postgresql = TestImage.forContainer(PostgreSQLContainer.class)
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
