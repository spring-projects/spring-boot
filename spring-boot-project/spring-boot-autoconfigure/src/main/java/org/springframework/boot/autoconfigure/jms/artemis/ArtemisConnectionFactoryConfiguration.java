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

package org.springframework.boot.autoconfigure.jms.artemis;

import javax.jms.ConnectionFactory;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.commons.pool2.PooledObject;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.JmsPoolConnectionFactoryFactory;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;

/**
 * Configuration for Artemis {@link ConnectionFactory}.
 *
 * @author Eddú Meléndez
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@Configuration
@ConditionalOnMissingBean(ConnectionFactory.class)
class ArtemisConnectionFactoryConfiguration {

	@Configuration
	@ConditionalOnClass(CachingConnectionFactory.class)
	@ConditionalOnProperty(prefix = "spring.artemis.pool", name = "enabled", havingValue = "false", matchIfMissing = true)
	static class SimpleConnectionFactoryConfiguration {

		private final JmsProperties jmsProperties;

		private final ArtemisProperties properties;

		private final ListableBeanFactory beanFactory;

		SimpleConnectionFactoryConfiguration(JmsProperties jmsProperties,
				ArtemisProperties properties, ListableBeanFactory beanFactory) {
			this.jmsProperties = jmsProperties;
			this.properties = properties;
			this.beanFactory = beanFactory;
		}

		@Bean
		@ConditionalOnProperty(prefix = "spring.jms.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
		public CachingConnectionFactory cachingJmsConnectionFactory() {
			JmsProperties.Cache cacheProperties = this.jmsProperties.getCache();
			CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
					createConnectionFactory());
			connectionFactory.setCacheConsumers(cacheProperties.isConsumers());
			connectionFactory.setCacheProducers(cacheProperties.isProducers());
			connectionFactory.setSessionCacheSize(cacheProperties.getSessionCacheSize());
			return connectionFactory;
		}

		@Bean
		@ConditionalOnProperty(prefix = "spring.jms.cache", name = "enabled", havingValue = "false")
		public ActiveMQConnectionFactory jmsConnectionFactory() {
			return createConnectionFactory();
		}

		private ActiveMQConnectionFactory createConnectionFactory() {
			return new ArtemisConnectionFactoryFactory(this.beanFactory, this.properties)
					.createConnectionFactory(ActiveMQConnectionFactory.class);
		}

	}

	@Configuration
	@ConditionalOnClass({ JmsPoolConnectionFactory.class, PooledObject.class })
	static class PooledConnectionFactoryConfiguration {

		@Bean(destroyMethod = "stop")
		@ConditionalOnProperty(prefix = "spring.artemis.pool", name = "enabled", havingValue = "true", matchIfMissing = false)
		public JmsPoolConnectionFactory pooledJmsConnectionFactory(
				ListableBeanFactory beanFactory, ArtemisProperties properties) {
			ActiveMQConnectionFactory connectionFactory = new ArtemisConnectionFactoryFactory(
					beanFactory, properties)
							.createConnectionFactory(ActiveMQConnectionFactory.class);
			return new JmsPoolConnectionFactoryFactory(properties.getPool())
					.createPooledConnectionFactory(connectionFactory);
		}

	}

}
