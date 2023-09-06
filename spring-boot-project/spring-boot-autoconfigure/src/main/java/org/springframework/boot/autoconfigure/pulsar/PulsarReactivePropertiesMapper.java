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

import org.apache.pulsar.reactive.client.api.ReactiveMessageConsumerBuilder;
import org.apache.pulsar.reactive.client.api.ReactiveMessageReaderBuilder;
import org.apache.pulsar.reactive.client.api.ReactiveMessageSenderBuilder;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.pulsar.reactive.listener.ReactivePulsarContainerProperties;

/**
 * Helper class used to map reactive {@link PulsarProperties} to various builder
 * customizers.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
final class PulsarReactivePropertiesMapper {

	private final PulsarProperties properties;

	PulsarReactivePropertiesMapper(PulsarProperties properties) {
		this.properties = properties;
	}

	<T> void customizeMessageSenderBuilder(ReactiveMessageSenderBuilder<T> builder) {
		PulsarProperties.Producer properties = this.properties.getProducer();
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

	<T> void customizeMessageConsumerBuilder(ReactiveMessageConsumerBuilder<T> builder) {
		PulsarProperties.Consumer properties = this.properties.getConsumer();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getName).to(builder::consumerName);
		map.from(properties::getTopics).as(ArrayList::new).to(builder::topics);
		map.from(properties::getTopicsPattern).to(builder::topicsPattern);
		map.from(properties::getPriorityLevel).to(builder::priorityLevel);
		map.from(properties::isReadCompacted).to(builder::readCompacted);
		map.from(properties::getDeadLetterPolicy).as(DeadLetterPolicyMapper::map).to(builder::deadLetterPolicy);
		map.from(properties::isRetryEnable).to(builder::retryLetterTopicEnable);
		customizerMessageConsumerBuilderSubscription(builder);
	}

	private <T> void customizerMessageConsumerBuilderSubscription(ReactiveMessageConsumerBuilder<T> builder) {
		PulsarProperties.Consumer.Subscription properties = this.properties.getConsumer().getSubscription();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getName).to(builder::subscriptionName);
		map.from(properties::getInitialPosition).to(builder::subscriptionInitialPosition);
		map.from(properties::getMode).to(builder::subscriptionMode);
		map.from(properties::getTopicsMode).to(builder::topicsPatternSubscriptionMode);
		map.from(properties::getType).to(builder::subscriptionType);
	}

	<T> void customizeContainerProperties(ReactivePulsarContainerProperties<T> containerProperties) {
		customizePulsarContainerConsumerSubscriptionProperties(containerProperties);
		customizePulsarContainerListenerProperties(containerProperties);
	}

	private void customizePulsarContainerConsumerSubscriptionProperties(
			ReactivePulsarContainerProperties<?> containerProperties) {
		PulsarProperties.Consumer.Subscription properties = this.properties.getConsumer().getSubscription();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getType).to(containerProperties::setSubscriptionType);
	}

	private void customizePulsarContainerListenerProperties(ReactivePulsarContainerProperties<?> containerProperties) {
		PulsarProperties.Listener properties = this.properties.getListener();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getSchemaType).to(containerProperties::setSchemaType);
	}

	void customizeMessageReaderBuilder(ReactiveMessageReaderBuilder<?> builder) {
		PulsarProperties.Reader properties = this.properties.getReader();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getName).to(builder::readerName);
		map.from(properties::getTopics).to(builder::topics);
		map.from(properties::getSubscriptionName).to(builder::subscriptionName);
		map.from(properties::getSubscriptionRolePrefix).to(builder::generatedSubscriptionNamePrefix);
		map.from(properties::isReadCompacted).to(builder::readCompacted);
	}

}
