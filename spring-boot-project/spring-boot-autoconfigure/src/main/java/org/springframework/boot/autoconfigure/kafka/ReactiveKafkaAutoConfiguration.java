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

import java.util.NoSuchElementException;
import java.util.function.Function;

import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.SenderOptions;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.context.annotation.Bean;

/**
 * {@link AutoConfiguration} for ReactorKafka.
 *
 * @author Chris Bono
 * @since 2.7.0
 */
@AutoConfiguration
@ConditionalOnClass({ ReceiverOptions.class, SenderOptions.class })
@EnableConfigurationProperties(KafkaProperties.class)
public class ReactiveKafkaAutoConfiguration {

	private final KafkaProperties kafkaProperties;

	public ReactiveKafkaAutoConfiguration(KafkaProperties kafkaProperties) {
		this.kafkaProperties = kafkaProperties;
	}

	@Bean
	@ConditionalOnMissingBean(ReceiverOptions.class)
	public <K, V> ReceiverOptions<K, V> receiverOptions() {
		KafkaProperties.Reactor.Receiver receiverProperties = this.kafkaProperties.getReactor().getReceiver();
		MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
			entries.put("spring.kafka.reactor.receiver.subscribe-topics", receiverProperties.getSubscribeTopics());
			entries.put("spring.kafka.reactor.receiver.subscribe-pattern", receiverProperties.getSubscribePattern());
		});
		ReceiverOptions<K, V> receiverOptions = ReceiverOptions.create(this.kafkaProperties.buildReactorConsumerProperties());

		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		receiverOptions = toInstanceSafe(map.from(receiverProperties::getCloseTimeout), receiverOptions::closeTimeout, receiverOptions);
		receiverOptions = toInstanceSafe(map.from(receiverProperties::getPollTimeout), receiverOptions::pollTimeout, receiverOptions);
		receiverOptions = toInstanceSafe(map.from(receiverProperties::getCommitInterval), receiverOptions::commitInterval, receiverOptions);
		receiverOptions = toInstanceSafe(map.from(receiverProperties::getSubscribeTopics), receiverOptions::subscription, receiverOptions);
		receiverOptions = toInstanceSafe(map.from(receiverProperties::getSubscribePattern), receiverOptions::subscription, receiverOptions);

		map = map.alwaysApplying(this::whenPositveInt);
		receiverOptions = toInstanceSafe(map.from(receiverProperties::getAtmostOnceCommitAheadSize), receiverOptions::atmostOnceCommitAheadSize, receiverOptions);
		receiverOptions = toInstanceSafe(map.from(receiverProperties::getMaxDeferredCommits), receiverOptions::maxDeferredCommits, receiverOptions);
		receiverOptions = toInstanceSafe(map.from(receiverProperties::getMaxCommitAttempts), receiverOptions::maxCommitAttempts, receiverOptions);
		receiverOptions = toInstanceSafe(map.from(receiverProperties::getCommitBatchSize), receiverOptions::commitBatchSize, receiverOptions);
		return receiverOptions;
	}

	@Bean
	@ConditionalOnMissingBean(SenderOptions.class)
	public <K, V> SenderOptions<K, V> senderOptions() {
		SenderOptions<K, V> senderOptions = SenderOptions.create(this.kafkaProperties.buildReactorProducerProperties());
		KafkaProperties.Reactor.Sender senderProperties = this.kafkaProperties.getReactor().getSender();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		senderOptions = toInstanceSafe(map.from(senderProperties::getCloseTimeout), senderOptions::closeTimeout, senderOptions);
		senderOptions = toInstanceSafe(map.from(senderProperties::getMaxInFlight), senderOptions::maxInFlight, senderOptions);
		senderOptions = toInstanceSafe(map.from(senderProperties::isStopOnError), senderOptions::stopOnError, senderOptions);
		return senderOptions;
	}

	private <T, R> R toInstanceSafe(PropertyMapper.Source<T> source, Function<T, R> target, R defaultOptions) {
		try {
			return source.toInstance(target);
		} catch (NoSuchElementException ex) {
			return defaultOptions;
		}
	}

	private <T> PropertyMapper.Source<T> whenPositveInt(PropertyMapper.Source<T> source) {
		return source.when(Integer.class::isInstance).when((i) -> (Integer) i > 0);
	}
}
