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

import java.io.IOException;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Jaas;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.security.jaas.KafkaJaasLoginModuleInitializer;
import org.springframework.kafka.support.LoggingProducerListener;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.transaction.KafkaTransactionManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Apache Kafka.
 *
 * @author Gary Russell
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Nakul Mishra
 * @since 1.5.0
 */
@Configuration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(KafkaProperties.class)
@Import(KafkaAnnotationDrivenConfiguration.class)
public class KafkaAutoConfiguration {

	private final KafkaProperties properties;

	private final RecordMessageConverter messageConverter;

	public KafkaAutoConfiguration(KafkaProperties properties, ObjectProvider<RecordMessageConverter> messageConverter) {
		this.properties = properties;
		this.messageConverter = messageConverter.getIfUnique();
	}

	@Bean
	@ConditionalOnMissingBean(KafkaTemplate.class)
	public KafkaTemplate<?, ?> kafkaTemplate(ProducerFactory<Object, Object> kafkaProducerFactory,
			ProducerListener<Object, Object> kafkaProducerListener) {
		KafkaTemplate<Object, Object> kafkaTemplate = new KafkaTemplate<>(kafkaProducerFactory);
		if (this.messageConverter != null) {
			kafkaTemplate.setMessageConverter(this.messageConverter);
		}
		kafkaTemplate.setProducerListener(kafkaProducerListener);
		kafkaTemplate.setDefaultTopic(this.properties.getTemplate().getDefaultTopic());
		return kafkaTemplate;
	}

	@Bean
	@ConditionalOnMissingBean(ProducerListener.class)
	public ProducerListener<Object, Object> kafkaProducerListener() {
		return new LoggingProducerListener<>();
	}

	@Bean
	@ConditionalOnMissingBean(ConsumerFactory.class)
	public ConsumerFactory<?, ?> kafkaConsumerFactory() {
		return new DefaultKafkaConsumerFactory<>(this.properties.buildConsumerProperties());
	}

	@Bean
	@ConditionalOnMissingBean(ProducerFactory.class)
	public ProducerFactory<?, ?> kafkaProducerFactory() {
		DefaultKafkaProducerFactory<?, ?> factory = new DefaultKafkaProducerFactory<>(
				this.properties.buildProducerProperties());
		String transactionIdPrefix = this.properties.getProducer().getTransactionIdPrefix();
		if (transactionIdPrefix != null) {
			factory.setTransactionIdPrefix(transactionIdPrefix);
		}
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
	public KafkaAdmin kafkaAdmin() {
		KafkaAdmin kafkaAdmin = new KafkaAdmin(this.properties.buildAdminProperties());
		kafkaAdmin.setFatalIfBrokerNotAvailable(this.properties.getAdmin().isFailFast());
		return kafkaAdmin;
	}

}
