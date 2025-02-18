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

package org.springframework.boot.actuate.neo4j;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.driver.exceptions.SessionExpiredException;
import org.neo4j.driver.reactivestreams.ReactiveResult;
import org.neo4j.driver.reactivestreams.ReactiveSession;
import org.neo4j.driver.summary.ResultSummary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link Neo4jReactiveHealthIndicator}.
 *
 * @author Michael J. Simons
 * @author Stephane Nicoll
 * @author Brian Clozel
 */
class Neo4jReactiveHealthIndicatorTests {

	@Test
	void neo4jIsUp() {
		ResultSummary resultSummary = ResultSummaryMock.createResultSummary("My Home", "test");
		Driver driver = mockDriver(resultSummary, "4711", "ultimate collectors edition");
		Neo4jReactiveHealthIndicator healthIndicator = new Neo4jReactiveHealthIndicator(driver);
		healthIndicator.health().as(StepVerifier::create).consumeNextWith((health) -> {
			assertThat(health.getStatus()).isEqualTo(Status.UP);
			assertThat(health.getDetails()).containsEntry("server", "4711@My Home");
			assertThat(health.getDetails()).containsEntry("edition", "ultimate collectors edition");
		}).expectComplete().verify(Duration.ofSeconds(30));
	}

	@Test
	void neo4jIsUpWithOneSessionExpiredException() {
		ResultSummary resultSummary = ResultSummaryMock.createResultSummary("My Home", "");
		ReactiveSession session = mock(ReactiveSession.class);
		ReactiveResult statementResult = mockStatementResult(resultSummary, "4711", "some edition");
		AtomicInteger count = new AtomicInteger();
		given(session.run(anyString())).will((invocation) -> {
			if (count.compareAndSet(0, 1)) {
				return Flux.error(new SessionExpiredException("Session expired"));
			}
			return Flux.just(statementResult);
		});
		Driver driver = mock(Driver.class);
		given(driver.session(eq(ReactiveSession.class), any(SessionConfig.class))).willReturn(session);
		Neo4jReactiveHealthIndicator healthIndicator = new Neo4jReactiveHealthIndicator(driver);
		healthIndicator.health().as(StepVerifier::create).consumeNextWith((health) -> {
			assertThat(health.getStatus()).isEqualTo(Status.UP);
			assertThat(health.getDetails()).containsEntry("server", "4711@My Home");
			assertThat(health.getDetails()).containsEntry("edition", "some edition");
		}).expectComplete().verify(Duration.ofSeconds(30));
		then(session).should(times(2)).close();
	}

	@Test
	void neo4jIsDown() {
		Driver driver = mock(Driver.class);
		given(driver.session(eq(ReactiveSession.class), any(SessionConfig.class)))
			.willThrow(ServiceUnavailableException.class);
		Neo4jReactiveHealthIndicator healthIndicator = new Neo4jReactiveHealthIndicator(driver);
		healthIndicator.health().as(StepVerifier::create).consumeNextWith((health) -> {
			assertThat(health.getStatus()).isEqualTo(Status.DOWN);
			assertThat(health.getDetails()).containsKeys("error");
		}).expectComplete().verify(Duration.ofSeconds(30));
	}

	private ReactiveResult mockStatementResult(ResultSummary resultSummary, String version, String edition) {
		Record record = mock(Record.class);
		given(record.get("edition")).willReturn(Values.value(edition));
		given(record.get("version")).willReturn(Values.value(version));
		ReactiveResult statementResult = mock(ReactiveResult.class);
		given(statementResult.records()).willReturn(Mono.just(record));
		given(statementResult.consume()).willReturn(Mono.just(resultSummary));
		return statementResult;
	}

	private Driver mockDriver(ResultSummary resultSummary, String version, String edition) {
		ReactiveResult statementResult = mockStatementResult(resultSummary, version, edition);
		ReactiveSession session = mock(ReactiveSession.class);
		given(session.run(anyString())).willReturn(Mono.just(statementResult));
		Driver driver = mock(Driver.class);
		given(driver.session(eq(ReactiveSession.class), any(SessionConfig.class))).willReturn(session);
		return driver;
	}

}
