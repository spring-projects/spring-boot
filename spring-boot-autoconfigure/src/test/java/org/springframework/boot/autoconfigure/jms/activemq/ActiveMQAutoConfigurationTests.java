/*
 * Copyright 2012-2017 the original author or authors.
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
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.test.context.ApplicationContextTester;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

/**
 * Tests for {@link ActiveMQAutoConfiguration}
 *
 * @author Andy Wilkinson
 * @author Aurélien Leboulanger
 * @author Stephane Nicoll
 */
public class ActiveMQAutoConfigurationTests {

	private final ApplicationContextTester context = new ApplicationContextTester()
			.withConfiguration(AutoConfigurations.of(ActiveMQAutoConfiguration.class,
					JmsAutoConfiguration.class));

	@Test
	public void brokerIsEmbeddedByDefault() {
		this.context.withUserConfiguration(EmptyConfiguration.class).run((loaded) -> {
			assertThat(loaded).getBean(ConnectionFactory.class)
					.isInstanceOf(ActiveMQConnectionFactory.class);
			assertThat(loaded.getBean(ActiveMQConnectionFactory.class).getBrokerURL())
					.isEqualTo("vm://localhost?broker.persistent=false");
		});
	}

	@Test
	public void configurationBacksOffWhenCustomConnectionFactoryExists() {
		this.context.withUserConfiguration(CustomConnectionFactoryConfiguration.class)
				.run((loaded) -> assertThat(
						mockingDetails(loaded.getBean(ConnectionFactory.class)).isMock())
								.isTrue());
	}

	@Test
	public void customPooledConnectionFactoryConfiguration() {
		this.context.withUserConfiguration(EmptyConfiguration.class)
				.withPropertyValues("spring.activemq.pool.enabled:true",
						"spring.activemq.pool.maxConnections:256",
						"spring.activemq.pool.idleTimeout:512",
						"spring.activemq.pool.expiryTimeout:4096",
						"spring.activemq.pool.configuration.maximumActiveSessionPerConnection:1024",
						"spring.activemq.pool.configuration.timeBetweenExpirationCheckMillis:2048")
				.run((loaded) -> {
					ConnectionFactory factory = loaded.getBean(ConnectionFactory.class);
					assertThat(factory).isInstanceOf(PooledConnectionFactory.class);
					PooledConnectionFactory pooledFactory = (PooledConnectionFactory) factory;
					assertThat(pooledFactory.getMaxConnections()).isEqualTo(256);
					assertThat(pooledFactory.getIdleTimeout()).isEqualTo(512);
					assertThat(pooledFactory.getMaximumActiveSessionPerConnection())
							.isEqualTo(1024);
					assertThat(pooledFactory.getTimeBetweenExpirationCheckMillis())
							.isEqualTo(2048);
					assertThat(pooledFactory.getExpiryTimeout()).isEqualTo(4096);
				});
	}

	@Test
	public void pooledConnectionFactoryConfiguration() throws JMSException {
		this.context.withUserConfiguration(EmptyConfiguration.class)
				.withPropertyValues("spring.activemq.pool.enabled:true").run((loaded) -> {
					ConnectionFactory factory = loaded.getBean(ConnectionFactory.class);
					assertThat(factory).isInstanceOf(PooledConnectionFactory.class);
					loaded.getSourceApplicationContext().close();
					assertThat(factory.createConnection()).isNull();
				});
	}

	@Configuration
	static class EmptyConfiguration {

	}

	@Configuration
	static class CustomConnectionFactoryConfiguration {

		@Bean
		public ConnectionFactory connectionFactory() {
			return mock(ConnectionFactory.class);
		}

	}

}
