/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jms.autoconfigure;

import java.time.Duration;

import jakarta.jms.ConnectionFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.jms.autoconfigure.JmsProperties.Listener.Session;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * Configure {@link DefaultJmsListenerContainerFactory} with sensible defaults tuned using
 * configuration properties.
 * <p>
 * Can be injected into application code and used to define a custom
 * {@code DefaultJmsListenerContainerFactory} whose configuration is based upon that
 * produced by auto-configuration.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Vedran Pavic
 * @author Lasse Wulff
 * @since 4.0.0
 * @see SimpleJmsListenerContainerFactoryConfigurer
 */
public final class DefaultJmsListenerContainerFactoryConfigurer
		extends AbstractJmsListenerContainerFactoryConfigurer<DefaultJmsListenerContainerFactory> {

	private @Nullable JtaTransactionManager transactionManager;

	/**
	 * Set the {@link JtaTransactionManager} to use or {@code null} if the JTA support
	 * should not be used.
	 * @param transactionManager the {@link JtaTransactionManager}
	 */
	void setTransactionManager(@Nullable JtaTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void configure(DefaultJmsListenerContainerFactory factory, ConnectionFactory connectionFactory) {
		super.configure(factory, connectionFactory);
		PropertyMapper map = PropertyMapper.get();
		JmsProperties.Listener listenerProperties = getJmsProperties().getListener();
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
