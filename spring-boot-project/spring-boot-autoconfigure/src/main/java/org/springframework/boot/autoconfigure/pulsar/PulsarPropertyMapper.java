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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.pulsar.client.admin.PulsarAdminBuilder;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.ProducerBuilder;
import org.apache.pulsar.client.api.PulsarClientException.UnsupportedAuthenticationException;
import org.apache.pulsar.client.api.ReaderBuilder;

import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Authentication;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.pulsar.core.ConsumerBuilderCustomizer;
import org.springframework.pulsar.core.ProducerBuilderCustomizer;
import org.springframework.pulsar.core.PulsarAdminBuilderCustomizer;
import org.springframework.pulsar.core.PulsarClientBuilderCustomizer;
import org.springframework.pulsar.core.ReaderBuilderCustomizer;
import org.springframework.pulsar.listener.PulsarContainerProperties;
import org.springframework.pulsar.reader.PulsarReaderContainerProperties;
import org.springframework.util.StringUtils;

/**
 * Helper class used to map {@link PulsarProperties} to various builder customizers.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
final class PulsarPropertyMapper {

	private PulsarPropertyMapper() {
	}

	static PulsarClientBuilderCustomizer clientBuilderCustomizer(PulsarProperties properties) {
		return (clientBuilder) -> customizeClientBuilder(clientBuilder, properties.getClient());
	}

	private static void customizeClientBuilder(ClientBuilder clientBuilder, PulsarProperties.Client properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getServiceUrl).to(clientBuilder::serviceUrl);
		map.from(properties::getConnectionTimeout).to(timeoutProperty(clientBuilder::connectionTimeout));
		map.from(properties::getOperationTimeout).to(timeoutProperty(clientBuilder::operationTimeout));
		map.from(properties::getLookupTimeout).to(timeoutProperty(clientBuilder::lookupTimeout));
		customizeAuthentication(clientBuilder::authentication, properties.getAuthentication());
	}

	static PulsarAdminBuilderCustomizer adminBuilderCustomizer(PulsarProperties properties) {
		return (adminBuilder) -> customizerAdminBuilder(adminBuilder, properties.getAdmin());
	}

	private static void customizerAdminBuilder(PulsarAdminBuilder adminBuilder, PulsarProperties.Admin properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getServiceUrl).to(adminBuilder::serviceHttpUrl);
		map.from(properties::getConnectionTimeout).to(timeoutProperty(adminBuilder::connectionTimeout));
		map.from(properties::getReadTimeout).to(timeoutProperty(adminBuilder::readTimeout));
		map.from(properties::getRequestTimeout).to(timeoutProperty(adminBuilder::requestTimeout));
		customizeAuthentication(adminBuilder::authentication, properties.getAuthentication());
	}

	private static void customizeAuthentication(AuthenticationConsumer authentication, Authentication properties) {
		if (StringUtils.hasText(properties.getPluginClassName())) {
			try {
				authentication.accept(properties.getPluginClassName(), properties.getParam());
			}
			catch (UnsupportedAuthenticationException ex) {
				throw new IllegalStateException("Unable to configure Pulsar authentication", ex);
			}
		}
	}

	static <T> ProducerBuilderCustomizer<T> producerBuilderCustomizer(PulsarProperties properties) {
		return (producerBuilder) -> customizeProducerBuilder(producerBuilder, properties.getProducer());
	}

	private static <T> void customizeProducerBuilder(ProducerBuilder<T> producerBuilder,
			PulsarProperties.Producer properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getName).to(producerBuilder::producerName);
		map.from(properties::getTopicName).to(producerBuilder::topic);
		map.from(properties::getSendTimeout).to(timeoutProperty(producerBuilder::sendTimeout));
		map.from(properties::getMessageRoutingMode).to(producerBuilder::messageRoutingMode);
		map.from(properties::getHashingScheme).to(producerBuilder::hashingScheme);
		map.from(properties::isBatchingEnabled).to(producerBuilder::enableBatching);
		map.from(properties::isChunkingEnabled).to(producerBuilder::enableChunking);
		map.from(properties::getCompressionType).to(producerBuilder::compressionType);
		map.from(properties::getAccessMode).to(producerBuilder::accessMode);
	}

	static <T> ConsumerBuilderCustomizer<T> consumerBuilderCustomizer(PulsarProperties properties) {
		return (consumerBuilder) -> customizeConsumerBuilder(consumerBuilder, properties.getConsumer());
	}

	private static <T> void customizeConsumerBuilder(ConsumerBuilder<T> consumerBuilder,
			PulsarProperties.Consumer properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getName).to(consumerBuilder::consumerName);
		map.from(properties::getTopics).as(ArrayList::new).to(consumerBuilder::topics);
		map.from(properties::getTopicsPattern).to(consumerBuilder::topicsPattern);
		map.from(properties::getPriorityLevel).to(consumerBuilder::priorityLevel);
		map.from(properties::isReadCompacted).to(consumerBuilder::readCompacted);
		map.from(properties::getDeadLetterPolicy).as(DeadLetterPolicyMapper::map).to(consumerBuilder::deadLetterPolicy);
		map.from(properties::isRetryEnable).to(consumerBuilder::enableRetry);
		customizeConsumerBuilderSubscription(consumerBuilder, properties.getSubscription());
	}

	private static void customizeConsumerBuilderSubscription(ConsumerBuilder<?> consumerBuilder,
			PulsarProperties.Consumer.Subscription properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getName).to(consumerBuilder::subscriptionName);
		map.from(properties::getInitialPosition).to(consumerBuilder::subscriptionInitialPosition);
		map.from(properties::getMode).to(consumerBuilder::subscriptionMode);
		map.from(properties::getTopicsMode).to(consumerBuilder::subscriptionTopicsMode);
		map.from(properties::getType).to(consumerBuilder::subscriptionType);
	}

	static Consumer<PulsarContainerProperties> containerPropertiesCustomizer(PulsarProperties properties) {
		return (containerProperties) -> customizeContainerProperties(containerProperties, properties);
	}

	private static void customizeContainerProperties(PulsarContainerProperties containerProperties,
			PulsarProperties properties) {
		customizePulsarContainerConsumerSubscriptionProperties(containerProperties,
				properties.getConsumer().getSubscription());
		customizePulsarContainerListenerProperties(containerProperties, properties.getListener());
	}

	private static void customizePulsarContainerConsumerSubscriptionProperties(
			PulsarContainerProperties containerProperties, PulsarProperties.Consumer.Subscription properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getType).to(containerProperties::setSubscriptionType);
	}

	private static void customizePulsarContainerListenerProperties(PulsarContainerProperties containerProperties,
			PulsarProperties.Listener properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getSchemaType).to(containerProperties::setSchemaType);
		map.from(properties::isObservationEnabled).to(containerProperties::setObservationEnabled);
	}

	static <T> ReaderBuilderCustomizer<T> readerBuilderCustomizer(PulsarProperties properties) {
		return (readerBuilder) -> customizeReaderBuilder(readerBuilder, properties.getReader());
	}

	private static <T> void customizeReaderBuilder(ReaderBuilder<T> readerBuilder, PulsarProperties.Reader properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getName).to(readerBuilder::readerName);
		map.from(properties::getTopics).to(readerBuilder::topics);
		map.from(properties::getSubscriptionName).to(readerBuilder::subscriptionName);
		map.from(properties::getSubscriptionRolePrefix).to(readerBuilder::subscriptionRolePrefix);
		map.from(properties::isReadCompacted).to(readerBuilder::readCompacted);
	}

	static Consumer<PulsarReaderContainerProperties> readerContainerPropertiesCustomizer(PulsarProperties properties) {
		return (readerContainerProperties) -> customizeReaderContainerProperties(readerContainerProperties,
				properties.getReader());
	}

	private static void customizeReaderContainerProperties(PulsarReaderContainerProperties readerContainerProperties,
			PulsarProperties.Reader properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getTopics).to(readerContainerProperties::setTopics);
	}

	private static Consumer<Duration> timeoutProperty(BiConsumer<Integer, TimeUnit> setter) {
		return (duration) -> setter.accept((int) duration.toMillis(), TimeUnit.MILLISECONDS);
	}

	private interface AuthenticationConsumer {

		void accept(String authPluginClassName, Map<String, String> authParams)
				throws UnsupportedAuthenticationException;

	}

}
