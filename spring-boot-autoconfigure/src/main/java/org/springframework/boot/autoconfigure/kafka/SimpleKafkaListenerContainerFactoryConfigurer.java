/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.kafka;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Listener;
import org.springframework.kafka.config.SimpleKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

/**
 * Default configurer for Kafka listener container factories.
 *
 * @author Gary Russell
 * @since 1.4
 *
 */
public class SimpleKafkaListenerContainerFactoryConfigurer {

	private KafkaProperties properties;

	/**
	 * Set the {@link KafkaProperties} to use.
	 * @param properties the properties
	 */
	public void setKafkaProperties(KafkaProperties properties) {
		this.properties = properties;
	}

	/**
	 * Configure the specified Kafka listener container factory. The factory can be
	 * further tuned and default settings can be overridden.
	 * @param listenerContainerFactory the {@link SimpleKafkaListenerContainerFactory} instance to
	 * configure
	 * @param consumerFactory the {@link ConsumerFactory} to use
	 */
	public void configure(SimpleKafkaListenerContainerFactory<Object, Object> listenerContainerFactory,
			ConsumerFactory<Object, Object> consumerFactory) {
		listenerContainerFactory.setConsumerFactory(consumerFactory);
		Listener container = this.properties.getListener();
		if (container.getAckMode() != null) {
			listenerContainerFactory.setAckMode(container.getAckMode());
		}
		if (container.getConcurrency() != null) {
			listenerContainerFactory.setConcurrency(container.getConcurrency());
		}
		if (container.getPollTimeout() != null) {
			listenerContainerFactory.setPollTimeout(container.getPollTimeout());
		}
	}

}
