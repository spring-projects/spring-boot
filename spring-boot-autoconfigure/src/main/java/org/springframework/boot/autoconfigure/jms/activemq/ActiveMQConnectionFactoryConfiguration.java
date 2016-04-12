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

package org.springframework.boot.autoconfigure.jms.activemq;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for ActiveMQ {@link ConnectionFactory}.
 *
 * @author Greg Turnquist
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Aurélien Leboulanger
 * @since 1.1.0
 */
@Configuration
@ConditionalOnMissingBean(ConnectionFactory.class)
class ActiveMQConnectionFactoryConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "spring.activemq.pool", name = "enabled", havingValue = "false", matchIfMissing = true)
	public ActiveMQConnectionFactory jmsConnectionFactory(ActiveMQProperties properties) {
		return new ActiveMQConnectionFactoryFactory(properties)
				.createConnectionFactory(ActiveMQConnectionFactory.class);
	}

	@ConditionalOnClass(PooledConnectionFactory.class)
	static class PooledConnectionFactoryConfiguration {

		@Bean(destroyMethod = "stop")
		@ConditionalOnProperty(prefix = "spring.activemq.pool", name = "enabled", havingValue = "true", matchIfMissing = false)
		@ConfigurationProperties("spring.activemq.pool.configuration")
		public PooledConnectionFactory pooledJmsConnectionFactory(
				ActiveMQProperties properties) {
			PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory(
					new ActiveMQConnectionFactoryFactory(properties)
							.createConnectionFactory(ActiveMQConnectionFactory.class));

			ActiveMQProperties.Pool pool = properties.getPool();
			pooledConnectionFactory.setMaxConnections(pool.getMaxConnections());
			pooledConnectionFactory.setIdleTimeout(pool.getIdleTimeout());
			pooledConnectionFactory.setExpiryTimeout(pool.getExpiryTimeout());
			return pooledConnectionFactory;
		}

	}

}
