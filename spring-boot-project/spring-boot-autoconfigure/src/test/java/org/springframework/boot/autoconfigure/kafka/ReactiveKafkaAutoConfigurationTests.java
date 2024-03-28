/*
 * Copyright 2012-2022 the original author or authors.
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
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveKafkaAutoConfiguration}.
 *
 * @author Almog Tavor
 */
class ReactiveKafkaAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class))
			.withConfiguration(AutoConfigurations.of(ReactiveKafkaAutoConfiguration.class));

	@Test
	void receiverProperties() {
		this.contextRunner.withPropertyValues("spring.reactor.kafka.receiver.commit-interval=2000",
				"spring.reactor.kafka.receiver.close-timeout=1500",
				"spring.reactor.kafka.receiver.commit-batch-size=100",
				"spring.reactor.kafka.receiver.poll-timeout=1000",
				"spring.reactor.kafka.receiver.atmost-once-commit-ahead-size=42",
				"spring.reactor.kafka.receiver.max-commit-attempts=3",
				"spring.reactor.kafka.receiver.max-deferred-commits=5",
				"spring.reactor.kafka.receiver.subscribe-topics=foo,bar").run((context) -> {
					ReceiverOptions<?, ?> receiverOptions = context.getBean(ReceiverOptions.class);
					assertThat(receiverOptions.commitInterval()).isEqualTo(Duration.ofSeconds(2));
					assertThat(receiverOptions.closeTimeout()).isEqualTo(Duration.ofMillis(1500));
					assertThat(receiverOptions.commitBatchSize()).isEqualTo(100);
					assertThat(receiverOptions.pollTimeout()).isEqualTo(Duration.ofSeconds(1));
					assertThat(receiverOptions.atmostOnceCommitAheadSize()).isEqualTo(42);
					assertThat(receiverOptions.maxCommitAttempts()).isEqualTo(3);
					assertThat(receiverOptions.maxDeferredCommits()).isEqualTo(5);
					assertThat(receiverOptions.subscriptionTopics()).containsAll(Arrays.asList("foo", "bar"));
				});
	}

	@Test
	void receiverPropertiesSubscribePattern() {
		this.contextRunner.withPropertyValues("spring.reactor.kafka.receiver.subscribe-pattern=myTopic.+")
				.run((context) -> {
					ReceiverOptions<?, ?> receiverOptions = context.getBean(ReceiverOptions.class);
					assertThat(receiverOptions.subscriptionPattern())
							.matches((p) -> Objects.equals(Pattern.compile("myTopic.+").pattern(), p.pattern()));
				});
	}

	@Test
	void receiverPropertiesDefaultValues() {
		this.contextRunner.withPropertyValues().run((context) -> {
			ReceiverOptions<?, ?> receiverOptions = context.getBean(ReceiverOptions.class);
			assertThat(receiverOptions.commitInterval()).isEqualTo(Duration.ofSeconds(5));
		});
	}

	@Test
	void producerProperties() {
		this.contextRunner.withPropertyValues("spring.reactor.kafka.sender.max-in-flight=1500",
				"spring.reactor.kafka.sender.stop-on-error=false", "spring.reactor.kafka.sender.close-timeout=500")
				.run((context) -> {
					SenderOptions<?, ?> senderOptions = context.getBean(SenderOptions.class);
					assertThat(senderOptions.maxInFlight()).isEqualTo(1500);
					assertThat(senderOptions.stopOnError()).isFalse();
					assertThat(senderOptions.closeTimeout()).isEqualTo(Duration.ofMillis(500));
				});
	}

}
