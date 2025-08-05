/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.kafka.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer;
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
 * @since 4.0.0
 */
@AutoConfiguration(before = KafkaAutoConfiguration.class,
		afterName = { "org.springframework.boot.metrics.autoconfigure.MetricsAutoConfiguration",
				"org.springframework.boot.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration" })
@ConditionalOnClass({ KafkaClientMetrics.class, ProducerFactory.class, MeterRegistry.class })
@ConditionalOnBean(MeterRegistry.class)
public final class KafkaMetricsAutoConfiguration {

	@Bean
	DefaultKafkaProducerFactoryCustomizer kafkaProducerMetrics(MeterRegistry meterRegistry) {
		return (producerFactory) -> addListener(producerFactory, meterRegistry);
	}

	@Bean
	DefaultKafkaConsumerFactoryCustomizer kafkaConsumerMetrics(MeterRegistry meterRegistry) {
		return (consumerFactory) -> addListener(consumerFactory, meterRegistry);
	}

	private <K, V> void addListener(DefaultKafkaConsumerFactory<K, V> factory, MeterRegistry meterRegistry) {
		factory.addListener(new MicrometerConsumerListener<>(meterRegistry));
	}

	private <K, V> void addListener(DefaultKafkaProducerFactory<K, V> factory, MeterRegistry meterRegistry) {
		factory.addListener(new MicrometerProducerListener<>(meterRegistry));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ KafkaStreamsMetrics.class, StreamsBuilderFactoryBean.class })
	static class KafkaStreamsMetricsConfiguration {

		@Bean
		StreamsBuilderFactoryBeanConfigurer kafkaStreamsMetrics(MeterRegistry meterRegistry) {
			return (factoryBean) -> factoryBean.addListener(new KafkaStreamsMicrometerListener(meterRegistry));
		}

	}

}
