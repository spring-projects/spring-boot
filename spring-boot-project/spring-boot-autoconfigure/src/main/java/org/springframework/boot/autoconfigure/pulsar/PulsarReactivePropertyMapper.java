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

package org.springframework.boot.autoconfigure.pulsar;

import java.util.ArrayList;
import java.util.function.Consumer;

import org.apache.pulsar.reactive.client.api.ReactiveMessageConsumerBuilder;
import org.apache.pulsar.reactive.client.api.ReactiveMessageReaderBuilder;
import org.apache.pulsar.reactive.client.api.ReactiveMessageSenderBuilder;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.pulsar.reactive.core.ReactiveMessageConsumerBuilderCustomizer;
import org.springframework.pulsar.reactive.core.ReactiveMessageReaderBuilderCustomizer;
import org.springframework.pulsar.reactive.core.ReactiveMessageSenderBuilderCustomizer;
import org.springframework.pulsar.reactive.listener.ReactivePulsarContainerProperties;

/**
 * Helper class used to map reactive {@link PulsarProperties} to various builder
 * customizers.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
final class PulsarReactivePropertyMapper {

	private PulsarReactivePropertyMapper() {
	}

	static <T> ReactiveMessageSenderBuilderCustomizer<T> messageSenderBuilderCustomizer(PulsarProperties properties) {
		return (builder) -> customizeMessageSenderBuilder(builder, properties.getProducer());
	}

	private static <T> void customizeMessageSenderBuilder(ReactiveMessageSenderBuilder<T> builder,
			PulsarProperties.Producer properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getName).to(builder::producerName);
		map.from(properties::getTopicName).to(builder::topic);
		map.from(properties::getSendTimeout).to(builder::sendTimeout);
		map.from(properties::getMessageRoutingMode).to(builder::messageRoutingMode);
		map.from(properties::getHashingScheme).to(builder::hashingScheme);
		map.from(properties::isBatchingEnabled).to(builder::batchingEnabled);
		map.from(properties::isChunkingEnabled).to(builder::chunkingEnabled);
		map.from(properties::getCompressionType).to(builder::compressionType);
		map.from(properties::getAccessMode).to(builder::accessMode);
	}

	static <T> ReactiveMessageConsumerBuilderCustomizer<T> messageConsumerBuilderCustomizer(
			PulsarProperties properties) {
		return (builder) -> customizerMessageConsumerBuilder(builder, properties.getConsumer());
	}

	private static void customizerMessageConsumerBuilder(ReactiveMessageConsumerBuilder<?> builder,
			PulsarProperties.Consumer properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getName).to(builder::consumerName);
		map.from(properties::getTopics).as(ArrayList::new).to(builder::topics);
		map.from(properties::getTopicsPattern).to(builder::topicsPattern);
		map.from(properties::getPriorityLevel).to(builder::priorityLevel);
		map.from(properties::isReadCompacted).to(builder::readCompacted);
		map.from(properties::getDeadLetterPolicy).as(DeadLetterPolicyMapper::map).to(builder::deadLetterPolicy);
		map.from(properties::isRetryEnable).to(builder::retryLetterTopicEnable);
		customizerMessageConsumerBuilderSubscription(builder, properties.getSubscription());
	}

	private static void customizerMessageConsumerBuilderSubscription(ReactiveMessageConsumerBuilder<?> builder,
			PulsarProperties.Consumer.Subscription properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getName).to(builder::subscriptionName);
		map.from(properties::getInitialPosition).to(builder::subscriptionInitialPosition);
		map.from(properties::getMode).to(builder::subscriptionMode);
		map.from(properties::getTopicsMode).to(builder::topicsPatternSubscriptionMode);
		map.from(properties::getType).to(builder::subscriptionType);
	}

	static Consumer<ReactivePulsarContainerProperties<?>> containerPropertiesCustomizer(PulsarProperties properties) {
		return (containerProperties) -> customizeContainerProperties(containerProperties, properties);
	}

	private static void customizeContainerProperties(ReactivePulsarContainerProperties<?> containerProperties,
			PulsarProperties properties) {
		customizePulsarContainerConsumerSubscriptionProperties(containerProperties,
				properties.getConsumer().getSubscription());
		customizePulsarContainerListenerProperties(containerProperties, properties.getListener());
	}

	private static void customizePulsarContainerConsumerSubscriptionProperties(
			ReactivePulsarContainerProperties<?> containerProperties,
			PulsarProperties.Consumer.Subscription properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getType).to(containerProperties::setSubscriptionType);
	}

	private static void customizePulsarContainerListenerProperties(
			ReactivePulsarContainerProperties<?> containerProperties, PulsarProperties.Listener properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getSchemaType).to(containerProperties::setSchemaType);
	}

	static <T> ReactiveMessageReaderBuilderCustomizer<T> messageReaderBuilderCustomizer(PulsarProperties properties) {
		return (builder) -> customizeMessageReaderBuilder(builder, properties.getReader());
	}

	private static void customizeMessageReaderBuilder(ReactiveMessageReaderBuilder<?> builder,
			PulsarProperties.Reader properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getName).to(builder::readerName);
		map.from(properties::getTopics).to(builder::topics);
		map.from(properties::getSubscriptionName).to(builder::subscriptionName);
		map.from(properties::getSubscriptionRolePrefix).to(builder::generatedSubscriptionNamePrefix);
		map.from(properties::isReadCompacted).to(builder::readCompacted);
	}

}
