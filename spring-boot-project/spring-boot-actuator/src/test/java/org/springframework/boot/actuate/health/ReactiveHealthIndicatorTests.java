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

package org.springframework.boot.actuate.health;

import org.junit.Rule;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.test.rule.OutputCapture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractReactiveHealthIndicator}.
 *
 * @author Dmytro Nosan
 * @author Stephane Nicoll
 */
public class ReactiveHealthIndicatorTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	@Test
	public void healthUp() {
		StepVerifier.create(new SimpleReactiveHealthIndicator().health())
				.consumeNextWith((health) -> assertThat(health).isEqualTo(Health.up().build())).verifyComplete();
		assertThat(this.output.toString()).doesNotContain("Health check failed for simple");
	}

	@Test
	public void healthDownWithCustomErrorMessage() {
		StepVerifier.create(new CustomErrorMessageReactiveHealthIndicator().health()).consumeNextWith(
				(health) -> assertThat(health).isEqualTo(Health.down(new UnsupportedOperationException()).build()))
				.verifyComplete();
		assertThat(this.output.toString()).contains("Health check failed for custom");
	}

	@Test
	public void healthDownWithCustomErrorMessageFunction() {
		StepVerifier.create(new CustomErrorMessageFunctionReactiveHealthIndicator().health())
				.consumeNextWith((health) -> assertThat(health).isEqualTo(Health.down(new RuntimeException()).build()))
				.verifyComplete();
		assertThat(this.output.toString()).contains("Health check failed with RuntimeException");
	}

	private static final class SimpleReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

		SimpleReactiveHealthIndicator() {
			super("Health check failed for simple");
		}

		@Override
		protected Mono<Health> doHealthCheck(Builder builder) {
			return Mono.just(builder.up().build());
		}

	}

	private static final class CustomErrorMessageReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

		CustomErrorMessageReactiveHealthIndicator() {
			super("Health check failed for custom");
		}

		@Override
		protected Mono<Health> doHealthCheck(Builder builder) {
			return Mono.error(new UnsupportedOperationException());
		}

	}

	private static final class CustomErrorMessageFunctionReactiveHealthIndicator
			extends AbstractReactiveHealthIndicator {

		CustomErrorMessageFunctionReactiveHealthIndicator() {
			super((ex) -> "Health check failed with " + ex.getClass().getSimpleName());
		}

		@Override
		protected Mono<Health> doHealthCheck(Builder builder) {
			throw new RuntimeException();
		}

	}

}
