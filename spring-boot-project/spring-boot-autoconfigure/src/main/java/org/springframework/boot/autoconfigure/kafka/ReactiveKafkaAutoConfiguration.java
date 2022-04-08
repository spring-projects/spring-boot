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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the Reactive client of Apache
 * Kafka.
 *
 * @author Almog Tavor
 * @since 2.7.0
 */
@AutoConfiguration
@ConditionalOnClass({ KafkaReceiver.class, KafkaSender.class })
@ConditionalOnBean(KafkaProperties.class)
@EnableConfigurationProperties(ReactiveKafkaProperties.class)
public class ReactiveKafkaAutoConfiguration {

	private final KafkaProperties kafkaProperties;

	private final ReactiveKafkaProperties reactiveKafkaProperties;

	public ReactiveKafkaAutoConfiguration(KafkaProperties kafkaProperties,
			ReactiveKafkaProperties reactiveKafkaProperties) {
		this.kafkaProperties = kafkaProperties;
		this.reactiveKafkaProperties = reactiveKafkaProperties;
	}

	@Bean
	@ConditionalOnMissingBean(ReceiverOptions.class)
	public <K, V> ReceiverOptions<K, V> receiverOptions() {
		Map<String, Object> properties = kafkaProperties.buildConsumerProperties();
		properties.putAll(this.reactiveKafkaProperties.buildReceiverProperties());
		ReceiverOptions<K, V> receiverOptions = ReceiverOptions.create(properties);
		int atmostOnceCommitAheadSize = this.reactiveKafkaProperties.getReceiver().getAtmostOnceCommitAheadSize();
		if (atmostOnceCommitAheadSize >= 0) {
			receiverOptions = receiverOptions.atmostOnceCommitAheadSize(atmostOnceCommitAheadSize);
		}
		Optional<Duration> closeTimeout = Optional
				.ofNullable(this.reactiveKafkaProperties.getReceiver().getCloseTimeout());
		if (closeTimeout.isPresent()) {
			receiverOptions = receiverOptions.closeTimeout(closeTimeout.get());
		}
		Optional<Duration> pollTimeout = Optional
				.ofNullable(this.reactiveKafkaProperties.getReceiver().getPollTimeout());
		if (pollTimeout.isPresent()) {
			receiverOptions = receiverOptions.pollTimeout(pollTimeout.get());
		}
		int maxDeferredCommits = this.reactiveKafkaProperties.getReceiver().getMaxDeferredCommits();
		if (maxDeferredCommits >= 0) {
			receiverOptions = receiverOptions.maxDeferredCommits(maxDeferredCommits);
		}
		int maxCommitAttempts = this.reactiveKafkaProperties.getReceiver().getMaxCommitAttempts();
		if (maxCommitAttempts >= 0) {
			receiverOptions = receiverOptions.maxCommitAttempts(maxCommitAttempts);
		}
		int commitBatchSize = this.reactiveKafkaProperties.getReceiver().getCommitBatchSize();
		if (commitBatchSize >= 0) {
			receiverOptions = receiverOptions.commitBatchSize(commitBatchSize);
		}
		Duration commitInterval = this.reactiveKafkaProperties.getReceiver().getCommitInterval();
		if (commitInterval != null) {
			receiverOptions = receiverOptions.commitInterval(commitInterval);
		}
		Collection<String> subscribeTopics = this.reactiveKafkaProperties.getReceiver().getSubscribeTopics();
		if (subscribeTopics != null) {
			receiverOptions = receiverOptions.subscription(subscribeTopics);
		}
		else {
			Pattern subscribePattern = this.reactiveKafkaProperties.getReceiver().getSubscribePattern();
			if (subscribePattern != null) {
				receiverOptions = receiverOptions.subscription(subscribePattern);
			}
		}
		return receiverOptions;
	}

	@Bean
	@ConditionalOnMissingBean(SenderOptions.class)
	public <K, V> SenderOptions<K, V> senderOptions() {
		Map<String, Object> properties = kafkaProperties.buildProducerProperties();
		properties.putAll(this.reactiveKafkaProperties.buildSenderProperties());
		SenderOptions<K, V> senderOptions = SenderOptions.<K, V>create(properties);
		Duration closeTimeout = this.reactiveKafkaProperties.getSender().getCloseTimeout();
		if (closeTimeout != null) {
			senderOptions = senderOptions.closeTimeout(closeTimeout);
		}
		int maxInFlight = this.reactiveKafkaProperties.getSender().getMaxInFlight();
		if (maxInFlight >= 0) {
			senderOptions = senderOptions.maxInFlight(maxInFlight);
		}
		senderOptions = senderOptions.stopOnError(this.reactiveKafkaProperties.getSender().isStopOnError());
		return senderOptions;
	}

}
