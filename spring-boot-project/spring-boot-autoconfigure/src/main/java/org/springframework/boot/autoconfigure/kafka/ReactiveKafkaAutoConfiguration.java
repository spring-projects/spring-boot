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
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the Reactive client of Apache
 * Kafka.
 *
 * @author Almog Tavor
 * @since 2.7.0
 */
@AutoConfiguration
@ConditionalOnClass({KafkaReceiver.class, KafkaSender.class})
@ConditionalOnBean(KafkaProperties.class)
@EnableConfigurationProperties(ReactiveKafkaProperties.class)
public class ReactiveKafkaAutoConfiguration {
	private final KafkaProperties kafkaProperties;
	private final ReactiveKafkaProperties reactiveKafkaProperties;
	private static final PropertyMapper map = PropertyMapper.get();

	public ReactiveKafkaAutoConfiguration(KafkaProperties kafkaProperties, ReactiveKafkaProperties reactiveKafkaProperties) {
		this.kafkaProperties = kafkaProperties;
		this.reactiveKafkaProperties = reactiveKafkaProperties;
	}

	@Bean
	@ConditionalOnMissingBean(ReceiverOptions.class)
	public <K, V> ReceiverOptions<K, V> receiverOptions() {
		Map<String, Object> properties = this.kafkaProperties.buildConsumerProperties();
		ReceiverOptions<K, V> receiverOptions = ReceiverOptions.create(properties);
		ReactiveKafkaProperties.Receiver receiverProperties = this.reactiveKafkaProperties.getReceiver();
		receiverOptions = setPropertyWhenGreaterThanZero(receiverProperties.getAtmostOnceCommitAheadSize(), receiverOptions::atmostOnceCommitAheadSize, receiverOptions);
		receiverOptions = setPropertyWhenGreaterThanZero(receiverProperties.getMaxDeferredCommits(), receiverOptions::maxDeferredCommits, receiverOptions);
		receiverOptions = setPropertyWhenGreaterThanZero(receiverProperties.getMaxCommitAttempts(), receiverOptions::maxCommitAttempts, receiverOptions);
		receiverOptions = setPropertyWhenGreaterThanZero(receiverProperties.getCommitBatchSize(), receiverOptions::commitBatchSize, receiverOptions);
		receiverOptions = setPropertyWhenNonNull(receiverProperties.getCloseTimeout(), receiverOptions::closeTimeout, receiverOptions);
		receiverOptions = setPropertyWhenNonNull(receiverProperties.getPollTimeout(), receiverOptions::pollTimeout, receiverOptions);
		receiverOptions = setPropertyWhenNonNull(receiverProperties.getCommitInterval(), receiverOptions::commitInterval, receiverOptions);
		receiverOptions = setPropertyWhenNonNull(receiverProperties.getSubscribeTopics(), receiverOptions::subscription, receiverOptions);
		if (Optional.ofNullable(receiverProperties.getSubscribeTopics()).isEmpty()) {
			receiverOptions = setPropertyWhenNonNull(receiverProperties.getSubscribePattern(), receiverOptions::subscription, receiverOptions);
		}
		return receiverOptions;
	}

	@Bean
	@ConditionalOnMissingBean(SenderOptions.class)
	public <K, V> SenderOptions<K, V> senderOptions() {
		Map<String, Object> properties = kafkaProperties.buildProducerProperties();
		SenderOptions<K, V> senderOptions = SenderOptions.create(properties);
		ReactiveKafkaProperties.Sender senderProperties = this.reactiveKafkaProperties.getSender();
		senderOptions = map.from(senderProperties.getCloseTimeout()).toInstance(senderOptions::closeTimeout);
		senderOptions = setPropertyWhenGreaterThanZero(senderProperties.getMaxInFlight(), senderOptions::maxInFlight, senderOptions);
		senderOptions = map.from(senderProperties.isStopOnError()).toInstance(senderOptions::stopOnError);
		return senderOptions;
	}

	private <T> T setPropertyWhenGreaterThanZero(Integer property, Function<Integer, T> function, T options) {
		if (property <= 0) {
			return options;
		}
		return map.from(property)
				.when(i -> i > 0)
				.toInstance(function);
	}

	private <S, T> T setPropertyWhenNonNull(S property, Function<S, T> function, T options) {
		if (Optional.ofNullable(property).isEmpty()) {
			return options;
		}
		return map.from(property)
				.whenNonNull()
				.toInstance(function);
	}
}
