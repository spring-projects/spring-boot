/*
 * Copyright 2012-2021 the original author or authors.
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerConfigUtils;
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
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EnableJms.class)
class JmsAnnotationDrivenConfiguration {

	private final ObjectProvider<DestinationResolver> destinationResolver;

	private final ObjectProvider<JtaTransactionManager> transactionManager;

	private final ObjectProvider<MessageConverter> messageConverter;

	private final ObjectProvider<ExceptionListener> exceptionListener;

	private final ObjectProvider<ObservationRegistry> observationRegistry;

	private final JmsProperties properties;

	/**
	 * Constructs a new JmsAnnotationDrivenConfiguration with the specified parameters.
	 * @param destinationResolver the provider for resolving JMS destinations
	 * @param transactionManager the provider for managing JTA transactions
	 * @param messageConverter the provider for converting JMS messages
	 * @param exceptionListener the provider for handling JMS exceptions
	 * @param observationRegistry the provider for registering JMS observations
	 * @param properties the JMS properties
	 */
	JmsAnnotationDrivenConfiguration(ObjectProvider<DestinationResolver> destinationResolver,
			ObjectProvider<JtaTransactionManager> transactionManager, ObjectProvider<MessageConverter> messageConverter,
			ObjectProvider<ExceptionListener> exceptionListener,
			ObjectProvider<ObservationRegistry> observationRegistry, JmsProperties properties) {
		this.destinationResolver = destinationResolver;
		this.transactionManager = transactionManager;
		this.messageConverter = messageConverter;
		this.exceptionListener = exceptionListener;
		this.observationRegistry = observationRegistry;
		this.properties = properties;
	}

	/**
	 * Creates a new instance of DefaultJmsListenerContainerFactoryConfigurer. This method
	 * is annotated with @Bean, indicating that it is a Spring bean and should be managed
	 * by the Spring container. It is also annotated with @ConditionalOnMissingBean, which
	 * means that this bean will only be created if there is no other bean of the same
	 * type already defined in the container.
	 *
	 * This method configures the DefaultJmsListenerContainerFactoryConfigurer by setting
	 * various properties such as destination resolver, transaction manager, message
	 * converter, exception listener, observation registry, and JMS properties.
	 * @return The configured DefaultJmsListenerContainerFactoryConfigurer instance.
	 */
	@Bean
	@ConditionalOnMissingBean
	DefaultJmsListenerContainerFactoryConfigurer jmsListenerContainerFactoryConfigurer() {
		DefaultJmsListenerContainerFactoryConfigurer configurer = new DefaultJmsListenerContainerFactoryConfigurer();
		configurer.setDestinationResolver(this.destinationResolver.getIfUnique());
		configurer.setTransactionManager(this.transactionManager.getIfUnique());
		configurer.setMessageConverter(this.messageConverter.getIfUnique());
		configurer.setExceptionListener(this.exceptionListener.getIfUnique());
		configurer.setObservationRegistry(this.observationRegistry.getIfUnique());
		configurer.setJmsProperties(this.properties);
		return configurer;
	}

	/**
	 * Creates a default JMS listener container factory if no other bean with the name
	 * "jmsListenerContainerFactory" is present. This factory is conditionally created
	 * only if a single candidate bean of type ConnectionFactory is available.
	 * @param configurer The configurer used to configure the default JMS listener
	 * container factory.
	 * @param connectionFactory The connection factory used by the default JMS listener
	 * container factory.
	 * @return The default JMS listener container factory.
	 */
	@Bean
	@ConditionalOnSingleCandidate(ConnectionFactory.class)
	@ConditionalOnMissingBean(name = "jmsListenerContainerFactory")
	DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
			DefaultJmsListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		return factory;
	}

	/**
	 * EnableJmsConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@EnableJms
	@ConditionalOnMissingBean(name = JmsListenerConfigUtils.JMS_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
	static class EnableJmsConfiguration {

	}

	/**
	 * JndiConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnJndi
	static class JndiConfiguration {

		/**
		 * Creates a JndiDestinationResolver bean if no other bean of type
		 * DestinationResolver is present.
		 *
		 * The JndiDestinationResolver is responsible for resolving JNDI destinations for
		 * messaging operations. It is configured to fallback to dynamic destinations if
		 * the JNDI lookup fails.
		 * @return the JndiDestinationResolver bean
		 */
		@Bean
		@ConditionalOnMissingBean(DestinationResolver.class)
		JndiDestinationResolver destinationResolver() {
			JndiDestinationResolver resolver = new JndiDestinationResolver();
			resolver.setFallbackToDynamicDestination(true);
			return resolver;
		}

	}

}
