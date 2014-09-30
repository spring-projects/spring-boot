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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for ActiveMQ {@link ConnectionFactory}.
 *
 * @author Greg Turnquist
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.1.0
 */
@Configuration
class ActiveMQConnectionFactoryConfiguration {

	private static final String DEFAULT_MQTT_BROKER_URL = "mqtt://localhost:1883";

	@Bean
	@Qualifier("autodetectedAmqConnectionUrl")
	@ConditionalOnClass(name = "org.apache.activemq.transport.mqtt.MQTTTransport")
	public String mqttAmqConnectionUrl() {
		return DEFAULT_MQTT_BROKER_URL;
	}

	@Autowired(required = false)
	@Qualifier("autodetectedAmqConnectionUrl")
	private String autodetectedAmqConnectionUrl;

	@Bean
	public ConnectionFactory jmsConnectionFactory(ActiveMQProperties properties) {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactoryFactory(
				properties, autodetectedAmqConnectionUrl).createConnectionFactory(ActiveMQConnectionFactory.class);
		if (properties.isPooled()) {
			PooledConnectionFactory pool = new PooledConnectionFactory();
			pool.setConnectionFactory(connectionFactory);
			return pool;
		}
		return connectionFactory;
	}

}
