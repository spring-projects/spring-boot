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
 * @author Moritz Halbritter
 */
@ExtendWith(OutputCaptureExtension.class)
class AbstractReactiveHealthIndicatorTests {

	@Test
	void healthCheckWhenUpDoesNotLogHealthCheckFailedMessage(CapturedOutput output) {
		Health health = new AbstractReactiveHealthIndicator("Test message") {
			@Override
			protected Mono<Health> doHealthCheck(Builder builder) {
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
			protected Mono<Health> doHealthCheck(Builder builder) {
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
			protected Mono<Health> doHealthCheck(Builder builder) {
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
			protected Mono<Health> doHealthCheck(Builder builder) {
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
			protected Mono<Health> doHealthCheck(Builder builder) {
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
			protected Mono<Health> doHealthCheck(Builder builder) {
				return Mono.just(builder.down().withException(new Error("Test error")).build());
			}
		}.health().block();
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(output).contains("Health check failed").contains("Test error");
	}

}
