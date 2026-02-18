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

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails.Configuration;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Jaas;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Retry.Topic.Backoff;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.retry.RetryPolicy;
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
import org.springframework.kafka.support.micrometer.KafkaTemplateObservationConvention;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.util.StringUtils;
import org.springframework.util.backoff.BackOff;

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
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(KafkaProperties.class)
@Import({ KafkaAnnotationDrivenConfiguration.class, KafkaStreamsAnnotationDrivenConfiguration.class })
@ImportRuntimeHints(KafkaAutoConfiguration.KafkaRuntimeHints.class)
public final class KafkaAutoConfiguration {

	private final KafkaProperties properties;

	KafkaAutoConfiguration(KafkaProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(KafkaConnectionDetails.class)
	PropertiesKafkaConnectionDetails kafkaConnectionDetails(ObjectProvider<SslBundles> sslBundles) {
		return new PropertiesKafkaConnectionDetails(this.properties, sslBundles.getIfAvailable());
	}

	@Bean
	@ConditionalOnMissingBean(KafkaTemplate.class)
	KafkaTemplate<?, ?> kafkaTemplate(ProducerFactory<Object, Object> kafkaProducerFactory,
			ProducerListener<Object, Object> kafkaProducerListener,
			ObjectProvider<RecordMessageConverter> messageConverter,
			ObjectProvider<KafkaTemplateObservationConvention> observationConvention) {
		PropertyMapper map = PropertyMapper.get();
		KafkaTemplate<Object, Object> kafkaTemplate = new KafkaTemplate<>(kafkaProducerFactory);
		messageConverter.ifUnique(kafkaTemplate::setMessageConverter);
		observationConvention.ifUnique(kafkaTemplate::setObservationConvention);
		map.from(kafkaProducerListener).to(kafkaTemplate::setProducerListener);
		map.from(this.properties.getTemplate().getDefaultTopic()).to(kafkaTemplate::setDefaultTopic);
		map.from(this.properties.getTemplate().getTransactionIdPrefix()).to(kafkaTemplate::setTransactionIdPrefix);
		map.from(this.properties.getTemplate().isObservationEnabled()).to(kafkaTemplate::setObservationEnabled);
		return kafkaTemplate;
	}

	@Bean
	@ConditionalOnMissingBean(ProducerListener.class)
	LoggingProducerListener<Object, Object> kafkaProducerListener() {
		return new LoggingProducerListener<>();
	}

	@Bean
	@ConditionalOnMissingBean(ConsumerFactory.class)
	DefaultKafkaConsumerFactory<?, ?> kafkaConsumerFactory(KafkaConnectionDetails connectionDetails,
			ObjectProvider<DefaultKafkaConsumerFactoryCustomizer> customizers) {
		Map<String, Object> properties = this.properties.buildConsumerProperties();
		applyKafkaConnectionDetailsForConsumer(properties, connectionDetails);
		DefaultKafkaConsumerFactory<Object, Object> factory = new DefaultKafkaConsumerFactory<>(properties);
		customizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
		return factory;
	}

	@Bean
	@ConditionalOnMissingBean(ProducerFactory.class)
	DefaultKafkaProducerFactory<?, ?> kafkaProducerFactory(KafkaConnectionDetails connectionDetails,
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
	KafkaTransactionManager<?, ?> kafkaTransactionManager(ProducerFactory<?, ?> producerFactory) {
		return new KafkaTransactionManager<>(producerFactory);
	}

	@Bean
	@ConditionalOnBooleanProperty("spring.kafka.jaas.enabled")
	@ConditionalOnMissingBean
	KafkaJaasLoginModuleInitializer kafkaJaasInitializer() throws IOException {
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
	KafkaAdmin kafkaAdmin(KafkaConnectionDetails connectionDetails) {
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
	@ConditionalOnBooleanProperty("spring.kafka.retry.topic.enabled")
	@ConditionalOnSingleCandidate(KafkaTemplate.class)
	RetryTopicConfiguration kafkaRetryTopicConfiguration(KafkaTemplate<?, ?> kafkaTemplate) {
		KafkaProperties.Retry.Topic retryTopic = this.properties.getRetry().getTopic();
		RetryTopicConfigurationBuilder builder = RetryTopicConfigurationBuilder.newInstance()
			.maxAttempts(retryTopic.getAttempts())
			.useSingleTopicForSameIntervals()
			.suffixTopicsWithIndexValues()
			.doNotAutoCreateRetryTopics()
			.customBackoff(getBackOff(retryTopic.getBackoff()));
		return builder.create(kafkaTemplate);
	}

	private void applyKafkaConnectionDetailsForConsumer(Map<String, Object> properties,
			KafkaConnectionDetails connectionDetails) {
		Configuration consumer = connectionDetails.getConsumer();
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumer.getBootstrapServers());
		applySecurityProtocol(properties, connectionDetails.getSecurityProtocol());
		applySslBundle(properties, consumer.getSslBundle());
	}

	private void applyKafkaConnectionDetailsForProducer(Map<String, Object> properties,
			KafkaConnectionDetails connectionDetails) {
		Configuration producer = connectionDetails.getProducer();
		properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producer.getBootstrapServers());
		applySecurityProtocol(properties, producer.getSecurityProtocol());
		applySslBundle(properties, producer.getSslBundle());
	}

	private void applyKafkaConnectionDetailsForAdmin(Map<String, Object> properties,
			KafkaConnectionDetails connectionDetails) {
		Configuration admin = connectionDetails.getAdmin();
		properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, admin.getBootstrapServers());
		applySecurityProtocol(properties, admin.getSecurityProtocol());
		applySslBundle(properties, admin.getSslBundle());
	}

	static BackOff getBackOff(Backoff retryTopicBackoff) {
		PropertyMapper map = PropertyMapper.get();
		RetryPolicy.Builder builder = RetryPolicy.builder().maxRetries(Long.MAX_VALUE);
		map.from(retryTopicBackoff.getDelay()).to(builder::delay);
		map.from(retryTopicBackoff.getMaxDelay()).when(Predicate.not(Duration::isZero)).to(builder::maxDelay);
		map.from(retryTopicBackoff.getMultiplier()).to(builder::multiplier);
		map.from(retryTopicBackoff.getJitter()).when((Predicate.not(Duration::isZero))).to(builder::jitter);
		return builder.build().getBackOff();
	}

	static void applySslBundle(Map<String, Object> properties, @Nullable SslBundle sslBundle) {
		if (sslBundle != null) {
			properties.put(SslConfigs.SSL_ENGINE_FACTORY_CLASS_CONFIG, SslBundleSslEngineFactory.class);
			properties.put(SslBundle.class.getName(), sslBundle);
		}
	}

	static void applySecurityProtocol(Map<String, Object> properties, @Nullable String securityProtocol) {
		if (StringUtils.hasLength(securityProtocol)) {
			properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
		}
	}

	static class KafkaRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			hints.reflection().registerType(SslBundleSslEngineFactory.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
		}

	}

}
