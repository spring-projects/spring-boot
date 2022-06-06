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

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Common Spring Kafka configuration properties - including bootstrap servers and producer and consumer
 * Kafka client properties.
 *
 * @author Chris Bono
 * @since 2.7.0
 */
class KafkaPropertiesBaseWithBootstrapServersAndProducerConsumer extends KafkaPropertiesBaseWithBootstrapServers {

	@NestedConfigurationProperty
	private final Consumer consumer = new Consumer();

	@NestedConfigurationProperty
	private final Producer producer = new Producer();

	KafkaPropertiesBaseWithBootstrapServersAndProducerConsumer(List<String> defaultBootstrapServers) {
		super(defaultBootstrapServers);
	}

	public Consumer getConsumer() {
		return this.consumer;
	}

	public Producer getProducer() {
		return this.producer;
	}

	/**
	 * Create an initial map of consumer properties from the state of this instance.
	 * <p>
	 * This allows you to add additional properties, if necessary, and override the
	 * default kafkaConsumerFactory bean.
	 * @return the consumer properties initialized with the customizations defined on this
	 * instance
	 */
	public Map<String, Object> buildConsumerProperties() {
		// <prefix>.<common-props> (eg. spring.kafka.client-id)
		Map<String, Object> properties = buildProperties();

		// <prefix>.consumer.<common-props> (eg. spring.kafka.consumer.client-id)
		// <prefix>.consumer.<specific-props> (eg. spring.kafka.consumer.fetch-min-size)
		properties.putAll(this.consumer.buildProperties());
		return properties;
	}

	/**
	 * Create an initial map of producer properties from the state of this instance.
	 * <p>
	 * This allows you to add additional properties, if necessary, and override the
	 * default kafkaProducerFactory bean.
	 * @return the producer properties initialized with the customizations defined on this
	 * instance
	 */
	public Map<String, Object> buildProducerProperties() {
		// <prefix>.<common-props>
		Map<String, Object> properties = buildProperties();

		// <prefix>.producer.<common-props->specific-props>
		properties.putAll(this.producer.buildProperties());
		return properties;
	}

}
