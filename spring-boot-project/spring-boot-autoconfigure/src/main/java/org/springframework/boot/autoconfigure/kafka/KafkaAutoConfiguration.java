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

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Jaas;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Retry.Topic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import org.springframework.kafka.security.jaas.KafkaJaasLoginModuleInitializer;
import org.springframework.kafka.support.LoggingProducerListener;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.retry.backoff.BackOffPolicyBuilder;
import org.springframework.retry.backoff.SleepingBackOffPolicy;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Apache Kafka.
 *
 * @author Gary Russell
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Nakul Mishra
 * @author Tomaz Fernandes
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 1.5.0
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(KafkaProperties.class)
@Import({ KafkaAnnotationDrivenConfiguration.class, KafkaStreamsAnnotationDrivenConfiguration.class })
public class KafkaAutoConfiguration {

	private final KafkaProperties properties;

	/**
	 * Constructs a new KafkaAutoConfiguration with the specified KafkaProperties.
	 * @param properties the KafkaProperties to be used for configuration
	 */
	KafkaAutoConfiguration(KafkaProperties properties) {
		this.properties = properties;
	}

	/**
	 * Generates a KafkaConnectionDetails bean if no bean of type KafkaConnectionDetails
	 * is already present.
	 * @param properties the KafkaProperties object used to configure the Kafka connection
	 * @return a PropertiesKafkaConnectionDetails object representing the Kafka connection
	 * details
	 */
	@Bean
	@ConditionalOnMissingBean(KafkaConnectionDetails.class)
	PropertiesKafkaConnectionDetails kafkaConnectionDetails(KafkaProperties properties) {
		return new PropertiesKafkaConnectionDetails(properties);
	}

	/**
	 * Creates a KafkaTemplate bean if no bean of type KafkaTemplate is already present.
	 * @param kafkaProducerFactory The producer factory used to create the KafkaTemplate.
	 * @param kafkaProducerListener The producer listener used by the KafkaTemplate.
	 * @param messageConverter The message converter used by the KafkaTemplate.
	 * @return The created KafkaTemplate bean.
	 */
	@Bean
	@ConditionalOnMissingBean(KafkaTemplate.class)
	public KafkaTemplate<?, ?> kafkaTemplate(ProducerFactory<Object, Object> kafkaProducerFactory,
			ProducerListener<Object, Object> kafkaProducerListener,
			ObjectProvider<RecordMessageConverter> messageConverter) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		KafkaTemplate<Object, Object> kafkaTemplate = new KafkaTemplate<>(kafkaProducerFactory);
		messageConverter.ifUnique(kafkaTemplate::setMessageConverter);
		map.from(kafkaProducerListener).to(kafkaTemplate::setProducerListener);
		map.from(this.properties.getTemplate().getDefaultTopic()).to(kafkaTemplate::setDefaultTopic);
		map.from(this.properties.getTemplate().getTransactionIdPrefix()).to(kafkaTemplate::setTransactionIdPrefix);
		map.from(this.properties.getTemplate().isObservationEnabled()).to(kafkaTemplate::setObservationEnabled);
		return kafkaTemplate;
	}

	/**
	 * Creates a new instance of LoggingProducerListener if no other bean of type
	 * ProducerListener is present. This listener logs the events related to Kafka
	 * producer.
	 * @return the LoggingProducerListener instance
	 */
	@Bean
	@ConditionalOnMissingBean(ProducerListener.class)
	public LoggingProducerListener<Object, Object> kafkaProducerListener() {
		return new LoggingProducerListener<>();
	}

	/**
	 * Creates a default Kafka consumer factory if no other bean of type ConsumerFactory
	 * is present.
	 * @param connectionDetails the Kafka connection details
	 * @param customizers the customizers for the consumer factory
	 * @param sslBundles the SSL bundles for the consumer factory
	 * @return the default Kafka consumer factory
	 */
	@Bean
	@ConditionalOnMissingBean(ConsumerFactory.class)
	public DefaultKafkaConsumerFactory<?, ?> kafkaConsumerFactory(KafkaConnectionDetails connectionDetails,
			ObjectProvider<DefaultKafkaConsumerFactoryCustomizer> customizers, ObjectProvider<SslBundles> sslBundles) {
		Map<String, Object> properties = this.properties.buildConsumerProperties(sslBundles.getIfAvailable());
		applyKafkaConnectionDetailsForConsumer(properties, connectionDetails);
		DefaultKafkaConsumerFactory<Object, Object> factory = new DefaultKafkaConsumerFactory<>(properties);
		customizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
		return factory;
	}

	/**
	 * Creates a Kafka producer factory if no other bean of type ProducerFactory is
	 * present.
	 * @param connectionDetails the Kafka connection details
	 * @param customizers the customizers for the producer factory
	 * @param sslBundles the SSL bundles for the producer properties
	 * @return the Kafka producer factory
	 */
	@Bean
	@ConditionalOnMissingBean(ProducerFactory.class)
	public DefaultKafkaProducerFactory<?, ?> kafkaProducerFactory(KafkaConnectionDetails connectionDetails,
			ObjectProvider<DefaultKafkaProducerFactoryCustomizer> customizers, ObjectProvider<SslBundles> sslBundles) {
		Map<String, Object> properties = this.properties.buildProducerProperties(sslBundles.getIfAvailable());
		applyKafkaConnectionDetailsForProducer(properties, connectionDetails);
		DefaultKafkaProducerFactory<?, ?> factory = new DefaultKafkaProducerFactory<>(properties);
		String transactionIdPrefix = this.properties.getProducer().getTransactionIdPrefix();
		if (transactionIdPrefix != null) {
			factory.setTransactionIdPrefix(transactionIdPrefix);
		}
		customizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
		return factory;
	}

	/**
	 * Creates a KafkaTransactionManager bean if the property
	 * "spring.kafka.producer.transaction-id-prefix" is present and no other bean of type
	 * KafkaTransactionManager is defined.
	 * @param producerFactory the ProducerFactory used to create the
	 * KafkaTransactionManager
	 * @return the created KafkaTransactionManager bean
	 */
	@Bean
	@ConditionalOnProperty(name = "spring.kafka.producer.transaction-id-prefix")
	@ConditionalOnMissingBean
	public KafkaTransactionManager<?, ?> kafkaTransactionManager(ProducerFactory<?, ?> producerFactory) {
		return new KafkaTransactionManager<>(producerFactory);
	}

	/**
	 * Creates a KafkaJaasLoginModuleInitializer bean if the property
	 * "spring.kafka.jaas.enabled" is set to true and no other bean of the same type
	 * exists.
	 * @return the KafkaJaasLoginModuleInitializer bean
	 * @throws IOException if an I/O error occurs while initializing the Jaas login module
	 */
	@Bean
	@ConditionalOnProperty(name = "spring.kafka.jaas.enabled")
	@ConditionalOnMissingBean
	public KafkaJaasLoginModuleInitializer kafkaJaasInitializer() throws IOException {
		KafkaJaasLoginModuleInitializer jaas = new KafkaJaasLoginModuleInitializer();
		Jaas jaasProperties = this.properties.getJaas();
		if (jaasProperties.getControlFlag() != null) {
			jaas.setControlFlag(jaasProperties.getControlFlag());
		}
		if (jaasProperties.getLoginModule() != null) {
			jaas.setLoginModule(jaasProperties.getLoginModule());
		}
		jaas.setOptions(jaasProperties.getOptions());
		return jaas;
	}

	/**
	 * Creates a KafkaAdmin bean if no other bean of type KafkaAdmin is present.
	 * @param connectionDetails the Kafka connection details
	 * @param sslBundles the SSL bundles (optional)
	 * @return the KafkaAdmin bean
	 */
	@Bean
	@ConditionalOnMissingBean
	public KafkaAdmin kafkaAdmin(KafkaConnectionDetails connectionDetails, ObjectProvider<SslBundles> sslBundles) {
		Map<String, Object> properties = this.properties.buildAdminProperties(sslBundles.getIfAvailable());
		applyKafkaConnectionDetailsForAdmin(properties, connectionDetails);
		KafkaAdmin kafkaAdmin = new KafkaAdmin(properties);
		KafkaProperties.Admin admin = this.properties.getAdmin();
		if (admin.getCloseTimeout() != null) {
			kafkaAdmin.setCloseTimeout((int) admin.getCloseTimeout().getSeconds());
		}
		if (admin.getOperationTimeout() != null) {
			kafkaAdmin.setOperationTimeout((int) admin.getOperationTimeout().getSeconds());
		}
		kafkaAdmin.setFatalIfBrokerNotAvailable(admin.isFailFast());
		kafkaAdmin.setModifyTopicConfigs(admin.isModifyTopicConfigs());
		kafkaAdmin.setAutoCreate(admin.isAutoCreate());
		return kafkaAdmin;
	}

	/**
	 * Configures the retry topic for Kafka based on the properties provided. This
	 * configuration is conditional on the property "spring.kafka.retry.topic.enabled"
	 * being set. It also requires a single candidate KafkaTemplate bean to be available.
	 * @param kafkaTemplate The KafkaTemplate bean used for producing messages.
	 * @return The RetryTopicConfiguration for the Kafka retry topic.
	 */
	@Bean
	@ConditionalOnProperty(name = "spring.kafka.retry.topic.enabled")
	@ConditionalOnSingleCandidate(KafkaTemplate.class)
	public RetryTopicConfiguration kafkaRetryTopicConfiguration(KafkaTemplate<?, ?> kafkaTemplate) {
		KafkaProperties.Retry.Topic retryTopic = this.properties.getRetry().getTopic();
		RetryTopicConfigurationBuilder builder = RetryTopicConfigurationBuilder.newInstance()
			.maxAttempts(retryTopic.getAttempts())
			.useSingleTopicForSameIntervals()
			.suffixTopicsWithIndexValues()
			.doNotAutoCreateRetryTopics();
		setBackOffPolicy(builder, retryTopic);
		return builder.create(kafkaTemplate);
	}

	/**
	 * Applies the Kafka connection details for the consumer to the given properties map.
	 * @param properties the properties map to apply the connection details to
	 * @param connectionDetails the Kafka connection details for the consumer
	 */
	private void applyKafkaConnectionDetailsForConsumer(Map<String, Object> properties,
			KafkaConnectionDetails connectionDetails) {
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getConsumerBootstrapServers());
		if (!(connectionDetails instanceof PropertiesKafkaConnectionDetails)) {
			properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
		}
	}

	/**
	 * Applies the Kafka connection details for the producer to the given properties map.
	 * @param properties the properties map to apply the connection details to
	 * @param connectionDetails the Kafka connection details for the producer
	 */
	private void applyKafkaConnectionDetailsForProducer(Map<String, Object> properties,
			KafkaConnectionDetails connectionDetails) {
		properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getProducerBootstrapServers());
		if (!(connectionDetails instanceof PropertiesKafkaConnectionDetails)) {
			properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
		}
	}

	/**
	 * Applies the Kafka connection details for the admin client to the given properties
	 * map.
	 * @param properties the properties map to apply the connection details to
	 * @param connectionDetails the Kafka connection details for the admin client
	 */
	private void applyKafkaConnectionDetailsForAdmin(Map<String, Object> properties,
			KafkaConnectionDetails connectionDetails) {
		properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getAdminBootstrapServers());
		if (!(connectionDetails instanceof PropertiesKafkaConnectionDetails)) {
			properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
		}
	}

	/**
	 * Sets the backoff policy for retrying a topic.
	 * @param builder the RetryTopicConfigurationBuilder to set the backoff policy on
	 * @param retryTopic the retry topic to get the backoff policy configuration from
	 */
	private static void setBackOffPolicy(RetryTopicConfigurationBuilder builder, Topic retryTopic) {
		long delay = (retryTopic.getDelay() != null) ? retryTopic.getDelay().toMillis() : 0;
		if (delay > 0) {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			BackOffPolicyBuilder backOffPolicy = BackOffPolicyBuilder.newBuilder();
			map.from(delay).to(backOffPolicy::delay);
			map.from(retryTopic.getMaxDelay()).as(Duration::toMillis).to(backOffPolicy::maxDelay);
			map.from(retryTopic.getMultiplier()).to(backOffPolicy::multiplier);
			map.from(retryTopic.isRandomBackOff()).to(backOffPolicy::random);
			builder.customBackoff((SleepingBackOffPolicy<?>) backOffPolicy.build());
		}
		else {
			builder.noBackoff();
		}
	}

}
