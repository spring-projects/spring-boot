/*
 * Copyright 2012-2018 the original author or authors.
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

import com.datastax.driver.core.querybuilder.Select;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.cassandra.CassandraInternalException;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;
import org.springframework.data.cassandra.core.cql.ReactiveCqlOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CassandraReactiveHealthIndicator}.
 *
 * @author Artsiom Yudovin
 */
public class CassandraReactiveHealthIndicatorTests {

	@Test
	public void testCassandraIsUp() {
		ReactiveCqlOperations reactiveCqlOperations = mock(ReactiveCqlOperations.class);
		given(reactiveCqlOperations.queryForObject(any(Select.class), eq(String.class)))
				.willReturn(Mono.just("6.0.0"));
		ReactiveCassandraOperations reactiveCassandraOperations = mock(
				ReactiveCassandraOperations.class);
		given(reactiveCassandraOperations.getReactiveCqlOperations())
				.willReturn(reactiveCqlOperations);

		CassandraReactiveHealthIndicator cassandraReactiveHealthIndicator = new CassandraReactiveHealthIndicator(
				reactiveCassandraOperations);
		Mono<Health> health = cassandraReactiveHealthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails()).containsOnlyKeys("version");
			assertThat(h.getDetails().get("version")).isEqualTo("6.0.0");
		}).verifyComplete();
	}

	@Test
	public void testCassandraIsDown() {
		ReactiveCassandraOperations reactiveCassandraOperations = mock(
				ReactiveCassandraOperations.class);
		given(reactiveCassandraOperations.getReactiveCqlOperations())
				.willThrow(new CassandraInternalException("Connection failed"));

		CassandraReactiveHealthIndicator cassandraReactiveHealthIndicator = new CassandraReactiveHealthIndicator(
				reactiveCassandraOperations);
		Mono<Health> health = cassandraReactiveHealthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.DOWN);
			assertThat(h.getDetails()).containsOnlyKeys("error");
			assertThat(h.getDetails().get("error")).isEqualTo(
					CassandraInternalException.class.getName() + ": Connection failed");
		}).verifyComplete();
	}

}
