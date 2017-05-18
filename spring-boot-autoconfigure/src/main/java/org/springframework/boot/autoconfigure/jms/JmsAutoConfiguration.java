/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.jms;

import javax.jms.ConnectionFactory;
import javax.jms.Message;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring JMS.
 *
 * @author Greg Turnquist
 * @author Stephane Nicoll
 */
@Configuration
@ConditionalOnClass({ Message.class, JmsTemplate.class })
@ConditionalOnBean(ConnectionFactory.class)
@EnableConfigurationProperties(JmsProperties.class)
@Import(JmsAnnotationDrivenConfiguration.class)
public class JmsAutoConfiguration {

	@Configuration
	protected static class JmsTemplateConfiguration {

		private final JmsProperties properties;

		private final ObjectProvider<DestinationResolver> destinationResolver;

		private final ObjectProvider<MessageConverter> messageConverter;

		public JmsTemplateConfiguration(JmsProperties properties,
				ObjectProvider<DestinationResolver> destinationResolver,
				ObjectProvider<MessageConverter> messageConverter) {
			this.properties = properties;
			this.destinationResolver = destinationResolver;
			this.messageConverter = messageConverter;
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnSingleCandidate(ConnectionFactory.class)
		public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
			JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
			jmsTemplate.setPubSubDomain(this.properties.isPubSubDomain());
			DestinationResolver destinationResolver = this.destinationResolver
					.getIfUnique();
			if (destinationResolver != null) {
				jmsTemplate.setDestinationResolver(destinationResolver);
			}
			MessageConverter messageConverter = this.messageConverter.getIfUnique();
			if (messageConverter != null) {
				jmsTemplate.setMessageConverter(messageConverter);
			}
			JmsProperties.Template template = this.properties.getTemplate();
			if (template.getDefaultDestination() != null) {
				jmsTemplate.setDefaultDestinationName(template.getDefaultDestination());
			}
			if (template.getDeliveryDelay() != null) {
				jmsTemplate.setDeliveryDelay(template.getDeliveryDelay());
			}
			jmsTemplate.setExplicitQosEnabled(template.determineQosEnabled());
			if (template.getDeliveryMode() != null) {
				jmsTemplate.setDeliveryMode(template.getDeliveryMode().getValue());
			}
			if (template.getPriority() != null) {
				jmsTemplate.setPriority(template.getPriority());
			}
			if (template.getTimeToLive() != null) {
				jmsTemplate.setTimeToLive(template.getTimeToLive());
			}
			if (template.getReceiveTimeout() != null) {
				jmsTemplate.setReceiveTimeout(template.getReceiveTimeout());
			}
			return jmsTemplate;
		}

	}

	@ConditionalOnClass(JmsMessagingTemplate.class)
	@Import(JmsTemplateConfiguration.class)
	protected static class MessagingTemplateConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnSingleCandidate(JmsTemplate.class)
		public JmsMessagingTemplate jmsMessagingTemplate(JmsTemplate jmsTemplate) {
			return new JmsMessagingTemplate(jmsTemplate);
		}

	}

}
