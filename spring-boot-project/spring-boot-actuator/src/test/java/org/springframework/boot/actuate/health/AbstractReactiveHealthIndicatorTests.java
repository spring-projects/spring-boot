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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractReactiveHealthIndicator}.
 *
 * @author Dmytro Nosan
 */
@ExtendWith(OutputCaptureExtension.class)
class AbstractReactiveHealthIndicatorTests {

	@Test
	void doHealth() {
		Health health = new SimpleReactiveHealthIndicator().health().block();
		assertThat(health).isEqualTo(Health.up().build());
	}

	@Test
	void doHealthCustomErrorMessage(CapturedOutput output) {
		Health health = new ErrorMessageReactiveHealthIndicator().health().block();
		assertThat(health).isEqualTo(Health.down(new UnsupportedOperationException()).build());
		assertThat(output).contains("Unsupported Operation");
	}

	@Test
	void doHealthCustomErrorMessageFunction(CapturedOutput output) {
		Health health = new CustomErrorFunctionReactiveHealthIndicator().health().block();
		assertThat(health).isEqualTo(Health.down(new RuntimeException()).build());
		assertThat(output).contains("Runtime Exception");
	}

	private static final class SimpleReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

		@Override
		protected Mono<Health> doHealthCheck(Builder builder) {
			return Mono.just(builder.up().build());
		}

	}

	private static final class ErrorMessageReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

		ErrorMessageReactiveHealthIndicator() {
			super("Unsupported Operation");
		}

		@Override
		protected Mono<Health> doHealthCheck(Builder builder) {
			return Mono.error(new UnsupportedOperationException());
		}

	}

	private static final class CustomErrorFunctionReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

		CustomErrorFunctionReactiveHealthIndicator() {
			super((ex) -> "Runtime Exception");
		}

		@Override
		protected Mono<Health> doHealthCheck(Builder builder) {
			throw new RuntimeException();
		}

	}

}
