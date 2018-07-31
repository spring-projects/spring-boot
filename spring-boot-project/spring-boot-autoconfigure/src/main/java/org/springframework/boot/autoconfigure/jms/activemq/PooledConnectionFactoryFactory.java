/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms.activemq;

import javax.jms.ConnectionFactory;

import org.apache.activemq.jms.pool.PooledConnectionFactory;

/**
 * Factory to create a {@link PooledConnectionFactory} from properties defined in
 * {@link PooledConnectionFactoryProperties}.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
 */
public class PooledConnectionFactoryFactory {

	private final PooledConnectionFactoryProperties properties;

	public PooledConnectionFactoryFactory(PooledConnectionFactoryProperties properties) {
		this.properties = properties;
	}

	/**
	 * Create a {@link PooledConnectionFactory} based on the specified
	 * {@link ConnectionFactory}.
	 * @param connectionFactory the connection factory to wrap
	 * @return a pooled connection factory
	 */
	public PooledConnectionFactory createPooledConnectionFactory(
			ConnectionFactory connectionFactory) {
		PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
		pooledConnectionFactory.setConnectionFactory(connectionFactory);

		pooledConnectionFactory
				.setBlockIfSessionPoolIsFull(this.properties.isBlockIfFull());
		if (this.properties.getBlockIfFullTimeout() != null) {
			pooledConnectionFactory.setBlockIfSessionPoolIsFullTimeout(
					this.properties.getBlockIfFullTimeout().toMillis());
		}
		if (this.properties.getIdleTimeout() != null) {
			pooledConnectionFactory
					.setIdleTimeout((int) this.properties.getIdleTimeout().toMillis());
		}
		pooledConnectionFactory.setMaxConnections(this.properties.getMaxConnections());
		pooledConnectionFactory.setMaximumActiveSessionPerConnection(
				this.properties.getMaximumActiveSessionPerConnection());
		if (this.properties.getTimeBetweenExpirationCheck() != null) {
			pooledConnectionFactory.setTimeBetweenExpirationCheckMillis(
					this.properties.getTimeBetweenExpirationCheck().toMillis());
		}
		pooledConnectionFactory
				.setUseAnonymousProducers(this.properties.isUseAnonymousProducers());
		return pooledConnectionFactory;
	}

}
