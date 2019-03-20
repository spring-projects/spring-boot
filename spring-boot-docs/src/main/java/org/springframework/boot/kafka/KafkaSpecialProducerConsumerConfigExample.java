/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.kafka;

import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Example custom kafka configuration beans used when the user wants to apply different
 * common properties to the producer and consumer.
 *
 * @author Gary Russell
 * @since 1.5
 */
public class KafkaSpecialProducerConsumerConfigExample {

	// tag::configuration[]
	@Configuration
	public static class CustomKafkaBeans {

		/**
		 * Customized ProducerFactory bean.
		 * @param properties the kafka properties.
		 * @return the bean.
		 */
		@Bean
		public ProducerFactory<?, ?> kafkaProducerFactory(KafkaProperties properties) {
			Map<String, Object> producerProperties = properties.buildProducerProperties();
			producerProperties.put(CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG,
					MyProducerMetricsReporter.class);
			return new DefaultKafkaProducerFactory<Object, Object>(producerProperties);
		}

		/**
		 * Customized ConsumerFactory bean.
		 * @param properties the kafka properties.
		 * @return the bean.
		 */
		@Bean
		public ConsumerFactory<?, ?> kafkaConsumerFactory(KafkaProperties properties) {
			Map<String, Object> consumerProperties = properties.buildConsumerProperties();
			consumerProperties.put(CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG,
					MyConsumerMetricsReporter.class);
			return new DefaultKafkaConsumerFactory<Object, Object>(consumerProperties);
		}

	}
	// end::configuration[]

	public static class MyConsumerMetricsReporter implements MetricsReporter {

		@Override
		public void configure(Map<String, ?> configs) {
		}

		@Override
		public void init(List<KafkaMetric> metrics) {
		}

		@Override
		public void metricChange(KafkaMetric metric) {
		}

		@Override
		public void metricRemoval(KafkaMetric metric) {
		}

		@Override
		public void close() {
		}

	}

	public static class MyProducerMetricsReporter implements MetricsReporter {

		@Override
		public void configure(Map<String, ?> configs) {
		}

		@Override
		public void init(List<KafkaMetric> metrics) {
		}

		@Override
		public void metricChange(KafkaMetric metric) {
		}

		@Override
		public void metricRemoval(KafkaMetric metric) {
		}

		@Override
		public void close() {
		}

	}

}
