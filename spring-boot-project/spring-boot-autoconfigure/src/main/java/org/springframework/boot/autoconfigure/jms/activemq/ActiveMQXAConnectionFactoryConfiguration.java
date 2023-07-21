/*
 * Copyright 2012-2023 the original author or authors.
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

import jakarta.jms.ConnectionFactory;
import jakarta.transaction.TransactionManager;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQXAConnectionFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jms.XAConnectionFactoryWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for ActiveMQ XA {@link ConnectionFactory}.
 *
 * @author Phillip Webb
 * @author Aurélien Leboulanger
 * @author Eddú Meléndez
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(TransactionManager.class)
@ConditionalOnBean(XAConnectionFactoryWrapper.class)
@ConditionalOnMissingBean(ConnectionFactory.class)
class ActiveMQXAConnectionFactoryConfiguration {

	@Primary
	@Bean(name = { "jmsConnectionFactory", "xaJmsConnectionFactory" })
	ConnectionFactory jmsConnectionFactory(ActiveMQProperties properties,
			ObjectProvider<ActiveMQConnectionFactoryCustomizer> factoryCustomizers, XAConnectionFactoryWrapper wrapper,
			ActiveMQConnectionDetails connectionDetails) throws Exception {
		ActiveMQXAConnectionFactory connectionFactory = new ActiveMQConnectionFactoryFactory(properties,
				factoryCustomizers.orderedStream().toList(), connectionDetails)
			.createConnectionFactory(ActiveMQXAConnectionFactory.class);
		return wrapper.wrapConnectionFactory(connectionFactory);
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.activemq.pool", name = "enabled", havingValue = "false",
			matchIfMissing = true)
	ActiveMQConnectionFactory nonXaJmsConnectionFactory(ActiveMQProperties properties,
			ObjectProvider<ActiveMQConnectionFactoryCustomizer> factoryCustomizers,
			ActiveMQConnectionDetails connectionDetails) {
		return new ActiveMQConnectionFactoryFactory(properties, factoryCustomizers.orderedStream().toList(),
				connectionDetails)
			.createConnectionFactory(ActiveMQConnectionFactory.class);
	}

}
