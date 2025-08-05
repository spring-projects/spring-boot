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

package org.springframework.boot.kafka.autoconfigure;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer;
import org.springframework.kafka.core.CleanupConfig;

/**
 * Configuration for Kafka Streams annotation-driven support.
 *
 * @author Gary Russell
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(StreamsBuilder.class)
@ConditionalOnBean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_BUILDER_BEAN_NAME)
class KafkaStreamsAnnotationDrivenConfiguration {

	private final KafkaProperties properties;

	KafkaStreamsAnnotationDrivenConfiguration(KafkaProperties properties) {
		this.properties = properties;
	}

	@ConditionalOnMissingBean
	@Bean(KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
	KafkaStreamsConfiguration defaultKafkaStreamsConfig(Environment environment,
			KafkaConnectionDetails connectionDetails) {
		Map<String, Object> properties = this.properties.buildStreamsProperties();
		applyKafkaConnectionDetailsForStreams(properties, connectionDetails);
		if (this.properties.getStreams().getApplicationId() == null) {
			String applicationName = environment.getProperty("spring.application.name");
			if (applicationName == null) {
				throw new InvalidConfigurationPropertyValueException("spring.kafka.streams.application-id", null,
						"This property is mandatory and fallback 'spring.application.name' is not set either.");
			}
			properties.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationName);
		}
		return new KafkaStreamsConfiguration(properties);
	}

	@Bean
	StreamsBuilderFactoryBeanConfigurer kafkaPropertiesStreamsBuilderFactoryBeanConfigurer() {
		return new KafkaPropertiesStreamsBuilderFactoryBeanConfigurer(this.properties);
	}

	private void applyKafkaConnectionDetailsForStreams(Map<String, Object> properties,
			KafkaConnectionDetails connectionDetails) {
		KafkaConnectionDetails.Configuration streams = connectionDetails.getStreams();
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, streams.getBootstrapServers());
		KafkaAutoConfiguration.applySecurityProtocol(properties, streams.getSecurityProtocol());
		KafkaAutoConfiguration.applySslBundle(properties, streams.getSslBundle());
	}

	private static final class KafkaPropertiesStreamsBuilderFactoryBeanConfigurer
			implements StreamsBuilderFactoryBeanConfigurer {

		private final KafkaProperties properties;

		private KafkaPropertiesStreamsBuilderFactoryBeanConfigurer(KafkaProperties properties) {
			this.properties = properties;
		}

		@Override
		public void configure(StreamsBuilderFactoryBean factoryBean) {
			factoryBean.setAutoStartup(this.properties.getStreams().isAutoStartup());
			KafkaProperties.Cleanup cleanup = this.properties.getStreams().getCleanup();
			CleanupConfig cleanupConfig = new CleanupConfig(cleanup.isOnStartup(), cleanup.isOnShutdown());
			factoryBean.setCleanupConfig(cleanupConfig);
		}

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}

	}

}
