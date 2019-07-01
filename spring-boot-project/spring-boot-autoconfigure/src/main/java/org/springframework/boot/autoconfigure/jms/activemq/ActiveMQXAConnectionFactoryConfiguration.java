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

package org.springframework.boot.autoconfigure.jms.activemq;

import java.util.List;
import java.util.stream.Collectors;

import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;
import javax.transaction.TransactionManager;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQSslConnectionFactory;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.apache.activemq.ActiveMQXASslConnectionFactory;

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
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(TransactionManager.class)
@ConditionalOnBean(XAConnectionFactoryWrapper.class)
@ConditionalOnMissingBean(ConnectionFactory.class)
class ActiveMQXAConnectionFactoryConfiguration {

	private final ActiveMQProperties properties;

	private final List<ActiveMQConnectionFactoryCustomizer> connectionFactoryCustomizers;

	ActiveMQXAConnectionFactoryConfiguration(ActiveMQProperties properties,
			ObjectProvider<ActiveMQConnectionFactoryCustomizer> connectionFactoryCustomizers) {
		this.properties = properties;
		this.connectionFactoryCustomizers = connectionFactoryCustomizers.orderedStream().collect(Collectors.toList());
	}

	private static XAConnectionFactory createXAConnectionFactory(ActiveMQProperties properties,
			List<ActiveMQConnectionFactoryCustomizer> connectionFactoryCustomizers) {
		boolean ssl = properties.getSsl().isEnabled();
		ActiveMQConnectionFactoryFactory factory = new ActiveMQConnectionFactoryFactory(properties,
				connectionFactoryCustomizers);
		return ssl ? factory.createSslConnectionFactory(ActiveMQXASslConnectionFactory.class)
				: factory.createConnectionFactory(ActiveMQXAConnectionFactory.class);
	}

	private static ActiveMQConnectionFactory createConnectionFactory(ActiveMQProperties properties,
			List<ActiveMQConnectionFactoryCustomizer> connectionFactoryCustomizers) {
		boolean ssl = properties.getSsl().isEnabled();
		ActiveMQConnectionFactoryFactory factory = new ActiveMQConnectionFactoryFactory(properties,
				connectionFactoryCustomizers);
		return ssl ? factory.createSslConnectionFactory(ActiveMQSslConnectionFactory.class)
				: factory.createConnectionFactory(ActiveMQConnectionFactory.class);
	}

	@Primary
	@Bean(name = { "jmsConnectionFactory", "xaJmsConnectionFactory" })
	ConnectionFactory jmsConnectionFactory(XAConnectionFactoryWrapper wrapper) throws Exception {
		XAConnectionFactory xaConnectionFactory = createXAConnectionFactory(this.properties,
				this.connectionFactoryCustomizers);
		return wrapper.wrapConnectionFactory(xaConnectionFactory);
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.activemq.pool", name = "enabled", havingValue = "false",
			matchIfMissing = true)
	ActiveMQConnectionFactory nonXaJmsConnectionFactory() {
		return createConnectionFactory(this.properties, this.connectionFactoryCustomizers);
	}

}
