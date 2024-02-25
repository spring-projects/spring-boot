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

package org.springframework.boot.actuate.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.StreamsBuilderFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.MicrometerConsumerListener;
import org.springframework.kafka.core.MicrometerProducerListener;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.streams.KafkaStreamsMicrometerListener;

/**
 * Auto-configuration for Kafka metrics.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @since 2.1.0
 */
@AutoConfiguration(before = KafkaAutoConfiguration.class,
		after = { MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class })
@ConditionalOnClass({ KafkaClientMetrics.class, ProducerFactory.class })
@ConditionalOnBean(MeterRegistry.class)
public class KafkaMetricsAutoConfiguration {

	/**
	 * Customizes the DefaultKafkaProducerFactory by adding a listener for Kafka producer
	 * metrics.
	 * @param meterRegistry the MeterRegistry used for collecting metrics
	 * @return the DefaultKafkaProducerFactoryCustomizer that adds the listener
	 */
	@Bean
	public DefaultKafkaProducerFactoryCustomizer kafkaProducerMetrics(MeterRegistry meterRegistry) {
		return (producerFactory) -> addListener(producerFactory, meterRegistry);
	}

	/**
	 * Customizes the DefaultKafkaConsumerFactory by adding a listener to collect metrics
	 * using the provided MeterRegistry.
	 * @param meterRegistry the MeterRegistry used to collect metrics
	 * @return the DefaultKafkaConsumerFactoryCustomizer that adds the listener to the
	 * consumer factory
	 */
	@Bean
	public DefaultKafkaConsumerFactoryCustomizer kafkaConsumerMetrics(MeterRegistry meterRegistry) {
		return (consumerFactory) -> addListener(consumerFactory, meterRegistry);
	}

	/**
	 * Adds a listener to the given Kafka consumer factory to track metrics using
	 * Micrometer.
	 * @param factory the Kafka consumer factory to add the listener to
	 * @param meterRegistry the Micrometer meter registry to use for tracking metrics
	 * @param <K> the key type for the Kafka consumer
	 * @param <V> the value type for the Kafka consumer
	 */
	private <K, V> void addListener(DefaultKafkaConsumerFactory<K, V> factory, MeterRegistry meterRegistry) {
		factory.addListener(new MicrometerConsumerListener<>(meterRegistry));
	}

	/**
	 * Adds a listener to the given Kafka producer factory to track metrics using
	 * Micrometer.
	 * @param factory the Kafka producer factory to add the listener to
	 * @param meterRegistry the Micrometer meter registry to track metrics
	 * @param <K> the key type of the Kafka producer
	 * @param <V> the value type of the Kafka producer
	 */
	private <K, V> void addListener(DefaultKafkaProducerFactory<K, V> factory, MeterRegistry meterRegistry) {
		factory.addListener(new MicrometerProducerListener<>(meterRegistry));
	}

	/**
	 * KafkaStreamsMetricsConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ KafkaStreamsMetrics.class, StreamsBuilderFactoryBean.class })
	static class KafkaStreamsMetricsConfiguration {

		/**
		 * Customizes the Kafka Streams StreamsBuilderFactoryBean by adding a listener for
		 * Kafka Streams metrics.
		 * @param meterRegistry the MeterRegistry used for collecting metrics
		 * @return the StreamsBuilderFactoryBeanCustomizer that adds the
		 * KafkaStreamsMicrometerListener to the factory bean
		 */
		@Bean
		StreamsBuilderFactoryBeanCustomizer kafkaStreamsMetrics(MeterRegistry meterRegistry) {
			return (factoryBean) -> factoryBean.addListener(new KafkaStreamsMicrometerListener(meterRegistry));
		}

	}

}
