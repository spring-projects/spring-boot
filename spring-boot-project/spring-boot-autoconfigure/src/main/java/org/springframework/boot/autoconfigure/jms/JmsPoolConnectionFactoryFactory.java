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

package org.springframework.boot.autoconfigure.jms;

import javax.jms.ConnectionFactory;

import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;

/**
 * Factory to create a {@link JmsPoolConnectionFactory} from properties defined in
 * {@link JmsPoolConnectionFactoryProperties}.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
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
