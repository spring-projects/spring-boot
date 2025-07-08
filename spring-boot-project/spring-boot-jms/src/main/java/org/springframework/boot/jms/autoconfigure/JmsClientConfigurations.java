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

package org.springframework.boot.jms.autoconfigure;

import java.time.Duration;

import io.micrometer.observation.ObservationRegistry;
import jakarta.jms.ConnectionFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.jms.autoconfigure.JmsProperties.DeliveryMode;
import org.springframework.boot.jms.autoconfigure.JmsProperties.Template;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsClient;
import org.springframework.jms.core.JmsMessageOperations;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;

/**
 * Configurations for JMS client infrastructure.
 *
 * @author Stephane Nicoll
 * @see JmsAutoConfiguration
 */
abstract class JmsClientConfigurations {

	@Configuration(proxyBeanMethods = false)
	static class JmsTemplateConfiguration {

		private final JmsProperties properties;

		private final ObjectProvider<DestinationResolver> destinationResolver;

		private final ObjectProvider<MessageConverter> messageConverter;

		private final ObjectProvider<ObservationRegistry> observationRegistry;

		JmsTemplateConfiguration(JmsProperties properties, ObjectProvider<DestinationResolver> destinationResolver,
				ObjectProvider<MessageConverter> messageConverter,
				ObjectProvider<ObservationRegistry> observationRegistry) {
			this.properties = properties;
			this.destinationResolver = destinationResolver;
			this.messageConverter = messageConverter;
			this.observationRegistry = observationRegistry;
		}

		@Bean
		@ConditionalOnMissingBean(JmsOperations.class)
		@ConditionalOnSingleCandidate(ConnectionFactory.class)
		JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
			PropertyMapper map = PropertyMapper.get();
			JmsTemplate template = new JmsTemplate(connectionFactory);
			template.setPubSubDomain(this.properties.isPubSubDomain());
			map.from(this.destinationResolver::getIfUnique).whenNonNull().to(template::setDestinationResolver);
			map.from(this.messageConverter::getIfUnique).whenNonNull().to(template::setMessageConverter);
			map.from(this.observationRegistry::getIfUnique).whenNonNull().to(template::setObservationRegistry);
			mapTemplateProperties(this.properties.getTemplate(), template);
			return template;
		}

		private void mapTemplateProperties(Template properties, JmsTemplate template) {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(properties.getSession().getAcknowledgeMode()::getMode).to(template::setSessionAcknowledgeMode);
			map.from(properties.getSession()::isTransacted).to(template::setSessionTransacted);
			map.from(properties::getDefaultDestination).whenNonNull().to(template::setDefaultDestinationName);
			map.from(properties::getDeliveryDelay).whenNonNull().as(Duration::toMillis).to(template::setDeliveryDelay);
			map.from(properties::determineQosEnabled).to(template::setExplicitQosEnabled);
			map.from(properties::getDeliveryMode).as(DeliveryMode::getValue).to(template::setDeliveryMode);
			map.from(properties::getPriority).whenNonNull().to(template::setPriority);
			map.from(properties::getTimeToLive).whenNonNull().as(Duration::toMillis).to(template::setTimeToLive);
			map.from(properties::getReceiveTimeout).as(Duration::toMillis).to(template::setReceiveTimeout);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JmsMessagingTemplate.class)
	static class MessagingTemplateConfiguration {

		@Bean
		@ConditionalOnMissingBean(JmsMessageOperations.class)
		@ConditionalOnSingleCandidate(JmsTemplate.class)
		JmsMessagingTemplate jmsMessagingTemplate(JmsProperties properties, JmsTemplate jmsTemplate) {
			JmsMessagingTemplate messagingTemplate = new JmsMessagingTemplate(jmsTemplate);
			mapTemplateProperties(properties.getTemplate(), messagingTemplate);
			return messagingTemplate;
		}

		private void mapTemplateProperties(Template properties, JmsMessagingTemplate messagingTemplate) {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(properties::getDefaultDestination).to(messagingTemplate::setDefaultDestinationName);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JmsClient.class)
	static class JmsClientConfiguration {

		@Bean
		@ConditionalOnMissingBean(JmsClient.class)
		@ConditionalOnSingleCandidate(JmsTemplate.class)
		JmsClient jmsClient(JmsTemplate jmsTemplate) {
			return JmsClient.create(jmsTemplate);
		}

	}

}
