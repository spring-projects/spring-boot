/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.Collections;

import javax.management.MBeanServer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.kafka.KafkaConsumerMetrics;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for Kafka metrics.
 *
 * @author Andy Wilkinson
 * @since 2.1.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({ MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class,
		JmxAutoConfiguration.class })
@ConditionalOnClass({ KafkaConsumerMetrics.class, KafkaConsumer.class })
@ConditionalOnBean(MeterRegistry.class)
public class KafkaMetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(MBeanServer.class)
	public KafkaConsumerMetrics kafkaConsumerMetrics(MBeanServer mbeanServer) {
		return new KafkaConsumerMetrics(mbeanServer, Collections.emptyList());
	}

}
