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

package org.springframework.boot.actuate.neo4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.concurrent.atomic.AtomicInteger;

import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.driver.exceptions.SessionExpiredException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link Neo4jHealthIndicator}.
 *
 * @author Eric Spiegelberg
 * @author Stephane Nicoll
 * @author Michael Simons
 */
@ExtendWith(MockitoExtension.class)
class Neo4jHealthIndicatorTests extends Neo4jHealthIndicatorTestBase {

	@Mock
	private Session session;

	@Mock
	private Result statementResult;

	@Test
	void shouldWorkWithoutDatabaseName() {
		when(this.serverInfo.version()).thenReturn("4711");
		when(this.serverInfo.address()).thenReturn("Zu Hause");
		when(this.resultSummary.server()).thenReturn(this.serverInfo);
		when(this.resultSummary.database()).thenReturn(this.databaseInfo);

		when(this.databaseInfo.name()).thenReturn(null);

		when(record.get("edition")).thenReturn(Values.value("some edition"));
		when(this.statementResult.single()).thenReturn(this.record);
		when(this.statementResult.consume()).thenReturn(this.resultSummary);
		when(this.session.run(anyString())).thenReturn(this.statementResult);

		when(this.driver.session(any(SessionConfig.class))).thenReturn(this.session);

		Neo4jHealthIndicator healthIndicator = new Neo4jHealthIndicator(this.driver);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("server", "4711@Zu Hause");
		assertThat(health.getDetails()).doesNotContainKey("database");
		assertThat(health.getDetails()).containsEntry("edition", "some edition");
	}

	@Test
	void shouldWorkWithEmptyDatabaseName() {
		when(this.serverInfo.version()).thenReturn("4711");
		when(this.serverInfo.address()).thenReturn("Zu Hause");
		when(this.resultSummary.server()).thenReturn(this.serverInfo);
		when(this.resultSummary.database()).thenReturn(this.databaseInfo);

		when(this.databaseInfo.name()).thenReturn("");

		when(record.get("edition")).thenReturn(Values.value("some edition"));
		when(this.statementResult.single()).thenReturn(this.record);
		when(this.statementResult.consume()).thenReturn(this.resultSummary);
		when(this.session.run(anyString())).thenReturn(this.statementResult);

		when(driver.session(any(SessionConfig.class))).thenReturn(this.session);

		Neo4jHealthIndicator healthIndicator = new Neo4jHealthIndicator(this.driver);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("server", "4711@Zu Hause");
		assertThat(health.getDetails()).doesNotContainKey("database");
		assertThat(health.getDetails()).containsEntry("edition", "some edition");
	}

	@Test
	void neo4jIsUp() {

		prepareSharedMocks();

		when(this.statementResult.single()).thenReturn(this.record);
		when(this.statementResult.consume()).thenReturn(this.resultSummary);
		when(this.session.run(anyString())).thenReturn(this.statementResult);

		when(this.driver.session(any(SessionConfig.class))).thenReturn(this.session);

		Neo4jHealthIndicator healthIndicator = new Neo4jHealthIndicator(this.driver);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("server", "4711@Zu Hause");
		assertThat(health.getDetails()).containsEntry("database", "n/a");
		assertThat(health.getDetails()).containsEntry("edition", "ultimate collectors edition");

		verify(session).close();
		verifyNoMoreInteractions(this.driver, this.session, this.statementResult, this.resultSummary, this.serverInfo,
				this.databaseInfo);
	}

	@Test
	void neo4jSessionIsExpiredOnce() {

		AtomicInteger cnt = new AtomicInteger(0);

		prepareSharedMocks();
		when(this.statementResult.single()).thenReturn(this.record);
		when(this.statementResult.consume()).thenReturn(this.resultSummary);
		when(this.session.run(anyString())).thenAnswer(invocation -> {
			if (cnt.compareAndSet(0, 1)) {
				throw new SessionExpiredException("Session expired");
			}
			return Neo4jHealthIndicatorTests.this.statementResult;
		});
		when(driver.session(any(SessionConfig.class))).thenReturn(this.session);

		Neo4jHealthIndicator healthIndicator = new Neo4jHealthIndicator(this.driver);
		Health health = healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("server", "4711@Zu Hause");

		verify(this.session, times(2)).close();
		verifyNoMoreInteractions(this.driver, this.session, this.statementResult, this.resultSummary, this.serverInfo,
				this.databaseInfo);
	}

	@Test
	void neo4jSessionIsDown() {

		when(driver.session(any(SessionConfig.class))).thenThrow(ServiceUnavailableException.class);

		Neo4jHealthIndicator healthIndicator = new Neo4jHealthIndicator(driver);
		Health health = healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsKeys("error");

		verifyNoMoreInteractions(this.driver, this.session, this.statementResult, this.resultSummary, this.serverInfo,
				this.databaseInfo);
	}

}
