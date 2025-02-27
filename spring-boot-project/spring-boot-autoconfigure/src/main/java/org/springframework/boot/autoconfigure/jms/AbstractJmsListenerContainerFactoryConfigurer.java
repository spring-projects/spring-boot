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

import org.springframework.boot.autoconfigure.jms.JmsProperties.Listener.Session;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.jms.config.AbstractJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.util.Assert;

/**
 * Configures {@link AbstractJmsListenerContainerFactory} with sensible defaults.
 *
 * @param <T> the connection factory type.
 * @author Vedran Pavic
 * @since 3.5.0
 */
public abstract class AbstractJmsListenerContainerFactoryConfigurer<T extends AbstractJmsListenerContainerFactory<?>> {

	private DestinationResolver destinationResolver;

	private MessageConverter messageConverter;

	private ExceptionListener exceptionListener;

	private ObservationRegistry observationRegistry;

	private JmsProperties jmsProperties;

	/**
	 * Set the {@link DestinationResolver} to use or {@code null} if no destination
	 * resolver should be associated with the factory by default.
	 * @param destinationResolver the {@link DestinationResolver}
	 */
	void setDestinationResolver(DestinationResolver destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	/**
	 * Set the {@link MessageConverter} to use or {@code null} if the out-of-the-box
	 * converter should be used.
	 * @param messageConverter the {@link MessageConverter}
	 */
	void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Set the {@link ExceptionListener} to use or {@code null} if no exception listener
	 * should be associated by default.
	 * @param exceptionListener the {@link ExceptionListener}
	 */
	void setExceptionListener(ExceptionListener exceptionListener) {
		this.exceptionListener = exceptionListener;
	}

	/**
	 * Set the {@link ObservationRegistry} to use.
	 * @param observationRegistry the {@link ObservationRegistry}
	 */
	void setObservationRegistry(ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
	}

	/**
	 * Set the {@link JmsProperties} to use.
	 * @param jmsProperties the {@link JmsProperties}
	 */
	void setJmsProperties(JmsProperties jmsProperties) {
		this.jmsProperties = jmsProperties;
	}

	/**
	 * Configure the specified jms listener container factory. The factory can be further
	 * tuned and default settings can be overridden.
	 * @param factory the {@link AbstractJmsListenerContainerFactory} instance to
	 * configure
	 * @param connectionFactory the {@link ConnectionFactory} to use
	 */
	public void configure(T factory, ConnectionFactory connectionFactory) {
		Assert.notNull(factory, "'factory' must not be null");
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");
		JmsProperties.Listener listenerProperties = this.jmsProperties.getListener();
		Session sessionProperties = listenerProperties.getSession();
		factory.setConnectionFactory(connectionFactory);
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(this.jmsProperties::isPubSubDomain).to(factory::setPubSubDomain);
		map.from(this.jmsProperties::isSubscriptionDurable).to(factory::setSubscriptionDurable);
		map.from(this.jmsProperties::getClientId).to(factory::setClientId);
		map.from(this.destinationResolver).to(factory::setDestinationResolver);
		map.from(this.messageConverter).to(factory::setMessageConverter);
		map.from(this.exceptionListener).to(factory::setExceptionListener);
		map.from(sessionProperties.getAcknowledgeMode()::getMode).to(factory::setSessionAcknowledgeMode);
		map.from(this.observationRegistry).to(factory::setObservationRegistry);
		map.from(sessionProperties::getTransacted).to(factory::setSessionTransacted);
		map.from(listenerProperties::isAutoStartup).to(factory::setAutoStartup);
		configure(factory, connectionFactory, this.jmsProperties);
	}

	/**
	 * Configures the given {@code factory} using the given {@code connectionFactory} and
	 * {@code jmsProperties}.
	 * @param factory the {@link AbstractJmsListenerContainerFactory} instance to
	 * configure
	 * @param connectionFactory the {@link ConnectionFactory} to use
	 * @param jmsProperties the {@link JmsProperties} to use
	 */
	protected abstract void configure(T factory, ConnectionFactory connectionFactory, JmsProperties jmsProperties);

}
