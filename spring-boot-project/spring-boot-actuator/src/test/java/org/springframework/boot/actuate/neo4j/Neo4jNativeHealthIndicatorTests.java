/*
 * Copyright 2012-2019 the original author or authors.
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.exceptions.ServiceUnavailableException;
import org.neo4j.driver.v1.exceptions.SessionExpiredException;
import org.neo4j.driver.v1.summary.ResultSummary;
import org.neo4j.driver.v1.summary.ServerInfo;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class Neo4jNativeHealthIndicatorTests {

	@Mock
	protected Driver driver;

	@Mock
	protected ResultSummary resultSummary;

	@Mock
	protected ServerInfo serverInfo;

	@Mock
	private Session session;

	@Mock
	private StatementResult statementResult;

	@Test
	void neo4jIsUp() {

		when(this.serverInfo.version()).thenReturn("4711");
		when(this.serverInfo.address()).thenReturn("somehost:7687");
		when(this.resultSummary.server()).thenReturn(this.serverInfo);
		when(this.statementResult.consume()).thenReturn(this.resultSummary);
		when(this.session.run(anyString())).thenReturn(this.statementResult);

		when(this.driver.session(AccessMode.WRITE)).thenReturn(this.session);

		Neo4jNativeHealthIndicator healthIndicator = new Neo4jNativeHealthIndicator(this.driver);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("server", "4711@somehost:7687");

		verify(this.session).close();
		verifyNoMoreInteractions(this.driver, this.session, this.statementResult, this.resultSummary, this.serverInfo);
	}

	@Test
	void neo4jSessionIsExpiredOnce() {

		AtomicInteger cnt = new AtomicInteger(0);

		when(this.serverInfo.version()).thenReturn("4711");
		when(this.serverInfo.address()).thenReturn("somehost:7687");
		when(this.resultSummary.server()).thenReturn(this.serverInfo);
		when(this.statementResult.consume()).thenReturn(this.resultSummary);
		when(this.session.run(anyString())).thenAnswer((invocation) -> {
			if (cnt.compareAndSet(0, 1)) {
				throw new SessionExpiredException("Session expired");
			}
			return Neo4jNativeHealthIndicatorTests.this.statementResult;
		});
		when(this.driver.session(AccessMode.WRITE)).thenReturn(this.session);

		Neo4jNativeHealthIndicator healthIndicator = new Neo4jNativeHealthIndicator(this.driver);
		Health health = healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("server", "4711@somehost:7687");

		verify(this.session, times(2)).close();
		verifyNoMoreInteractions(this.driver, this.session, this.statementResult, this.resultSummary, this.serverInfo);
	}

	@Test
	void neo4jSessionIsDown() {

		when(this.driver.session(AccessMode.WRITE)).thenThrow(ServiceUnavailableException.class);

		Neo4jNativeHealthIndicator healthIndicator = new Neo4jNativeHealthIndicator(this.driver);
		Health health = healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsKeys("error");

		verifyNoMoreInteractions(this.driver, this.session, this.statementResult, this.resultSummary, this.serverInfo);
	}

}
