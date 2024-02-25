/*
 * Copyright 2012-2023 the original author or authors.
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

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
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

	/**
     * Constructs a new KafkaStreamsAnnotationDrivenConfiguration with the specified KafkaProperties.
     *
     * @param properties the KafkaProperties to be used for configuring the KafkaStreamsAnnotationDrivenConfiguration
     */
    KafkaStreamsAnnotationDrivenConfiguration(KafkaProperties properties) {
		this.properties = properties;
	}

	/**
     * Generates a default KafkaStreamsConfiguration bean if no other bean of the same type is present.
     * 
     * @param environment The environment object used to retrieve properties.
     * @param connectionDetails The Kafka connection details.
     * @param sslBundles The SSL bundles object provider.
     * @return The default KafkaStreamsConfiguration bean.
     * @throws InvalidConfigurationPropertyValueException If the 'spring.kafka.streams.application-id' property is not set.
     */
    @ConditionalOnMissingBean
	@Bean(KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
	KafkaStreamsConfiguration defaultKafkaStreamsConfig(Environment environment,
			KafkaConnectionDetails connectionDetails, ObjectProvider<SslBundles> sslBundles) {
		Map<String, Object> properties = this.properties.buildStreamsProperties(sslBundles.getIfAvailable());
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

	/**
     * Configures the KafkaStreamsFactoryBean with customizers.
     * 
     * @param factoryBean The StreamsBuilderFactoryBean instance.
     * @param customizers The customizers for the StreamsBuilderFactoryBean.
     * @return The KafkaStreamsFactoryBeanConfigurer instance.
     */
    @Bean
	KafkaStreamsFactoryBeanConfigurer kafkaStreamsFactoryBeanConfigurer(
			@Qualifier(KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_BUILDER_BEAN_NAME) StreamsBuilderFactoryBean factoryBean,
			ObjectProvider<StreamsBuilderFactoryBeanCustomizer> customizers) {
		customizers.orderedStream().forEach((customizer) -> customizer.customize(factoryBean));
		return new KafkaStreamsFactoryBeanConfigurer(this.properties, factoryBean);
	}

	/**
     * Applies the Kafka connection details for streams to the given properties map.
     * 
     * @param properties the properties map to apply the connection details to
     * @param connectionDetails the Kafka connection details to apply
     */
    private void applyKafkaConnectionDetailsForStreams(Map<String, Object> properties,
			KafkaConnectionDetails connectionDetails) {
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getStreamsBootstrapServers());
		if (!(connectionDetails instanceof PropertiesKafkaConnectionDetails)) {
			properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
		}
	}

	// Separate class required to avoid BeanCurrentlyInCreationException
	static class KafkaStreamsFactoryBeanConfigurer implements InitializingBean {

		private final KafkaProperties properties;

		private final StreamsBuilderFactoryBean factoryBean;

		/**
         * Constructs a new KafkaStreamsFactoryBeanConfigurer with the specified KafkaProperties and StreamsBuilderFactoryBean.
         * 
         * @param properties the KafkaProperties to be used
         * @param factoryBean the StreamsBuilderFactoryBean to be used
         */
        KafkaStreamsFactoryBeanConfigurer(KafkaProperties properties, StreamsBuilderFactoryBean factoryBean) {
			this.properties = properties;
			this.factoryBean = factoryBean;
		}

		/**
         * Sets the auto startup and cleanup configuration for the KafkaStreamsFactoryBean.
         * This method is called after all properties have been set.
         * 
         * @throws Exception if an error occurs during the initialization
         */
        @Override
		public void afterPropertiesSet() {
			this.factoryBean.setAutoStartup(this.properties.getStreams().isAutoStartup());
			KafkaProperties.Cleanup cleanup = this.properties.getStreams().getCleanup();
			CleanupConfig cleanupConfig = new CleanupConfig(cleanup.isOnStartup(), cleanup.isOnShutdown());
			this.factoryBean.setCleanupConfig(cleanupConfig);
		}

	}

}
