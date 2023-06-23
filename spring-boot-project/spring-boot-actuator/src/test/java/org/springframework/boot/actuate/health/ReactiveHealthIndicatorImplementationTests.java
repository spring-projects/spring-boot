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

package org.springframework.boot.actuate.health;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractReactiveHealthIndicator}.
 *
 * @author Dmytro Nosan
 * @author Stephane Nicoll
 */
@ExtendWith(OutputCaptureExtension.class)
class ReactiveHealthIndicatorImplementationTests {

	@Test
	void healthUp(CapturedOutput output) {
		StepVerifier.create(new SimpleReactiveHealthIndicator().health())
			.consumeNextWith((health) -> assertThat(health).isEqualTo(Health.up().build()))
			.expectComplete()
			.verify(Duration.ofSeconds(30));
		assertThat(output).doesNotContain("Health check failed for simple");
	}

	@Test
	void healthDownWithCustomErrorMessage(CapturedOutput output) {
		StepVerifier.create(new CustomErrorMessageReactiveHealthIndicator().health())
			.consumeNextWith(
					(health) -> assertThat(health).isEqualTo(Health.down(new UnsupportedOperationException()).build()))
			.expectComplete()
			.verify(Duration.ofSeconds(30));
		assertThat(output).contains("Health check failed for custom");
	}

	@Test
	void healthDownWithCustomErrorMessageFunction(CapturedOutput output) {
		StepVerifier.create(new CustomErrorMessageFunctionReactiveHealthIndicator().health())
			.consumeNextWith((health) -> assertThat(health).isEqualTo(Health.down(new RuntimeException()).build()))
			.expectComplete()
			.verify(Duration.ofSeconds(30));
		assertThat(output).contains("Health check failed with RuntimeException");
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
