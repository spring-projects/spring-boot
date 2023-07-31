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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.pulsar.annotation.EnablePulsar;
import org.springframework.pulsar.config.ConcurrentPulsarListenerContainerFactory;
import org.springframework.pulsar.config.DefaultPulsarReaderContainerFactory;
import org.springframework.pulsar.config.PulsarAnnotationSupportBeanNames;
import org.springframework.pulsar.core.PulsarConsumerFactory;
import org.springframework.pulsar.core.PulsarReaderFactory;
import org.springframework.pulsar.core.SchemaResolver;
import org.springframework.pulsar.core.TopicResolver;
import org.springframework.pulsar.listener.PulsarContainerProperties;
import org.springframework.pulsar.reader.PulsarReaderContainerProperties;
import org.springframework.util.unit.DataSize;

/**
 * Configuration for Pulsar annotation-driven support.
 *
 * @author Soby Chacko
 * @author Chris Bono
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EnablePulsar.class)
class PulsarAnnotationDrivenConfiguration {

	private final PulsarProperties pulsarProperties;

	PulsarAnnotationDrivenConfiguration(PulsarProperties pulsarProperties) {
		this.pulsarProperties = pulsarProperties;
	}

	@Bean
	@ConditionalOnMissingBean(name = "pulsarListenerContainerFactory")
	ConcurrentPulsarListenerContainerFactory<?> pulsarListenerContainerFactory(
			ObjectProvider<PulsarConsumerFactory<Object>> consumerFactoryProvider, SchemaResolver schemaResolver,
			TopicResolver topicResolver) {

		PulsarContainerProperties containerProperties = new PulsarContainerProperties();
		containerProperties.setSchemaResolver(schemaResolver);
		containerProperties.setTopicResolver(topicResolver);
		containerProperties.setSubscriptionType(this.pulsarProperties.getConsumer().getSubscription().getType());
		containerProperties.setObservationEnabled(this.pulsarProperties.getListener().isObservationsEnabled());

		PulsarProperties.Listener listenerProperties = this.pulsarProperties.getListener();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(listenerProperties::getSchemaType).to(containerProperties::setSchemaType);
		map.from(listenerProperties::getAckMode).to(containerProperties::setAckMode);
		map.from(listenerProperties::getBatchTimeout)
			.asInt(Duration::toMillis)
			.to(containerProperties::setBatchTimeoutMillis);
		map.from(listenerProperties::getMaxNumBytes).asInt(DataSize::toBytes).to(containerProperties::setMaxNumBytes);
		map.from(listenerProperties::getMaxNumMessages).to(containerProperties::setMaxNumMessages);

		return new ConcurrentPulsarListenerContainerFactory<>(consumerFactoryProvider.getIfAvailable(),
				containerProperties);
	}

	@Bean
	@ConditionalOnMissingBean(name = "pulsarReaderContainerFactory")
	DefaultPulsarReaderContainerFactory<?> pulsarReaderContainerFactory(
			ObjectProvider<PulsarReaderFactory<Object>> readerFactoryProvider, SchemaResolver schemaResolver) {

		PulsarReaderContainerProperties containerProperties = new PulsarReaderContainerProperties();
		containerProperties.setSchemaResolver(schemaResolver);

		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		PulsarProperties.Reader readerProperties = this.pulsarProperties.getReader();
		map.from(readerProperties::getTopicNames).to(containerProperties::setTopics);

		return new DefaultPulsarReaderContainerFactory<>(readerFactoryProvider.getIfAvailable(), containerProperties);
	}

	@Configuration(proxyBeanMethods = false)
	@EnablePulsar
	@ConditionalOnMissingBean(name = { PulsarAnnotationSupportBeanNames.PULSAR_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME,
			PulsarAnnotationSupportBeanNames.PULSAR_READER_ANNOTATION_PROCESSOR_BEAN_NAME })
	static class EnablePulsarConfiguration {

	}

}
