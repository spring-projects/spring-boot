/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms.artemis;

import jakarta.jms.ConnectionFactory;
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
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(ConnectionFactory.class)
class ArtemisConnectionFactoryConfiguration {

	/**
     * SimpleConnectionFactoryConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.artemis.pool", name = "enabled", havingValue = "false",
			matchIfMissing = true)
	static class SimpleConnectionFactoryConfiguration {

		/**
         * Creates a JMS connection factory bean with the given properties, connection details, and bean factory.
         * This method is annotated with @Bean and @ConditionalOnProperty to conditionally create the bean based on the
         * value of the "spring.jms.cache.enabled" property. If the property is set to "false", the bean will be created.
         * 
         * @param properties         the ArtemisProperties object containing the JMS properties
         * @param beanFactory        the ListableBeanFactory object used to access other beans
         * @param connectionDetails  the ArtemisConnectionDetails object containing the connection details
         * @return                   the created ActiveMQConnectionFactory bean
         */
        @Bean(name = "jmsConnectionFactory")
		@ConditionalOnProperty(prefix = "spring.jms.cache", name = "enabled", havingValue = "false")
		ActiveMQConnectionFactory jmsConnectionFactory(ArtemisProperties properties, ListableBeanFactory beanFactory,
				ArtemisConnectionDetails connectionDetails) {
			return createJmsConnectionFactory(properties, connectionDetails, beanFactory);
		}

		/**
         * Creates a JMS connection factory using the provided Artemis properties, connection details, and bean factory.
         * 
         * @param properties the Artemis properties used to configure the connection factory
         * @param connectionDetails the Artemis connection details used to establish the connection
         * @param beanFactory the bean factory used to create the connection factory
         * @return the created JMS connection factory
         */
        private static ActiveMQConnectionFactory createJmsConnectionFactory(ArtemisProperties properties,
				ArtemisConnectionDetails connectionDetails, ListableBeanFactory beanFactory) {
			return new ArtemisConnectionFactoryFactory(beanFactory, properties, connectionDetails)
				.createConnectionFactory(ActiveMQConnectionFactory.class);
		}

		/**
         * CachingConnectionFactoryConfiguration class.
         */
        @Configuration(proxyBeanMethods = false)
		@ConditionalOnClass(CachingConnectionFactory.class)
		@ConditionalOnProperty(prefix = "spring.jms.cache", name = "enabled", havingValue = "true",
				matchIfMissing = true)
		static class CachingConnectionFactoryConfiguration {

			/**
             * Creates a caching JMS connection factory bean with the given JMS properties, Artemis properties,
             * Artemis connection details, and bean factory.
             *
             * @param jmsProperties      the JMS properties
             * @param properties         the Artemis properties
             * @param connectionDetails  the Artemis connection details
             * @param beanFactory        the bean factory
             * @return the caching JMS connection factory bean
             */
            @Bean(name = "jmsConnectionFactory")
			CachingConnectionFactory cachingJmsConnectionFactory(JmsProperties jmsProperties,
					ArtemisProperties properties, ArtemisConnectionDetails connectionDetails,
					ListableBeanFactory beanFactory) {
				JmsProperties.Cache cacheProperties = jmsProperties.getCache();
				CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
						createJmsConnectionFactory(properties, connectionDetails, beanFactory));
				connectionFactory.setCacheConsumers(cacheProperties.isConsumers());
				connectionFactory.setCacheProducers(cacheProperties.isProducers());
				connectionFactory.setSessionCacheSize(cacheProperties.getSessionCacheSize());
				return connectionFactory;
			}

		}

	}

	/**
     * PooledConnectionFactoryConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ JmsPoolConnectionFactory.class, PooledObject.class })
	@ConditionalOnProperty(prefix = "spring.artemis.pool", name = "enabled", havingValue = "true")
	static class PooledConnectionFactoryConfiguration {

		/**
         * Creates a JmsPoolConnectionFactory bean with the specified properties.
         *
         * @param beanFactory        the ListableBeanFactory used to create the connection factory
         * @param properties         the ArtemisProperties used to configure the connection factory
         * @param connectionDetails  the ArtemisConnectionDetails used to establish the connection
         * @return the JmsPoolConnectionFactory bean
         */
        @Bean(destroyMethod = "stop")
		JmsPoolConnectionFactory jmsConnectionFactory(ListableBeanFactory beanFactory, ArtemisProperties properties,
				ArtemisConnectionDetails connectionDetails) {
			ActiveMQConnectionFactory connectionFactory = new ArtemisConnectionFactoryFactory(beanFactory, properties,
					connectionDetails)
				.createConnectionFactory(ActiveMQConnectionFactory.class);
			return new JmsPoolConnectionFactoryFactory(properties.getPool())
				.createPooledConnectionFactory(connectionFactory);
		}

	}

}
