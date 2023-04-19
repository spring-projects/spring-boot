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
import org.apache.activemq.ActiveMQConnectionFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.autoconfigure.jms.JndiConnectionFactoryAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to integrate with an ActiveMQ
 * broker.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @since 3.1.0
 */
@AutoConfiguration(before = JmsAutoConfiguration.class, after = JndiConnectionFactoryAutoConfiguration.class)
@ConditionalOnClass({ ConnectionFactory.class, ActiveMQConnectionFactory.class })
@ConditionalOnMissingBean(ConnectionFactory.class)
@EnableConfigurationProperties({ ActiveMQProperties.class, JmsProperties.class })
@Import({ ActiveMQXAConnectionFactoryConfiguration.class, ActiveMQConnectionFactoryConfiguration.class })
public class ActiveMQAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ActiveMQConnectionDetails.class)
	ActiveMQConnectionDetails activemqConnectionDetails(ActiveMQProperties properties) {
		return new PropertiesActiveMQConnectionDetails(properties);
	}

	/**
	 * Adapts {@link ActiveMQProperties} to {@link ActiveMQConnectionDetails}.
	 */
	static class PropertiesActiveMQConnectionDetails implements ActiveMQConnectionDetails {

		private final ActiveMQProperties properties;

		PropertiesActiveMQConnectionDetails(ActiveMQProperties properties) {
			this.properties = properties;
		}

		@Override
		public String getBrokerUrl() {
			return this.properties.determineBrokerUrl();
		}

		@Override
		public String getUser() {
			return this.properties.getUser();
		}

		@Override
		public String getPassword() {
			return this.properties.getPassword();
		}

	}

}
