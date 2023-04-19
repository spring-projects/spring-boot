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

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractHealthIndicator}.
 *
 * @author Stephane Nicoll
 * @author Madhura Bhave
 */
@ExtendWith(OutputCaptureExtension.class)
class AbstractHealthIndicatorTests {

	@Test
	void healthCheckWhenUpDoesNotLogHealthCheckFailedMessage(CapturedOutput output) {
		TestHealthIndicator indicator = new TestHealthIndicator("Test message", Builder::up);
		Health heath = indicator.health();
		assertThat(heath.getStatus()).isEqualTo(Status.UP);
		assertThat(output).doesNotContain("Test message");
	}

	@Test
	void healthCheckWhenDownWithExceptionThrownLogsHealthCheckFailedMessage(CapturedOutput output) {
		TestHealthIndicator indicator = new TestHealthIndicator("Test message", (builder) -> {
			throw new IllegalStateException("Test exception");
		});
		Health heath = indicator.health();
		assertThat(heath.getStatus()).isEqualTo(Status.DOWN);
		assertThat(output).contains("Test message").contains("Test exception");
	}

	@Test
	void healthCheckWhenDownWithExceptionConfiguredLogsHealthCheckFailedMessage(CapturedOutput output) {
		Health heath = new TestHealthIndicator("Test message",
				(builder) -> builder.down().withException(new IllegalStateException("Test exception")))
			.health();
		assertThat(heath.getStatus()).isEqualTo(Status.DOWN);
		assertThat(output).contains("Test message").contains("Test exception");
	}

	@Test
	void healthCheckWhenDownWithExceptionConfiguredDoesNotLogHealthCheckFailedMessageTwice(CapturedOutput output) {
		TestHealthIndicator indicator = new TestHealthIndicator("Test message", (builder) -> {
			IllegalStateException ex = new IllegalStateException("Test exception");
			builder.down().withException(ex);
			throw ex;
		});
		Health heath = indicator.health();
		assertThat(heath.getStatus()).isEqualTo(Status.DOWN);
		assertThat(output).contains("Test message").containsOnlyOnce("Test exception");
	}

	@Test
	void healthCheckWhenDownWithExceptionAndNoFailureMessageLogsDefaultMessage(CapturedOutput output) {
		TestHealthIndicator indicator = new TestHealthIndicator(
				(builder) -> builder.down().withException(new IllegalStateException("Test exception")));
		Health heath = indicator.health();
		assertThat(heath.getStatus()).isEqualTo(Status.DOWN);
		assertThat(output).contains("Health check failed").contains("Test exception");
	}

	@Test
	void healthCheckWhenDownWithErrorLogsDefaultMessage(CapturedOutput output) {
		TestHealthIndicator indicator = new TestHealthIndicator("Test Message",
				(builder) -> builder.down().withException(new Error("Test error")));
		Health heath = indicator.health();
		assertThat(heath.getStatus()).isEqualTo(Status.DOWN);
		assertThat(output).contains("Health check failed").contains("Test error");
	}

	static class TestHealthIndicator extends AbstractHealthIndicator {

		private final Consumer<Builder> action;

		TestHealthIndicator(String message, Consumer<Builder> action) {
			super(message);
			this.action = action;
		}

		TestHealthIndicator(Consumer<Builder> action) {
			this.action = action;
		}

		@Override
		protected void doHealthCheck(Builder builder) throws Exception {
			this.action.accept(builder);
		}

	}

}
