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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.driver.exceptions.SessionExpiredException;
import org.neo4j.driver.summary.ResultSummary;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link Neo4jHealthIndicator}.
 *
 * @author Eric Spiegelberg
 * @author Stephane Nicoll
 * @author Michael Simons
 */
class Neo4jHealthIndicatorTests {

	@Test
	void neo4jIsUp() {
		ResultSummary resultSummary = ResultSummaryMock.createResultSummary("4711", "My Home", "test");
		Driver driver = mockDriver(resultSummary, "ultimate collectors edition");
		Health health = new Neo4jHealthIndicator(driver).health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("server", "4711@My Home");
		assertThat(health.getDetails()).containsEntry("database", "test");
		assertThat(health.getDetails()).containsEntry("edition", "ultimate collectors edition");
	}

	@Test
	void neo4jIsUpWithoutDatabaseName() {
		ResultSummary resultSummary = ResultSummaryMock.createResultSummary("4711", "My Home", null);
		Driver driver = mockDriver(resultSummary, "some edition");
		Health health = new Neo4jHealthIndicator(driver).health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("server", "4711@My Home");
		assertThat(health.getDetails()).doesNotContainKey("database");
		assertThat(health.getDetails()).containsEntry("edition", "some edition");
	}

	@Test
	void neo4jIsUpWithEmptyDatabaseName() {
		ResultSummary resultSummary = ResultSummaryMock.createResultSummary("4711", "My Home", "");
		Driver driver = mockDriver(resultSummary, "some edition");
		Health health = new Neo4jHealthIndicator(driver).health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("server", "4711@My Home");
		assertThat(health.getDetails()).doesNotContainKey("database");
		assertThat(health.getDetails()).containsEntry("edition", "some edition");
	}

	@Test
	void neo4jIsUpWithOneSessionExpiredException() {
		ResultSummary resultSummary = ResultSummaryMock.createResultSummary("4711", "My Home", "");
		Session session = mock(Session.class);
		Result statementResult = mockStatementResult(resultSummary, "some edition");
		AtomicInteger count = new AtomicInteger();
		given(session.run(anyString())).will((invocation) -> {
			if (count.compareAndSet(0, 1)) {
				throw new SessionExpiredException("Session expired");
			}
			return statementResult;
		});
		Driver driver = mock(Driver.class);
		given(driver.session(any(SessionConfig.class))).willReturn(session);
		Neo4jHealthIndicator healthIndicator = new Neo4jHealthIndicator(driver);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("server", "4711@My Home");
		verify(session, times(2)).close();
	}

	@Test
	void neo4jIsDown() {
		Driver driver = mock(Driver.class);
		given(driver.session(any(SessionConfig.class))).willThrow(ServiceUnavailableException.class);
		Health health = new Neo4jHealthIndicator(driver).health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsKeys("error");
	}

	private Result mockStatementResult(ResultSummary resultSummary, String edition) {
		Record record = mock(Record.class);
		given(record.get("edition")).willReturn(Values.value(edition));
		Result statementResult = mock(Result.class);
		given(statementResult.single()).willReturn(record);
		given(statementResult.consume()).willReturn(resultSummary);
		return statementResult;
	}

	private Driver mockDriver(ResultSummary resultSummary, String edition) {
		Result statementResult = mockStatementResult(resultSummary, edition);
		Session session = mock(Session.class);
		given(session.run(anyString())).willReturn(statementResult);
		Driver driver = mock(Driver.class);
		given(driver.session(any(SessionConfig.class))).willReturn(session);
		return driver;
	}

}
