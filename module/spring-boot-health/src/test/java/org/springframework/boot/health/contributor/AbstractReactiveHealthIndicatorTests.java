/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractReactiveHealthIndicator}.
 *
 * @author Moritz Halbritter
 * @author Dmytro Nosan
 * @author Stephane Nicoll
 */
@ExtendWith(OutputCaptureExtension.class)
class AbstractReactiveHealthIndicatorTests {

	@Test
	void healthCheckWhenUpDoesNotLogHealthCheckFailedMessage(CapturedOutput output) {
		Health health = new AbstractReactiveHealthIndicator("Test message") {

			@Override
			protected Mono<Health> doHealthCheck(Health.Builder builder) {
				return Mono.just(builder.up().build());
			}

		}.health().block();
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(output).doesNotContain("Test message");
	}

	@Test
	void healthCheckWhenDownWithExceptionThrownLogsHealthCheckFailedMessage(CapturedOutput output) {
		Health health = new AbstractReactiveHealthIndicator("Test message") {

			@Override
			protected Mono<Health> doHealthCheck(Health.Builder builder) {
				throw new IllegalStateException("Test exception");
			}

		}.health().block();
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(output).contains("Test message").contains("Test exception");
	}

	@Test
	void healthCheckWhenDownWithExceptionConfiguredLogsHealthCheckFailedMessage(CapturedOutput output) {
		Health health = new AbstractReactiveHealthIndicator("Test message") {

			@Override
			protected Mono<Health> doHealthCheck(Health.Builder builder) {
				return Mono.just(builder.down().withException(new IllegalStateException("Test exception")).build());
			}

		}.health().block();
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(output).contains("Test message").contains("Test exception");
	}

	@Test
	void healthCheckWhenDownWithExceptionConfiguredDoesNotLogHealthCheckFailedMessageTwice(CapturedOutput output) {
		Health health = new AbstractReactiveHealthIndicator("Test message") {

			@Override
			protected Mono<Health> doHealthCheck(Health.Builder builder) {
				IllegalStateException ex = new IllegalStateException("Test exception");
				builder.down().withException(ex);
				throw ex;
			}

		}.health().block();
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(output).contains("Test message").containsOnlyOnce("Test exception");
	}

	@Test
	void healthCheckWhenDownWithExceptionAndNoFailureMessageLogsDefaultMessage(CapturedOutput output) {
		Health health = new AbstractReactiveHealthIndicator() {

			@Override
			protected Mono<Health> doHealthCheck(Health.Builder builder) {
				return Mono.just(builder.down().withException(new IllegalStateException("Test exception")).build());
			}

		}.health().block();
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(output).contains("Health check failed").contains("Test exception");
	}

	@Test
	void healthCheckWhenDownWithErrorLogsDefaultMessage(CapturedOutput output) {
		Health health = new AbstractReactiveHealthIndicator("Test Message") {

			@Override
			protected Mono<Health> doHealthCheck(Health.Builder builder) {
				return Mono.just(builder.down().withException(new Error("Test error")).build());
			}

		}.health().block();
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(output).contains("Health check failed").contains("Test error");
	}

}
