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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.pulsar.config.PulsarAnnotationSupportBeanNames;
import org.springframework.pulsar.core.SchemaResolver;
import org.springframework.pulsar.core.TopicResolver;
import org.springframework.pulsar.reactive.config.DefaultReactivePulsarListenerContainerFactory;
import org.springframework.pulsar.reactive.config.annotation.EnableReactivePulsar;
import org.springframework.pulsar.reactive.core.ReactivePulsarConsumerFactory;
import org.springframework.pulsar.reactive.listener.ReactivePulsarContainerProperties;

/**
 * Configuration for Reactive Pulsar annotation-driven support.
 *
 * @author Christophe Bornet
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EnableReactivePulsar.class)
class PulsarReactiveAnnotationDrivenConfiguration {

	private final PulsarReactiveProperties properties;

	PulsarReactiveAnnotationDrivenConfiguration(PulsarReactiveProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(name = "reactivePulsarListenerContainerFactory")
	DefaultReactivePulsarListenerContainerFactory<?> reactivePulsarListenerContainerFactory(
			ObjectProvider<ReactivePulsarConsumerFactory<Object>> consumerFactoryProvider,
			SchemaResolver schemaResolver, TopicResolver topicResolver) {

		ReactivePulsarContainerProperties<Object> containerProperties = new ReactivePulsarContainerProperties<>();
		containerProperties.setSchemaResolver(schemaResolver);
		containerProperties.setTopicResolver(topicResolver);
		containerProperties.setSubscriptionType(this.properties.getConsumer().getSubscription().getType());

		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		PulsarReactiveProperties.Listener listenerProperties = this.properties.getListener();
		map.from(listenerProperties::getSchemaType).to(containerProperties::setSchemaType);
		map.from(listenerProperties::getHandlingTimeout).to(containerProperties::setHandlingTimeout);
		map.from(listenerProperties::getUseKeyOrderedProcessing).to(containerProperties::setUseKeyOrderedProcessing);

		return new DefaultReactivePulsarListenerContainerFactory<>(consumerFactoryProvider.getIfAvailable(),
				containerProperties);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableReactivePulsar
	@ConditionalOnMissingBean(
			name = PulsarAnnotationSupportBeanNames.REACTIVE_PULSAR_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
	static class EnableReactivePulsarConfiguration {

	}

}
