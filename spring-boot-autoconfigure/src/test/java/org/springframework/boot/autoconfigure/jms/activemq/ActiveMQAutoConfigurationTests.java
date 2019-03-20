/*
 * Copyright 2012-2017 the original author or authors.
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

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.junit.Test;

import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
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

	private AnnotationConfigApplicationContext context;

	@Test
	public void brokerIsEmbeddedByDefault() {
		load(EmptyConfiguration.class);
		ConnectionFactory connectionFactory = this.context
				.getBean(ConnectionFactory.class);
		assertThat(connectionFactory).isInstanceOf(ActiveMQConnectionFactory.class);
		String brokerUrl = ((ActiveMQConnectionFactory) connectionFactory).getBrokerURL();
		assertThat(brokerUrl).isEqualTo("vm://localhost?broker.persistent=false");
	}

	@Test
	public void configurationBacksOffWhenCustomConnectionFactoryExists() {
		load(CustomConnectionFactoryConfiguration.class);
		assertThat(mockingDetails(this.context.getBean(ConnectionFactory.class)).isMock())
				.isTrue();
	}

	@Test
	public void defaultsConnectionFactoryAreApplied() {
		load(EmptyConfiguration.class, "spring.activemq.pool.enabled=false");
		assertThat(this.context.getBeansOfType(ActiveMQConnectionFactory.class))
				.hasSize(1);
		ActiveMQConnectionFactory connectionFactory = this.context
				.getBean(ActiveMQConnectionFactory.class);
		ActiveMQConnectionFactory defaultFactory = new ActiveMQConnectionFactory(
				"vm://localhost?broker.persistent=false");
		assertThat(connectionFactory.getUserName())
				.isEqualTo(defaultFactory.getUserName());
		assertThat(connectionFactory.getPassword())
				.isEqualTo(defaultFactory.getPassword());
		assertThat(connectionFactory.getCloseTimeout())
				.isEqualTo(defaultFactory.getCloseTimeout());
		assertThat(connectionFactory.isNonBlockingRedelivery())
				.isEqualTo(defaultFactory.isNonBlockingRedelivery());
		assertThat(connectionFactory.getSendTimeout())
				.isEqualTo(defaultFactory.getSendTimeout());
		assertThat(connectionFactory.isTrustAllPackages())
				.isEqualTo(defaultFactory.isTrustAllPackages());
		assertThat(connectionFactory.getTrustedPackages()).containsExactly(
				defaultFactory.getTrustedPackages().toArray(new String[] {}));
	}

	@Test
	public void customConnectionFactoryAreApplied() {
		load(EmptyConfiguration.class, "spring.activemq.pool.enabled=false",
				"spring.activemq.brokerUrl=vm://localhost?useJmx=false&broker.persistent=false",
				"spring.activemq.user=foo", "spring.activemq.password=bar",
				"spring.activemq.closeTimeout=500",
				"spring.activemq.nonBlockingRedelivery=true",
				"spring.activemq.sendTimeout=1000",
				"spring.activemq.packages.trust-all=false",
				"spring.activemq.packages.trusted=com.example.acme");
		assertThat(this.context.getBeansOfType(ActiveMQConnectionFactory.class))
				.hasSize(1);
		ActiveMQConnectionFactory connectionFactory = this.context
				.getBean(ActiveMQConnectionFactory.class);
		assertThat(connectionFactory.getUserName()).isEqualTo("foo");
		assertThat(connectionFactory.getPassword()).isEqualTo("bar");
		assertThat(connectionFactory.getCloseTimeout()).isEqualTo(500);
		assertThat(connectionFactory.isNonBlockingRedelivery()).isEqualTo(true);
		assertThat(connectionFactory.getSendTimeout()).isEqualTo(1000);
		assertThat(connectionFactory.isTrustAllPackages()).isFalse();
		assertThat(connectionFactory.getTrustedPackages())
				.containsExactly("com.example.acme");
	}

	@Test
	public void defaultsPooledConnectionFactoryAreApplied() {
		load(EmptyConfiguration.class, "spring.activemq.pool.enabled=true");
		assertThat(this.context.getBeansOfType(PooledConnectionFactory.class)).hasSize(1);
		PooledConnectionFactory connectionFactory = this.context
				.getBean(PooledConnectionFactory.class);
		PooledConnectionFactory defaultFactory = new PooledConnectionFactory();
		assertThat(connectionFactory.isBlockIfSessionPoolIsFull())
				.isEqualTo(defaultFactory.isBlockIfSessionPoolIsFull());
		assertThat(connectionFactory.getBlockIfSessionPoolIsFullTimeout())
				.isEqualTo(defaultFactory.getBlockIfSessionPoolIsFullTimeout());
		assertThat(connectionFactory.isCreateConnectionOnStartup())
				.isEqualTo(defaultFactory.isCreateConnectionOnStartup());
		assertThat(connectionFactory.getExpiryTimeout())
				.isEqualTo(defaultFactory.getExpiryTimeout());
		assertThat(connectionFactory.getIdleTimeout())
				.isEqualTo(defaultFactory.getIdleTimeout());
		assertThat(connectionFactory.getMaxConnections())
				.isEqualTo(defaultFactory.getMaxConnections());
		assertThat(connectionFactory.getMaximumActiveSessionPerConnection())
				.isEqualTo(defaultFactory.getMaximumActiveSessionPerConnection());
		assertThat(connectionFactory.isReconnectOnException())
				.isEqualTo(defaultFactory.isReconnectOnException());
		assertThat(connectionFactory.getTimeBetweenExpirationCheckMillis())
				.isEqualTo(defaultFactory.getTimeBetweenExpirationCheckMillis());
		assertThat(connectionFactory.isUseAnonymousProducers())
				.isEqualTo(defaultFactory.isUseAnonymousProducers());
	}

	@Test
	public void customPooledConnectionFactoryAreApplied() {
		load(EmptyConfiguration.class, "spring.activemq.pool.enabled=true",
				"spring.activemq.pool.blockIfFull=false",
				"spring.activemq.pool.blockIfFullTimeout=64",
				"spring.activemq.pool.createConnectionOnStartup=false",
				"spring.activemq.pool.expiryTimeout=4096",
				"spring.activemq.pool.idleTimeout=512",
				"spring.activemq.pool.maxConnections=256",
				"spring.activemq.pool.maximumActiveSessionPerConnection=1024",
				"spring.activemq.pool.reconnectOnException=false",
				"spring.activemq.pool.timeBetweenExpirationCheck=2048",
				"spring.activemq.pool.useAnonymousProducers=false");
		assertThat(this.context.getBeansOfType(PooledConnectionFactory.class)).hasSize(1);
		PooledConnectionFactory connectionFactory = this.context
				.getBean(PooledConnectionFactory.class);
		assertThat(connectionFactory.isBlockIfSessionPoolIsFull()).isEqualTo(false);
		assertThat(connectionFactory.getBlockIfSessionPoolIsFullTimeout()).isEqualTo(64);
		assertThat(connectionFactory.isCreateConnectionOnStartup()).isEqualTo(false);
		assertThat(connectionFactory.getExpiryTimeout()).isEqualTo(4096);
		assertThat(connectionFactory.getIdleTimeout()).isEqualTo(512);
		assertThat(connectionFactory.getMaxConnections()).isEqualTo(256);
		assertThat(connectionFactory.getMaximumActiveSessionPerConnection())
				.isEqualTo(1024);
		assertThat(connectionFactory.isReconnectOnException()).isEqualTo(false);
		assertThat(connectionFactory.getTimeBetweenExpirationCheckMillis())
				.isEqualTo(2048);
		assertThat(connectionFactory.isUseAnonymousProducers()).isEqualTo(false);
	}

	@Test
	@Deprecated
	public void customPooledConnectionFactoryOnTargetInstanceAreApplied() {
		load(EmptyConfiguration.class, "spring.activemq.pool.enabled=true",
				"spring.activemq.pool.configuration.blockIfSessionPoolIsFull=false",
				"spring.activemq.pool.configuration.blockIfSessionPoolIsFullTimeout=64",
				"spring.activemq.pool.configuration.createConnectionOnStartup=false",
				"spring.activemq.pool.expiryTimeout=4096",
				"spring.activemq.pool.idleTimeout=512",
				"spring.activemq.pool.maxConnections=256",
				"spring.activemq.pool.configuration.maximumActiveSessionPerConnection=1024",
				"spring.activemq.pool.configuration.reconnectOnException=false",
				"spring.activemq.pool.configuration.timeBetweenExpirationCheckMillis=2048",
				"spring.activemq.pool.configuration.useAnonymousProducers=false");
		assertThat(this.context.getBeansOfType(PooledConnectionFactory.class)).hasSize(1);
		PooledConnectionFactory connectionFactory = this.context
				.getBean(PooledConnectionFactory.class);
		assertThat(connectionFactory.isBlockIfSessionPoolIsFull()).isEqualTo(false);
		assertThat(connectionFactory.getBlockIfSessionPoolIsFullTimeout()).isEqualTo(64);
		assertThat(connectionFactory.isCreateConnectionOnStartup()).isEqualTo(false);
		assertThat(connectionFactory.getExpiryTimeout()).isEqualTo(4096);
		assertThat(connectionFactory.getIdleTimeout()).isEqualTo(512);
		assertThat(connectionFactory.getMaxConnections()).isEqualTo(256);
		assertThat(connectionFactory.getMaximumActiveSessionPerConnection())
				.isEqualTo(1024);
		assertThat(connectionFactory.isReconnectOnException()).isEqualTo(false);
		assertThat(connectionFactory.getTimeBetweenExpirationCheckMillis())
				.isEqualTo(2048);
		assertThat(connectionFactory.isUseAnonymousProducers()).isEqualTo(false);
	}

	@Test
	public void pooledConnectionFactoryConfiguration() throws JMSException {
		load(EmptyConfiguration.class, "spring.activemq.pool.enabled:true");
		ConnectionFactory connectionFactory = this.context
				.getBean(ConnectionFactory.class);
		assertThat(connectionFactory).isInstanceOf(PooledConnectionFactory.class);
		this.context.close();
		assertThat(connectionFactory.createConnection()).isNull();
	}

	@Test
	public void customizerOverridesAutConfig() {
		load(CustomizerConfiguration.class);
		ActiveMQConnectionFactory connectionFactory = this.context
				.getBean(ActiveMQConnectionFactory.class);
		assertThat(connectionFactory.getBrokerURL())
				.isEqualTo("vm://localhost?useJmx=false&broker.persistent=false");
		assertThat(connectionFactory.getUserName()).isEqualTo("foobar");
	}

	private void load(Class<?> config, String... environment) {
		this.context = doLoad(config, environment);
	}

	private AnnotationConfigApplicationContext doLoad(Class<?> config,
			String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(config);
		applicationContext.register(ActiveMQAutoConfiguration.class,
				JmsAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.refresh();
		return applicationContext;
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

	@Configuration
	static class CustomizerConfiguration {

		@Bean
		public ActiveMQConnectionFactoryCustomizer activeMQConnectionFactoryCustomizer() {
			return new ActiveMQConnectionFactoryCustomizer() {
				@Override
				public void customize(ActiveMQConnectionFactory factory) {
					factory.setBrokerURL(
							"vm://localhost?useJmx=false&broker.persistent=false");
					factory.setUserName("foobar");
				}
			};
		}

	}

}
