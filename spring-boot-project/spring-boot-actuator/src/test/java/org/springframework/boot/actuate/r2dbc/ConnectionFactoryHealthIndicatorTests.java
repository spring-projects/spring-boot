/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.r2dbc;

import java.util.Collections;
import java.util.UUID;

import io.r2dbc.h2.CloseableConnectionFactory;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.h2.H2ConnectionOption;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.ValidationDepth;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConnectionFactoryHealthIndicator}.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
class ConnectionFactoryHealthIndicatorTests {

	@Test
	void healthIndicatorWhenDatabaseUpWithConnectionValidation() {
		CloseableConnectionFactory connectionFactory = createTestDatabase();
		try {
			ConnectionFactoryHealthIndicator healthIndicator = new ConnectionFactoryHealthIndicator(connectionFactory);
			healthIndicator.health().as(StepVerifier::create).assertNext((actual) -> {
				assertThat(actual.getStatus()).isEqualTo(Status.UP);
				assertThat(actual.getDetails()).containsOnly(entry("database", "H2"),
						entry("validationQuery", "validate(REMOTE)"));
			}).verifyComplete();
		}
		finally {
			StepVerifier.create(connectionFactory.close()).verifyComplete();
		}
	}

	@Test
	void healthIndicatorWhenDatabaseDownWithConnectionValidation() {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.getMetadata()).willReturn(() -> "mock");
		RuntimeException exception = new RuntimeException("test");
		given(connectionFactory.create()).willReturn(Mono.error(exception));
		ConnectionFactoryHealthIndicator healthIndicator = new ConnectionFactoryHealthIndicator(connectionFactory);
		healthIndicator.health().as(StepVerifier::create).assertNext((actual) -> {
			assertThat(actual.getStatus()).isEqualTo(Status.DOWN);
			assertThat(actual.getDetails()).containsOnly(entry("database", "mock"),
					entry("validationQuery", "validate(REMOTE)"), entry("error", "java.lang.RuntimeException: test"));
		}).verifyComplete();
	}

	@Test
	void healthIndicatorWhenConnectionValidationFails() {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.getMetadata()).willReturn(() -> "mock");
		Connection connection = mock(Connection.class);
		given(connection.validate(ValidationDepth.REMOTE)).willReturn(Mono.just(false));
		given(connection.close()).willReturn(Mono.empty());
		given(connectionFactory.create()).willAnswer((invocation) -> Mono.just(connection));
		ConnectionFactoryHealthIndicator healthIndicator = new ConnectionFactoryHealthIndicator(connectionFactory);
		healthIndicator.health().as(StepVerifier::create).assertNext((actual) -> {
			assertThat(actual.getStatus()).isEqualTo(Status.DOWN);
			assertThat(actual.getDetails()).containsOnly(entry("database", "mock"),
					entry("validationQuery", "validate(REMOTE)"));
		}).verifyComplete();
	}

	@Test
	void healthIndicatorWhenDatabaseUpWithSuccessValidationQuery() {
		CloseableConnectionFactory connectionFactory = createTestDatabase();
		try {
			String customValidationQuery = "SELECT COUNT(*) from HEALTH_TEST";
			Mono.from(connectionFactory.create()).flatMapMany((it) -> Flux
					.from(it.createStatement("CREATE TABLE HEALTH_TEST (id INTEGER IDENTITY PRIMARY KEY)").execute())
					.flatMap(Result::getRowsUpdated).thenMany(it.close())).as(StepVerifier::create).verifyComplete();
			ReactiveHealthIndicator healthIndicator = new ConnectionFactoryHealthIndicator(connectionFactory,
					customValidationQuery);
			healthIndicator.health().as(StepVerifier::create).assertNext((actual) -> {
				assertThat(actual.getStatus()).isEqualTo(Status.UP);
				assertThat(actual.getDetails()).containsOnly(entry("database", "H2"), entry("result", 0L),
						entry("validationQuery", customValidationQuery));
			}).verifyComplete();
		}
		finally {
			StepVerifier.create(connectionFactory.close()).verifyComplete();
		}

	}

	@Test
	void healthIndicatorWhenDatabaseUpWithFailureValidationQuery() {
		CloseableConnectionFactory connectionFactory = createTestDatabase();
		try {
			String invalidValidationQuery = "SELECT COUNT(*) from DOES_NOT_EXIST";
			ReactiveHealthIndicator healthIndicator = new ConnectionFactoryHealthIndicator(connectionFactory,
					invalidValidationQuery);
			healthIndicator.health().as(StepVerifier::create).assertNext((actual) -> {
				assertThat(actual.getStatus()).isEqualTo(Status.DOWN);
				assertThat(actual.getDetails()).contains(entry("database", "H2"),
						entry("validationQuery", invalidValidationQuery));
				assertThat(actual.getDetails()).containsOnlyKeys("database", "error", "validationQuery");
			}).verifyComplete();
		}
		finally {
			StepVerifier.create(connectionFactory.close()).verifyComplete();
		}
	}

	private CloseableConnectionFactory createTestDatabase() {
		return H2ConnectionFactory.inMemory("db-" + UUID.randomUUID(), "sa", "",
				Collections.singletonMap(H2ConnectionOption.DB_CLOSE_DELAY, "-1"));
	}

}
