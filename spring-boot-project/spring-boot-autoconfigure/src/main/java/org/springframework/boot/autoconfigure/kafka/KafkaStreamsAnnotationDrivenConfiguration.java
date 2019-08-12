/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.Map;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

/**
 * Configuration for Kafka Streams annotation-driven support.
 *
 * @author Gary Russell
 * @author Stephane Nicoll
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
	KafkaStreamsConfiguration defaultKafkaStreamsConfig(Environment environment) {
		Map<String, Object> streamsProperties = this.properties.buildStreamsProperties();
		if (this.properties.getStreams().getApplicationId() == null) {
			String applicationName = environment.getProperty("spring.application.name");
			if (applicationName == null) {
				throw new InvalidConfigurationPropertyValueException("spring.kafka.streams.application-id", null,
						"This property is mandatory and fallback 'spring.application.name' is not set either.");
			}
			streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationName);
		}
		return new KafkaStreamsConfiguration(streamsProperties);
	}

	@Bean
	KafkaStreamsFactoryBeanConfigurer kafkaStreamsFactoryBeanConfigurer(
			@Qualifier(KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_BUILDER_BEAN_NAME) StreamsBuilderFactoryBean factoryBean) {
		return new KafkaStreamsFactoryBeanConfigurer(this.properties, factoryBean);
	}

	// Separate class required to avoid BeanCurrentlyInCreationException
	static class KafkaStreamsFactoryBeanConfigurer implements InitializingBean {

		private final KafkaProperties properties;

		private final StreamsBuilderFactoryBean factoryBean;

		KafkaStreamsFactoryBeanConfigurer(KafkaProperties properties, StreamsBuilderFactoryBean factoryBean) {
			this.properties = properties;
			this.factoryBean = factoryBean;
		}

		@Override
		public void afterPropertiesSet() {
			this.factoryBean.setAutoStartup(this.properties.getStreams().isAutoStartup());
		}

	}

}
