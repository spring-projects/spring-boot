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

package org.springframework.boot.autoconfigure.jms;

import java.time.Duration;
import java.util.List;

import io.micrometer.observation.ObservationRegistry;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration.JmsRuntimeHints;
import org.springframework.boot.autoconfigure.jms.JmsProperties.DeliveryMode;
import org.springframework.boot.autoconfigure.jms.JmsProperties.Template;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.jms.core.JmsMessageOperations;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring JMS.
 *
 * @author Greg Turnquist
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ Message.class, JmsTemplate.class })
@ConditionalOnBean(ConnectionFactory.class)
@EnableConfigurationProperties(JmsProperties.class)
@Import(JmsAnnotationDrivenConfiguration.class)
@ImportRuntimeHints(JmsRuntimeHints.class)
public class JmsAutoConfiguration {

	/**
     * JmsTemplateConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	protected static class JmsTemplateConfiguration {

		private final JmsProperties properties;

		private final ObjectProvider<DestinationResolver> destinationResolver;

		private final ObjectProvider<MessageConverter> messageConverter;

		private final ObjectProvider<ObservationRegistry> observationRegistry;

		/**
         * Constructs a new JmsTemplateConfiguration with the provided JmsProperties, DestinationResolver,
         * MessageConverter, and ObservationRegistry.
         * 
         * @param properties the JmsProperties to be used for configuring the JmsTemplate
         * @param destinationResolver the DestinationResolver to be used for resolving destinations
         * @param messageConverter the MessageConverter to be used for converting messages
         * @param observationRegistry the ObservationRegistry to be used for observing JMS operations
         */
        public JmsTemplateConfiguration(JmsProperties properties,
				ObjectProvider<DestinationResolver> destinationResolver,
				ObjectProvider<MessageConverter> messageConverter,
				ObjectProvider<ObservationRegistry> observationRegistry) {
			this.properties = properties;
			this.destinationResolver = destinationResolver;
			this.messageConverter = messageConverter;
			this.observationRegistry = observationRegistry;
		}

		/**
         * Creates a JmsTemplate bean if no other bean of type JmsOperations is present and there is a single candidate bean of type ConnectionFactory.
         * 
         * @param connectionFactory the ConnectionFactory bean to be used for creating the JmsTemplate
         * @return the created JmsTemplate bean
         */
        @Bean
		@ConditionalOnMissingBean(JmsOperations.class)
		@ConditionalOnSingleCandidate(ConnectionFactory.class)
		public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
			PropertyMapper map = PropertyMapper.get();
			JmsTemplate template = new JmsTemplate(connectionFactory);
			template.setPubSubDomain(this.properties.isPubSubDomain());
			map.from(this.destinationResolver::getIfUnique).whenNonNull().to(template::setDestinationResolver);
			map.from(this.messageConverter::getIfUnique).whenNonNull().to(template::setMessageConverter);
			map.from(this.observationRegistry::getIfUnique).whenNonNull().to(template::setObservationRegistry);
			mapTemplateProperties(this.properties.getTemplate(), template);
			return template;
		}

		/**
         * Maps the properties of a Template object to a JmsTemplate object.
         * 
         * @param properties the Template object containing the properties to be mapped
         * @param template the JmsTemplate object to map the properties to
         */
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

	/**
     * MessagingTemplateConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JmsMessagingTemplate.class)
	@Import(JmsTemplateConfiguration.class)
	protected static class MessagingTemplateConfiguration {

		/**
         * Creates a JmsMessagingTemplate bean if there is no existing bean of type JmsMessageOperations and there is a single candidate bean of type JmsTemplate.
         * 
         * @param properties the JmsProperties object containing the properties for the JmsTemplate
         * @param jmsTemplate the JmsTemplate bean used for sending and receiving JMS messages
         * @return the JmsMessagingTemplate bean
         */
        @Bean
		@ConditionalOnMissingBean(JmsMessageOperations.class)
		@ConditionalOnSingleCandidate(JmsTemplate.class)
		public JmsMessagingTemplate jmsMessagingTemplate(JmsProperties properties, JmsTemplate jmsTemplate) {
			JmsMessagingTemplate messagingTemplate = new JmsMessagingTemplate(jmsTemplate);
			mapTemplateProperties(properties.getTemplate(), messagingTemplate);
			return messagingTemplate;
		}

		/**
         * Maps the properties of a Template object to a JmsMessagingTemplate object.
         * 
         * @param properties the Template object containing the properties to be mapped
         * @param messagingTemplate the JmsMessagingTemplate object to map the properties to
         */
        private void mapTemplateProperties(Template properties, JmsMessagingTemplate messagingTemplate) {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(properties::getDefaultDestination).to(messagingTemplate::setDefaultDestinationName);
		}

	}

	/**
     * JmsRuntimeHints class.
     */
    static class JmsRuntimeHints implements RuntimeHintsRegistrar {

		/**
         * Registers hints for the runtime.
         * 
         * @param hints the runtime hints to register
         * @param classLoader the class loader to use for reflection
         */
        @Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.reflection()
				.registerType(TypeReference.of(AcknowledgeMode.class), (type) -> type.withMethod("of",
						List.of(TypeReference.of(String.class)), ExecutableMode.INVOKE));
		}

	}

}
