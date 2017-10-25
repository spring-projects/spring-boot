/*
 * Copyright 2012-2017 the original author or authors.
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
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.kafka.support.converter.RecordMessageConverter;

/**
 * Configure {@link ConcurrentKafkaListenerContainerFactory} with sensible defaults.
 *
 * @author Gary Russell
 * @author Eddú Meléndez
 * @since 1.5.0
 */
public class ConcurrentKafkaListenerContainerFactoryConfigurer {

	private KafkaProperties properties;

	private RecordMessageConverter messageConverter;

	private KafkaTemplate<Object, Object> replyTemplate;

	/**
	 * Set the {@link KafkaProperties} to use.
	 * @param properties the properties
	 */
	void setKafkaProperties(KafkaProperties properties) {
		this.properties = properties;
	}

	/**
	 * Set the {@link RecordMessageConverter} to use.
	 * @param messageConverter the message converter
	 */
	void setMessageConverter(RecordMessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Set the {@link KafkaTemplate} to use to send replies.
	 * @param replyTemplate the reply template
	 */
	void setReplyTemplate(KafkaTemplate<Object, Object> replyTemplate) {
		this.replyTemplate = replyTemplate;
	}

	/**
	 * Configure the specified Kafka listener container factory. The factory can be
	 * further tuned and default settings can be overridden.
	 * @param listenerContainerFactory the {@link ConcurrentKafkaListenerContainerFactory}
	 * instance to configure
	 * @param consumerFactory the {@link ConsumerFactory} to use
	 */
	public void configure(
			ConcurrentKafkaListenerContainerFactory<Object, Object> listenerContainerFactory,
			ConsumerFactory<Object, Object> consumerFactory) {
		listenerContainerFactory.setConsumerFactory(consumerFactory);
		if (this.messageConverter != null) {
			listenerContainerFactory.setMessageConverter(this.messageConverter);
		}
		if (this.replyTemplate != null) {
			listenerContainerFactory.setReplyTemplate(this.replyTemplate);
		}
		Listener container = this.properties.getListener();
		ContainerProperties containerProperties = listenerContainerFactory
				.getContainerProperties();
		if (container.getAckMode() != null) {
			containerProperties.setAckMode(container.getAckMode());
		}
		if (container.getAckCount() != null) {
			containerProperties.setAckCount(container.getAckCount());
		}
		if (container.getAckTime() != null) {
			containerProperties.setAckTime(container.getAckTime());
		}
		if (container.getPollTimeout() != null) {
			containerProperties.setPollTimeout(container.getPollTimeout());
		}
		if (container.getConcurrency() != null) {
			listenerContainerFactory.setConcurrency(container.getConcurrency());
		}
		if (container.getType() == Listener.Type.BATCH) {
			listenerContainerFactory.setBatchListener(true);
		}
	}

}
