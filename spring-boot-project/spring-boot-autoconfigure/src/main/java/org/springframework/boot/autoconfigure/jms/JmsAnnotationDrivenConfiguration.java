/*
 * Copyright 2012-2025 the original author or authors.
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

import io.micrometer.observation.ObservationRegistry;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.ExceptionListener;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJndi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.jms.ConnectionFactoryUnwrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerConfigUtils;
import org.springframework.jms.config.SimpleJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * Configuration for Spring 4.1 annotation driven JMS.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Vedran Pavic
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EnableJms.class)
class JmsAnnotationDrivenConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(name = "spring.jms.listener.container-type", havingValue = "default", matchIfMissing = true)
	static class DefaultJmsListenerContainerFactoryConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@SuppressWarnings("removal")
		DefaultJmsListenerContainerFactoryConfigurer jmsListenerContainerFactoryConfigurer(
				ObjectProvider<DestinationResolver> destinationResolver,
				ObjectProvider<JtaTransactionManager> transactionManager,
				ObjectProvider<MessageConverter> messageConverter, ObjectProvider<ExceptionListener> exceptionListener,
				ObjectProvider<ObservationRegistry> observationRegistry, JmsProperties properties) {
			DefaultJmsListenerContainerFactoryConfigurer configurer = new DefaultJmsListenerContainerFactoryConfigurer();
			configurer.setDestinationResolver(destinationResolver.getIfUnique());
			configurer.setTransactionManager(transactionManager.getIfUnique());
			configurer.setMessageConverter(messageConverter.getIfUnique());
			configurer.setExceptionListener(exceptionListener.getIfUnique());
			configurer.setObservationRegistry(observationRegistry.getIfUnique());
			configurer.setJmsProperties(properties);
			return configurer;
		}

		@Bean
		@ConditionalOnSingleCandidate(ConnectionFactory.class)
		@ConditionalOnMissingBean(name = "jmsListenerContainerFactory")
		DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
				DefaultJmsListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
			DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
			configurer.configure(factory, ConnectionFactoryUnwrapper.unwrapCaching(connectionFactory));
			return factory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(name = "spring.jms.listener.container-type", havingValue = "simple")
	static class SimpleJmsListenerContainerFactoryConfiguration {

		@Bean
		@ConditionalOnMissingBean
		SimpleJmsListenerContainerFactoryConfigurer jmsListenerContainerFactoryConfigurer(
				ObjectProvider<DestinationResolver> destinationResolver,
				ObjectProvider<MessageConverter> messageConverter, ObjectProvider<ExceptionListener> exceptionListener,
				ObjectProvider<ObservationRegistry> observationRegistry, JmsProperties properties) {
			SimpleJmsListenerContainerFactoryConfigurer configurer = new SimpleJmsListenerContainerFactoryConfigurer();
			configurer.setDestinationResolver(destinationResolver.getIfUnique());
			configurer.setMessageConverter(messageConverter.getIfUnique());
			configurer.setExceptionListener(exceptionListener.getIfUnique());
			configurer.setObservationRegistry(observationRegistry.getIfUnique());
			configurer.setJmsProperties(properties);
			return configurer;
		}

		@Bean
		@ConditionalOnSingleCandidate(ConnectionFactory.class)
		@ConditionalOnMissingBean(name = "jmsListenerContainerFactory")
		SimpleJmsListenerContainerFactory jmsListenerContainerFactory(
				SimpleJmsListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
			SimpleJmsListenerContainerFactory factory = new SimpleJmsListenerContainerFactory();
			configurer.configure(factory, ConnectionFactoryUnwrapper.unwrapCaching(connectionFactory));
			return factory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableJms
	@ConditionalOnMissingBean(name = JmsListenerConfigUtils.JMS_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
	static class EnableJmsConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnJndi
	static class JndiConfiguration {

		@Bean
		@ConditionalOnMissingBean(DestinationResolver.class)
		JndiDestinationResolver destinationResolver() {
			JndiDestinationResolver resolver = new JndiDestinationResolver();
			resolver.setFallbackToDynamicDestination(true);
			return resolver;
		}

	}

}
