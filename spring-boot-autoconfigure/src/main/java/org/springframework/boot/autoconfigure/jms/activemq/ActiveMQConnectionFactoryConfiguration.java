/*
 * Copyright 2012-2015 the original author or authors.
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

import java.lang.reflect.Method;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;

/**
 * Configuration for ActiveMQ {@link ConnectionFactory}.
 *
 * @author Greg Turnquist
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.1.0
 */
@Configuration
@ConditionalOnMissingBean(ConnectionFactory.class)
class ActiveMQConnectionFactoryConfiguration {

	@Bean
	public ConnectionFactory jmsConnectionFactory(ActiveMQProperties properties) {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactoryFactory(
				properties).createConnectionFactory(ActiveMQConnectionFactory.class);
		if (properties.isPooled()) {
			PooledConnectionFactory pool = new PooledConnectionFactory();
			Method connectionFactorySetter = findConnectionFactorySetter();
			if (connectionFactorySetter != null) {
				ReflectionUtils.invokeMethod(connectionFactorySetter, pool,
						connectionFactory);
			}
			else {
				throw new IllegalStateException(
						"No supported setConnectionFactory method was found");
			}
			return pool;
		}
		return connectionFactory;
	}

	private Method findConnectionFactorySetter() {
		Method setter = findConnectionFactorySetter(ConnectionFactory.class);
		if (setter == null) {
			setter = findConnectionFactorySetter(Object.class);
		}
		return setter;
	}

	private Method findConnectionFactorySetter(Class<?> param) {
		return ReflectionUtils.findMethod(PooledConnectionFactory.class,
				"setConnectionFactory", param);
	}

}
