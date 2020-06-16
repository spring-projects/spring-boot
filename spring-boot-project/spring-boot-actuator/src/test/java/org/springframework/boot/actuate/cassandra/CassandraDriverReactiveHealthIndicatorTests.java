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
package org.springframework.boot.actuate.cassandra;

import com.datastax.dse.driver.api.core.cql.reactive.ReactiveResultSet;
import com.datastax.dse.driver.api.core.cql.reactive.ReactiveRow;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverTimeoutException;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.doAnswer;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

/**
 * Tests for {@link CassandraDriverReactiveHealthIndicator}.
 *
 * @author Alexandre Dutra
 * @since 2.4.0
 */
class CassandraDriverReactiveHealthIndicatorTests {

	@Test
	void createWhenCqlSessionIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CassandraDriverReactiveHealthIndicator(null));
	}

	@Test
	void testCassandraIsUp() {
		CqlSession session = mock(CqlSession.class);
		ReactiveResultSet results = mock(ReactiveResultSet.class);
		ReactiveRow row = mock(ReactiveRow.class);
		given(session.executeReactive(any(SimpleStatement.class))).willReturn(results);
		doAnswer(mockReactiveResultSetBehavior(row)).when(results).subscribe(any());
		given(row.getString(0)).willReturn("6.0.0");
		CassandraDriverReactiveHealthIndicator cassandraReactiveHealthIndicator = new CassandraDriverReactiveHealthIndicator(
				session);
		Mono<Health> health = cassandraReactiveHealthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails()).containsOnlyKeys("version");
			assertThat(h.getDetails().get("version")).isEqualTo("6.0.0");
		}).verifyComplete();
	}

	@Test
	void testCassandraIsDown() {
		CqlSession session = mock(CqlSession.class);
		given(session.executeReactive(any(SimpleStatement.class)))
				.willThrow(new DriverTimeoutException("Test Exception"));
		CassandraDriverReactiveHealthIndicator cassandraReactiveHealthIndicator = new CassandraDriverReactiveHealthIndicator(
				session);
		Mono<Health> health = cassandraReactiveHealthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.DOWN);
			assertThat(h.getDetails()).containsOnlyKeys("error");
			assertThat(h.getDetails().get("error"))
					.isEqualTo(DriverTimeoutException.class.getName() + ": Test Exception");
		}).verifyComplete();
	}

	private Answer<Void> mockReactiveResultSetBehavior(ReactiveRow row) {
		return (invocation) -> {
			Subscriber<ReactiveRow> subscriber = invocation.getArgument(0);
			Subscription s = new Subscription() {
				@Override
				public void request(long n) {
					subscriber.onNext(row);
					subscriber.onComplete();
				}

				@Override
				public void cancel() {
				}
			};
			subscriber.onSubscribe(s);
			return null;
		};
	}

}
