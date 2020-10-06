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

package org.springframework.boot.actuate.health;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HealthIndicatorReactiveAdapter}.
 *
 * @author Stephane Nicoll
 */
class HealthIndicatorReactiveAdapterTests {

	@Test
	void delegateReturnsHealth() {
		HealthIndicator delegate = mock(HealthIndicator.class);
		HealthIndicatorReactiveAdapter adapter = new HealthIndicatorReactiveAdapter(delegate);
		Health status = Health.up().build();
		given(delegate.health()).willReturn(status);
		StepVerifier.create(adapter.health()).expectNext(status).verifyComplete();
	}

	@Test
	void delegateThrowError() {
		HealthIndicator delegate = mock(HealthIndicator.class);
		HealthIndicatorReactiveAdapter adapter = new HealthIndicatorReactiveAdapter(delegate);
		given(delegate.health()).willThrow(new IllegalStateException("Expected"));
		StepVerifier.create(adapter.health()).expectError(IllegalStateException.class).verify(Duration.ofSeconds(10));
	}

	@Test
	void delegateRunsOnTheElasticScheduler() {
		String currentThread = Thread.currentThread().getName();
		HealthIndicator delegate = () -> Health
				.status(Thread.currentThread().getName().equals(currentThread) ? Status.DOWN : Status.UP).build();
		HealthIndicatorReactiveAdapter adapter = new HealthIndicatorReactiveAdapter(delegate);
		StepVerifier.create(adapter.health()).expectNext(Health.status(Status.UP).build()).verifyComplete();
	}

}
