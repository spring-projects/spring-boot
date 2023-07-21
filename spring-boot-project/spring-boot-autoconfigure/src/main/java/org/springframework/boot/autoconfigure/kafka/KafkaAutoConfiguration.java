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
 * @since 1.5.0
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(KafkaProperties.class)
@Import({ KafkaAnnotationDrivenConfiguration.class, KafkaStreamsAnnotationDrivenConfiguration.class })
public class KafkaAutoConfiguration {

	private final KafkaProperties properties;

	KafkaAutoConfiguration(KafkaProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(KafkaConnectionDetails.class)
	PropertiesKafkaConnectionDetails kafkaConnectionDetails(KafkaProperties properties) {
		return new PropertiesKafkaConnectionDetails(properties);
	}

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
		return kafkaTemplate;
	}

	@Bean
	@ConditionalOnMissingBean(ProducerListener.class)
	public LoggingProducerListener<Object, Object> kafkaProducerListener() {
		return new LoggingProducerListener<>();
	}

	@Bean
	@ConditionalOnMissingBean(ConsumerFactory.class)
	public DefaultKafkaConsumerFactory<?, ?> kafkaConsumerFactory(KafkaConnectionDetails connectionDetails,
			ObjectProvider<DefaultKafkaConsumerFactoryCustomizer> customizers) {
		Map<String, Object> properties = this.properties.buildConsumerProperties();
		applyKafkaConnectionDetailsForConsumer(properties, connectionDetails);
		DefaultKafkaConsumerFactory<Object, Object> factory = new DefaultKafkaConsumerFactory<>(properties);
		customizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
		return factory;
	}

	@Bean
	@ConditionalOnMissingBean(ProducerFactory.class)
	public DefaultKafkaProducerFactory<?, ?> kafkaProducerFactory(KafkaConnectionDetails connectionDetails,
			ObjectProvider<DefaultKafkaProducerFactoryCustomizer> customizers) {
		Map<String, Object> properties = this.properties.buildProducerProperties();
		applyKafkaConnectionDetailsForProducer(properties, connectionDetails);
		DefaultKafkaProducerFactory<?, ?> factory = new DefaultKafkaProducerFactory<>(properties);
		String transactionIdPrefix = this.properties.getProducer().getTransactionIdPrefix();
		if (transactionIdPrefix != null) {
			factory.setTransactionIdPrefix(transactionIdPrefix);
		}
		customizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
		return factory;
	}

	@Bean
	@ConditionalOnProperty(name = "spring.kafka.producer.transaction-id-prefix")
	@ConditionalOnMissingBean
	public KafkaTransactionManager<?, ?> kafkaTransactionManager(ProducerFactory<?, ?> producerFactory) {
		return new KafkaTransactionManager<>(producerFactory);
	}

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

	@Bean
	@ConditionalOnMissingBean
	public KafkaAdmin kafkaAdmin(KafkaConnectionDetails connectionDetails) {
		Map<String, Object> properties = this.properties.buildAdminProperties();
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

	private void applyKafkaConnectionDetailsForConsumer(Map<String, Object> properties,
			KafkaConnectionDetails connectionDetails) {
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getConsumerBootstrapServers());
		if (!(connectionDetails instanceof PropertiesKafkaConnectionDetails)) {
			properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
		}
	}

	private void applyKafkaConnectionDetailsForProducer(Map<String, Object> properties,
			KafkaConnectionDetails connectionDetails) {
		properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getProducerBootstrapServers());
		if (!(connectionDetails instanceof PropertiesKafkaConnectionDetails)) {
			properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
		}
	}

	private void applyKafkaConnectionDetailsForAdmin(Map<String, Object> properties,
			KafkaConnectionDetails connectionDetails) {
		properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getAdminBootstrapServers());
		if (!(connectionDetails instanceof PropertiesKafkaConnectionDetails)) {
			properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
		}
	}

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
