/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jms.autoconfigure;

import jakarta.jms.ConnectionFactory;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;

/**
 * Factory to create a {@link JmsPoolConnectionFactory} from properties defined in
 * {@link JmsPoolConnectionFactoryProperties}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public class JmsPoolConnectionFactoryFactory {

	private final JmsPoolConnectionFactoryProperties properties;

	public JmsPoolConnectionFactoryFactory(JmsPoolConnectionFactoryProperties properties) {
		this.properties = properties;
	}

	/**
	 * Create a {@link JmsPoolConnectionFactory} based on the specified
	 * {@link ConnectionFactory}.
	 * @param connectionFactory the connection factory to wrap
	 * @return a pooled connection factory
	 */
	public JmsPoolConnectionFactory createPooledConnectionFactory(ConnectionFactory connectionFactory) {
		JmsPoolConnectionFactory pooledConnectionFactory = new JmsPoolConnectionFactory();
		pooledConnectionFactory.setConnectionFactory(connectionFactory);

		pooledConnectionFactory.setBlockIfSessionPoolIsFull(this.properties.isBlockIfFull());
		if (this.properties.getBlockIfFullTimeout() != null) {
			pooledConnectionFactory
				.setBlockIfSessionPoolIsFullTimeout(this.properties.getBlockIfFullTimeout().toMillis());
		}
		if (this.properties.getIdleTimeout() != null) {
			pooledConnectionFactory.setConnectionIdleTimeout((int) this.properties.getIdleTimeout().toMillis());
		}
		pooledConnectionFactory.setMaxConnections(this.properties.getMaxConnections());
		pooledConnectionFactory.setMaxSessionsPerConnection(this.properties.getMaxSessionsPerConnection());
		if (this.properties.getTimeBetweenExpirationCheck() != null) {
			pooledConnectionFactory
				.setConnectionCheckInterval(this.properties.getTimeBetweenExpirationCheck().toMillis());
		}
		pooledConnectionFactory.setUseAnonymousProducers(this.properties.isUseAnonymousProducers());
		return pooledConnectionFactory;
	}

}
