/*
 * Copyright 2012-2021 the original author or authors.
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

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
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
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(KafkaAutoConfiguration.class)
@AutoConfigureAfter({ MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class })
@ConditionalOnClass({ KafkaClientMetrics.class, ProducerFactory.class })
@ConditionalOnBean(MeterRegistry.class)
public class KafkaMetricsAutoConfiguration {

	@Bean
	public DefaultKafkaProducerFactoryCustomizer kafkaProducerMetrics(MeterRegistry meterRegistry) {
		return (producerFactory) -> addListener(producerFactory, meterRegistry);
	}

	@Bean
	public DefaultKafkaConsumerFactoryCustomizer kafkaConsumerMetrics(MeterRegistry meterRegistry) {
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
		StreamsBuilderFactoryBeanCustomizer kafkaStreamsMetrics(MeterRegistry meterRegistry) {
			return (factoryBean) -> factoryBean.addListener(new KafkaStreamsMicrometerListener(meterRegistry));
		}

	}

}
