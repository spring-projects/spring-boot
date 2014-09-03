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

package org.springframework.boot.autoconfigure.jms.hornetq;

import javax.jms.ConnectionFactory;
import javax.transaction.TransactionManager;

import org.hornetq.jms.client.HornetQConnectionFactory;
import org.hornetq.jms.client.HornetQXAConnectionFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jta.XAConnectionFactoryWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for HornetQ XA {@link ConnectionFactory}.
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
@Configuration
@ConditionalOnMissingBean(ConnectionFactory.class)
@ConditionalOnClass(TransactionManager.class)
@ConditionalOnBean(XAConnectionFactoryWrapper.class)
class HornetQXAConnectionFactoryConfiguration {

	@Primary
	@Bean(name = { "jmsConnectionFactory", "xaJmsConnectionFactory" })
	public ConnectionFactory jmsConnectionFactory(ListableBeanFactory beanFactory,
			HornetQProperties properties, XAConnectionFactoryWrapper wrapper)
			throws Exception {
		return wrapper.wrapConnectionFactory(new HornetQConnectionFactoryFactory(
				beanFactory, properties)
				.createConnectionFactory(HornetQXAConnectionFactory.class));
	}

	@Bean
	public ConnectionFactory nonXaJmsConnectionFactory(ListableBeanFactory beanFactory,
			HornetQProperties properties) {
		return new HornetQConnectionFactoryFactory(beanFactory, properties)
				.createConnectionFactory(HornetQConnectionFactory.class);
	}
}
