/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms.activemq;

import java.util.stream.Collectors;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.pool2.PooledObject;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.JmsPoolConnectionFactoryFactory;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;

/**
 * Configuration for ActiveMQ {@link ConnectionFactory}.
 *
 * @author Greg Turnquist
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Aurélien Leboulanger
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(ConnectionFactory.class)
class ActiveMQConnectionFactoryConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.activemq.pool", name = "enabled", havingValue = "false",
			matchIfMissing = true)
	static class SimpleConnectionFactoryConfiguration {

		@Bean
		@ConditionalOnProperty(prefix = "spring.jms.cache", name = "enabled", havingValue = "false")
		ActiveMQConnectionFactory jmsConnectionFactory(ActiveMQProperties properties,
				ObjectProvider<ActiveMQConnectionFactoryCustomizer> factoryCustomizers) {
			return createJmsConnectionFactory(properties, factoryCustomizers);
		}

		private static ActiveMQConnectionFactory createJmsConnectionFactory(ActiveMQProperties properties,
				ObjectProvider<ActiveMQConnectionFactoryCustomizer> factoryCustomizers) {
			return new ActiveMQConnectionFactoryFactory(properties,
					factoryCustomizers.orderedStream().collect(Collectors.toList()))
							.createConnectionFactory(ActiveMQConnectionFactory.class);
		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass(CachingConnectionFactory.class)
		@ConditionalOnProperty(prefix = "spring.jms.cache", name = "enabled", havingValue = "true",
				matchIfMissing = true)
		static class CachingConnectionFactoryConfiguration {

			@Bean
			CachingConnectionFactory jmsConnectionFactory(JmsProperties jmsProperties, ActiveMQProperties properties,
					ObjectProvider<ActiveMQConnectionFactoryCustomizer> factoryCustomizers) {
				JmsProperties.Cache cacheProperties = jmsProperties.getCache();
				CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
						createJmsConnectionFactory(properties, factoryCustomizers));
				connectionFactory.setCacheConsumers(cacheProperties.isConsumers());
				connectionFactory.setCacheProducers(cacheProperties.isProducers());
				connectionFactory.setSessionCacheSize(cacheProperties.getSessionCacheSize());
				return connectionFactory;
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ JmsPoolConnectionFactory.class, PooledObject.class })
	static class PooledConnectionFactoryConfiguration {

		@Bean(destroyMethod = "stop")
		@ConditionalOnProperty(prefix = "spring.activemq.pool", name = "enabled", havingValue = "true")
		JmsPoolConnectionFactory jmsConnectionFactory(ActiveMQProperties properties,
				ObjectProvider<ActiveMQConnectionFactoryCustomizer> factoryCustomizers) {
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactoryFactory(properties,
					factoryCustomizers.orderedStream().collect(Collectors.toList()))
							.createConnectionFactory(ActiveMQConnectionFactory.class);
			return new JmsPoolConnectionFactoryFactory(properties.getPool())
					.createPooledConnectionFactory(connectionFactory);
		}

	}

}
