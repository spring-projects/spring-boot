/*
 * Copyright 2012-2014 the original author or authors.
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
import javax.transaction.TransactionManager;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jta.XAConnectionFactoryWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for ActiveMQ XA {@link ConnectionFactory}.
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
@Configuration
@ConditionalOnClass(TransactionManager.class)
@ConditionalOnBean(XAConnectionFactoryWrapper.class)
@ConditionalOnMissingBean(ConnectionFactory.class)
class ActiveMQXAConnectionFactoryConfiguration {

	@Primary
	@Bean(name = { "jmsConnectionFactory", "xaJmsConnectionFactory" })
	public ConnectionFactory jmsConnectionFactory(ActiveMQProperties properties,
			XAConnectionFactoryWrapper wrapper) throws Exception {
		ActiveMQXAConnectionFactory connectionFactory = new ActiveMQConnectionFactoryFactory(
				properties).createConnectionFactory(ActiveMQXAConnectionFactory.class);
		return wrapper.wrapConnectionFactory(connectionFactory);
	}

	@Bean
	public ConnectionFactory nonXaJmsConnectionFactory(ActiveMQProperties properties) {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactoryFactory(
				properties).createConnectionFactory(ActiveMQConnectionFactory.class);
		if (properties.isPooled()) {
			PooledConnectionFactory pool = new PooledConnectionFactory();
			pool.setConnectionFactory(connectionFactory);
			return pool;
		}
		return connectionFactory;
	}

}
