/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.pulsar.client.admin.PulsarAdminBuilder;
import org.apache.pulsar.client.api.Authentication;
import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.AutoClusterFailoverBuilder;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.ProducerBuilder;
import org.apache.pulsar.client.api.PulsarClientException.UnsupportedAuthenticationException;
import org.apache.pulsar.client.api.ReaderBuilder;
import org.apache.pulsar.client.api.ServiceUrlProvider;
import org.apache.pulsar.client.impl.AutoClusterFailover.AutoClusterFailoverBuilderImpl;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.pulsar.listener.PulsarContainerProperties;
import org.springframework.pulsar.reader.PulsarReaderContainerProperties;
import org.springframework.util.StringUtils;

/**
 * Helper class used to map {@link PulsarProperties} to various builder customizers.
 *
 * @author Chris Bono
 * @author Phillip Webb
 * @author Swamy Mavuri
 */
final class PulsarPropertiesMapper {

	private final PulsarProperties properties;

	PulsarPropertiesMapper(PulsarProperties properties) {
		this.properties = properties;
	}

	void customizeClientBuilder(ClientBuilder clientBuilder, PulsarConnectionDetails connectionDetails) {
		PulsarProperties.Client properties = this.properties.getClient();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getConnectionTimeout).to(timeoutProperty(clientBuilder::connectionTimeout));
		map.from(properties::getOperationTimeout).to(timeoutProperty(clientBuilder::operationTimeout));
		map.from(properties::getLookupTimeout).to(timeoutProperty(clientBuilder::lookupTimeout));
		map.from(this.properties.getTransaction()::isEnabled).whenTrue().to(clientBuilder::enableTransaction);
		customizeAuthentication(properties.getAuthentication(), clientBuilder::authentication);
		customizeServiceUrlProviderBuilder(clientBuilder::serviceUrl, clientBuilder::serviceUrlProvider, properties,
				connectionDetails);
	}

	private void customizeServiceUrlProviderBuilder(Consumer<String> serviceUrlConsumer,
			Consumer<ServiceUrlProvider> serviceUrlProviderConsumer, PulsarProperties.Client properties,
			PulsarConnectionDetails connectionDetails) {
		PulsarProperties.Failover failoverProperties = properties.getFailover();
		if (failoverProperties.getBackupClusters().isEmpty()) {
			serviceUrlConsumer.accept(connectionDetails.getBrokerUrl());
			return;
		}
		Map<String, Authentication> secondaryAuths = getSecondaryAuths(failoverProperties);
		AutoClusterFailoverBuilder autoClusterFailoverBuilder = new AutoClusterFailoverBuilderImpl();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(connectionDetails::getBrokerUrl).to(autoClusterFailoverBuilder::primary);
		map.from(secondaryAuths::keySet).as(ArrayList::new).to(autoClusterFailoverBuilder::secondary);
		map.from(failoverProperties::getPolicy).to(autoClusterFailoverBuilder::failoverPolicy);
		map.from(failoverProperties::getDelay).to(timeoutProperty(autoClusterFailoverBuilder::failoverDelay));
		map.from(failoverProperties::getSwitchBackDelay)
			.to(timeoutProperty(autoClusterFailoverBuilder::switchBackDelay));
		map.from(failoverProperties::getCheckInterval).to(timeoutProperty(autoClusterFailoverBuilder::checkInterval));
		map.from(secondaryAuths).to(autoClusterFailoverBuilder::secondaryAuthentication);
		serviceUrlProviderConsumer.accept(autoClusterFailoverBuilder.build());
	}

	private Map<String, Authentication> getSecondaryAuths(PulsarProperties.Failover properties) {
		Map<String, Authentication> secondaryAuths = new LinkedHashMap<>();
		properties.getBackupClusters().forEach((backupCluster) -> {
			PulsarProperties.Authentication authenticationProperties = backupCluster.getAuthentication();
			if (authenticationProperties.getPluginClassName() == null) {
				secondaryAuths.put(backupCluster.getServiceUrl(), null);
			}
			else {
				customizeAuthentication(authenticationProperties, (authPluginClassName, authParams) -> {
					Authentication authentication = AuthenticationFactory.create(authPluginClassName, authParams);
					secondaryAuths.put(backupCluster.getServiceUrl(), authentication);
				});
			}
		});
		return secondaryAuths;
	}

	void customizeAdminBuilder(PulsarAdminBuilder adminBuilder, PulsarConnectionDetails connectionDetails) {
		PulsarProperties.Admin properties = this.properties.getAdmin();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(connectionDetails::getAdminUrl).to(adminBuilder::serviceHttpUrl);
		map.from(properties::getConnectionTimeout).to(timeoutProperty(adminBuilder::connectionTimeout));
		map.from(properties::getReadTimeout).to(timeoutProperty(adminBuilder::readTimeout));
		map.from(properties::getRequestTimeout).to(timeoutProperty(adminBuilder::requestTimeout));
		customizeAuthentication(properties.getAuthentication(), adminBuilder::authentication);
	}

	private void customizeAuthentication(PulsarProperties.Authentication properties, AuthenticationConsumer action) {
		String pluginClassName = properties.getPluginClassName();
		if (StringUtils.hasText(pluginClassName)) {
			try {
				action.accept(pluginClassName, getAuthenticationParamsJson(properties.getParam()));
			}
			catch (UnsupportedAuthenticationException ex) {
				throw new IllegalStateException("Unable to configure Pulsar authentication", ex);
			}
		}
	}

	private String getAuthenticationParamsJson(Map<String, String> params) {
		Map<String, String> sortedParams = new TreeMap<>(params);
		try {
			return sortedParams.entrySet()
				.stream()
				.map((entry) -> "\"%s\":\"%s\"".formatted(entry.getKey(), escapeJson(entry.getValue())))
				.collect(Collectors.joining(",", "{", "}"));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not convert auth parameters to encoded string", ex);
		}
	}

	private String escapeJson(String raw) {
		return raw.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("/", "\\/")
			.replace("\b", "\\b")
			.replace("\t", "\\t")
			.replace("\n", "\\n")
			.replace("\f", "\\f")
			.replace("\r", "\\r");
	}

	<T> void customizeProducerBuilder(ProducerBuilder<T> producerBuilder) {
		PulsarProperties.Producer properties = this.properties.getProducer();
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

	<T> void customizeTemplate(PulsarTemplate<T> template) {
		template.transactions().setEnabled(this.properties.getTransaction().isEnabled());
	}

	<T> void customizeConsumerBuilder(ConsumerBuilder<T> consumerBuilder) {
		PulsarProperties.Consumer properties = this.properties.getConsumer();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getName).to(consumerBuilder::consumerName);
		map.from(properties::getTopics).as(ArrayList::new).to(consumerBuilder::topics);
		map.from(properties::getTopicsPattern).to(consumerBuilder::topicsPattern);
		map.from(properties::getPriorityLevel).to(consumerBuilder::priorityLevel);
		map.from(properties::isReadCompacted).to(consumerBuilder::readCompacted);
		map.from(properties::getDeadLetterPolicy).as(DeadLetterPolicyMapper::map).to(consumerBuilder::deadLetterPolicy);
		map.from(properties::isRetryEnable).to(consumerBuilder::enableRetry);
		customizeConsumerBuilderSubscription(consumerBuilder);
	}

	private void customizeConsumerBuilderSubscription(ConsumerBuilder<?> consumerBuilder) {
		PulsarProperties.Consumer.Subscription properties = this.properties.getConsumer().getSubscription();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getName).to(consumerBuilder::subscriptionName);
		map.from(properties::getInitialPosition).to(consumerBuilder::subscriptionInitialPosition);
		map.from(properties::getMode).to(consumerBuilder::subscriptionMode);
		map.from(properties::getTopicsMode).to(consumerBuilder::subscriptionTopicsMode);
		map.from(properties::getType).to(consumerBuilder::subscriptionType);
	}

	void customizeContainerProperties(PulsarContainerProperties containerProperties) {
		customizePulsarContainerConsumerSubscriptionProperties(containerProperties);
		customizePulsarContainerListenerProperties(containerProperties);
		containerProperties.transactions().setEnabled(this.properties.getTransaction().isEnabled());
	}

	private void customizePulsarContainerConsumerSubscriptionProperties(PulsarContainerProperties containerProperties) {
		PulsarProperties.Consumer.Subscription properties = this.properties.getConsumer().getSubscription();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getType).to(containerProperties::setSubscriptionType);
	}

	private void customizePulsarContainerListenerProperties(PulsarContainerProperties containerProperties) {
		PulsarProperties.Listener properties = this.properties.getListener();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getSchemaType).to(containerProperties::setSchemaType);
		map.from(properties::isObservationEnabled).to(containerProperties::setObservationEnabled);
	}

	<T> void customizeReaderBuilder(ReaderBuilder<T> readerBuilder) {
		PulsarProperties.Reader properties = this.properties.getReader();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getName).to(readerBuilder::readerName);
		map.from(properties::getTopics).to(readerBuilder::topics);
		map.from(properties::getSubscriptionName).to(readerBuilder::subscriptionName);
		map.from(properties::getSubscriptionRolePrefix).to(readerBuilder::subscriptionRolePrefix);
		map.from(properties::isReadCompacted).to(readerBuilder::readCompacted);
	}

	void customizeReaderContainerProperties(PulsarReaderContainerProperties readerContainerProperties) {
		PulsarProperties.Reader properties = this.properties.getReader();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getTopics).to(readerContainerProperties::setTopics);
	}

	private Consumer<Duration> timeoutProperty(BiConsumer<Integer, TimeUnit> setter) {
		return (duration) -> setter.accept((int) duration.toMillis(), TimeUnit.MILLISECONDS);
	}

	private interface AuthenticationConsumer {

		void accept(String authPluginClassName, String authParamString) throws UnsupportedAuthenticationException;

	}

}
