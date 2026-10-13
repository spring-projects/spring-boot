/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
		TestHealthIndicator indicator = new TestHealthIndicator("Test message", Health.Builder::up);
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

		private final Consumer<Health.Builder> action;

		TestHealthIndicator(String message, Consumer<Health.Builder> action) {
			super(message);
			this.action = action;
		}

		TestHealthIndicator(Consumer<Health.Builder> action) {
			this.action = action;
		}

		@Override
		protected void doHealthCheck(Health.Builder builder) throws Exception {
			this.action.accept(builder);
		}

	}

}
