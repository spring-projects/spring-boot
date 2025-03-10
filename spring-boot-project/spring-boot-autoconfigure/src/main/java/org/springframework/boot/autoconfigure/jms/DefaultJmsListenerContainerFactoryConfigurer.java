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

import java.time.Duration;

import io.micrometer.observation.ObservationRegistry;
import jakarta.jms.ConnectionFactory;

import org.springframework.boot.autoconfigure.jms.JmsProperties.Listener.Session;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * Configures {@link DefaultJmsListenerContainerFactory} with sensible defaults.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Vedran Pavic
 * @author Lasse Wulff
 * @since 1.3.3
 */
public final class DefaultJmsListenerContainerFactoryConfigurer
		extends AbstractJmsListenerContainerFactoryConfigurer<DefaultJmsListenerContainerFactory> {

	private JtaTransactionManager transactionManager;

	/**
	 * Set the {@link JtaTransactionManager} to use or {@code null} if the JTA support
	 * should not be used.
	 * @param transactionManager the {@link JtaTransactionManager}
	 */
	void setTransactionManager(JtaTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * Set the {@link ObservationRegistry} to use.
	 * @param observationRegistry the {@link ObservationRegistry}
	 * @since 3.2.1
	 * @deprecated since 3.3.10 for removal in 3.6.0 as this should have been package
	 * private
	 */
	@Override
	@Deprecated(since = "3.3.10", forRemoval = true)
	public void setObservationRegistry(ObservationRegistry observationRegistry) {
		super.setObservationRegistry(observationRegistry);
	}

	@Override
	protected void configure(DefaultJmsListenerContainerFactory factory, ConnectionFactory connectionFactory,
			JmsProperties jmsProperties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		JmsProperties.Listener listenerProperties = jmsProperties.getListener();
		Session sessionProperties = listenerProperties.getSession();
		map.from(this.transactionManager).to(factory::setTransactionManager);
		if (this.transactionManager == null && sessionProperties.getTransacted() == null) {
			factory.setSessionTransacted(true);
		}
		map.from(listenerProperties::formatConcurrency).to(factory::setConcurrency);
		map.from(listenerProperties::getReceiveTimeout).as(Duration::toMillis).to(factory::setReceiveTimeout);
		map.from(listenerProperties::getMaxMessagesPerTask).to(factory::setMaxMessagesPerTask);
	}

}
