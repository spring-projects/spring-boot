/*
 * Copyright 2022-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.kafka;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.SenderOptions;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveKafkaAutoConfiguration}.
 *
 * @author Chris Bono
 */
class ReactiveKafkaAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class))
			.withConfiguration(AutoConfigurations.of(ReactiveKafkaAutoConfiguration.class));

	@Test
	void receiverOptionsFullySpecified() {
		this.contextRunner.withPropertyValues(
				"spring.kafka.reactor.receiver.commit-interval=2000",
				"spring.kafka.reactor.receiver.close-timeout=1500",
				"spring.kafka.reactor.receiver.commit-batch-size=100",
				"spring.kafka.reactor.receiver.poll-timeout=1000",
				"spring.kafka.reactor.receiver.atmost-once-commit-ahead-size=42",
				"spring.kafka.reactor.receiver.max-commit-attempts=3",
				"spring.kafka.reactor.receiver.max-deferred-commits=5",
				"spring.kafka.reactor.receiver.subscribe-topics=foo,bar").run((context) -> {
			ReceiverOptions<?, ?> receiverOptions = context.getBean(ReceiverOptions.class);
			assertThat(receiverOptions.commitInterval()).isEqualTo(Duration.ofSeconds(2));
			assertThat(receiverOptions.closeTimeout()).isEqualTo(Duration.ofMillis(1500));
			assertThat(receiverOptions.commitBatchSize()).isEqualTo(100);
			assertThat(receiverOptions.pollTimeout()).isEqualTo(Duration.ofSeconds(1));
			assertThat(receiverOptions.atmostOnceCommitAheadSize()).isEqualTo(42);
			assertThat(receiverOptions.maxCommitAttempts()).isEqualTo(3);
			assertThat(receiverOptions.maxDeferredCommits()).isEqualTo(5);
			assertThat(receiverOptions.subscriptionTopics()).containsAll(Arrays.asList("foo", "bar"));
			// TOOD set/assert the consumer properties as well
		});
	}

	@Test
	void receiverOptionsWithDefaultValues() {
		this.contextRunner.withPropertyValues().run((context) -> {
			ReceiverOptions<?, ?> receiverOptions = context.getBean(ReceiverOptions.class);
			assertThat(receiverOptions.pollTimeout()).isEqualTo(Duration.ofMillis(100));
			assertThat(receiverOptions.maxCommitAttempts()).isEqualTo(100);
			assertThat(receiverOptions.commitRetryInterval()).isEqualTo(Duration.ofMillis(500));
			assertThat(receiverOptions.commitInterval()).isEqualTo(Duration.ofSeconds(5));
			// TOOD assert the default consumer properties as well
		});
	}

	@Test
	void receiverOptionsWithSubscribePattern() {
		this.contextRunner.withPropertyValues("spring.kafka.reactor.receiver.subscribe-pattern=myTopic.+")
				.run((context) -> {
					ReceiverOptions<?, ?> receiverOptions = context.getBean(ReceiverOptions.class);
					assertThat(receiverOptions.subscriptionPattern())
							.matches((p) -> Objects.equals(Pattern.compile("myTopic.+").pattern(), p.pattern()));
				});
	}

	@Test
	void receiverOptionsWithSubscribeTopicsAndPatternsThrowsException() {
		this.contextRunner.withPropertyValues(
						"spring.kafka.reactor.receiver.subscribe-topics=foo,bar", "spring.kafka.reactor.receiver.subscribe-pattern=myTopic.+")
				.run((context) -> assertThat(context).hasFailed().getFailure().getRootCause()
						.isInstanceOf(MutuallyExclusiveConfigurationPropertiesException.class)
						.hasMessageContaining("spring.kafka.reactor.receiver.subscribe-topics")
						.hasMessageContaining("spring.kafka.reactor.receiver.subscribe-pattern"));
	}

	@Test
	void senderOptionsFullySpecified() {
		this.contextRunner.withPropertyValues(
						"spring.kafka.reactor.sender.max-in-flight=1500",
						"spring.kafka.reactor.sender.stop-on-error=false",
						"spring.kafka.reactor.sender.close-timeout=500")
				.run((context) -> {
					SenderOptions<?, ?> senderOptions = context.getBean(SenderOptions.class);
					assertThat(senderOptions.maxInFlight()).isEqualTo(1500);
					assertThat(senderOptions.stopOnError()).isFalse();
					assertThat(senderOptions.closeTimeout()).isEqualTo(Duration.ofMillis(500));
					// TODO set/asser the producer properties as well
				});
	}

	@Test
	void senderOptionsWithDefaultValues() {

	}

	// TODO other tests for hierarchy overrides w/ spring.kafka.* 'parent' properties
}
