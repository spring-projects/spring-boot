/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.health.contributor;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveHealthIndicatorAdapter}.
 *
 * @author Phillip Webb
 */
class ReactiveHealthIndicatorAdapterTests {

	@Test
	void getHealthReturnsDetails() {
		ReactiveHealthIndicator reactiveHealthIndicator = () -> Mono
			.just(Health.up().withDetail("test", "test").build());
		ReactiveHealthIndicatorAdapter adapter = new ReactiveHealthIndicatorAdapter(reactiveHealthIndicator);
		assertThat(adapter.health().getStatus()).isEqualTo(Status.UP);
		assertThat(adapter.health().getDetails()).containsEntry("test", "test");
	}

	@Test
	void getHealthWithoutDetailsReturnsHealth() {
		ReactiveHealthIndicator reactiveHealthIndicator = () -> Mono
			.just(Health.up().withDetail("test", "test").build());
		ReactiveHealthIndicatorAdapter adapter = new ReactiveHealthIndicatorAdapter(reactiveHealthIndicator);
		assertThat(adapter.health(false).getStatus()).isEqualTo(Status.UP);
		assertThat(adapter.health(false).getDetails()).isEmpty();
	}

}
