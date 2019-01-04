/*
 * Copyright 2012-2018 the original author or authors.
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
import org.junit.Test;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

/**
 * Tests for {@link ActiveMQAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Aurélien Leboulanger
 * @author Stephane Nicoll
 */
public class ActiveMQAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ActiveMQAutoConfiguration.class,
					JmsAutoConfiguration.class));

	@Test
	public void brokerIsEmbeddedByDefault() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(CachingConnectionFactory.class);
					CachingConnectionFactory cachingConnectionFactory = context
							.getBean(CachingConnectionFactory.class);
					assertThat(cachingConnectionFactory.getTargetConnectionFactory())
							.isInstanceOf(ActiveMQConnectionFactory.class);
					assertThat(((ActiveMQConnectionFactory) cachingConnectionFactory
							.getTargetConnectionFactory()).getBrokerURL())
									.isEqualTo("vm://localhost?broker.persistent=false");
				});
	}

	@Test
	public void configurationBacksOffWhenCustomConnectionFactoryExists() {
		this.contextRunner
				.withUserConfiguration(CustomConnectionFactoryConfiguration.class)
				.run((context) -> assertThat(
						mockingDetails(context.getBean(ConnectionFactory.class)).isMock())
								.isTrue());
	}

	@Test
	public void connectionFactoryIsCachedByDefault() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(ConnectionFactory.class);
					assertThat(context).hasSingleBean(CachingConnectionFactory.class);
					CachingConnectionFactory connectionFactory = context
							.getBean(CachingConnectionFactory.class);
					assertThat(connectionFactory.getTargetConnectionFactory())
							.isInstanceOf(ActiveMQConnectionFactory.class);
					assertThat(connectionFactory.isCacheConsumers()).isFalse();
					assertThat(connectionFactory.isCacheProducers()).isTrue();
					assertThat(connectionFactory.getSessionCacheSize()).isEqualTo(1);
				});
	}

	@Test
	public void connectionFactoryCachingCanBeCustomized() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
				.withPropertyValues("spring.jms.cache.consumers=true",
						"spring.jms.cache.producers=false",
						"spring.jms.cache.session-cache-size=10")
				.run((context) -> {
					assertThat(context).hasSingleBean(ConnectionFactory.class);
					assertThat(context).hasSingleBean(CachingConnectionFactory.class);
					CachingConnectionFactory connectionFactory = context
							.getBean(CachingConnectionFactory.class);
					assertThat(connectionFactory.isCacheConsumers()).isTrue();
					assertThat(connectionFactory.isCacheProducers()).isFalse();
					assertThat(connectionFactory.getSessionCacheSize()).isEqualTo(10);
				});
	}

	@Test
	public void connectionFactoryCachingCanBeDisabled() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
				.withPropertyValues("spring.jms.cache.enabled=false").run((context) -> {
					assertThat(context.getBeansOfType(ActiveMQConnectionFactory.class))
							.hasSize(1);
					ActiveMQConnectionFactory connectionFactory = context
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
					assertThat(connectionFactory.getTrustedPackages())
							.containsExactly(StringUtils
									.toStringArray(defaultFactory.getTrustedPackages()));
				});
	}

	@Test
	public void customConnectionFactoryIsApplied() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
				.withPropertyValues("spring.jms.cache.enabled=false",
						"spring.activemq.brokerUrl=vm://localhost?useJmx=false&broker.persistent=false",
						"spring.activemq.user=foo", "spring.activemq.password=bar",
						"spring.activemq.closeTimeout=500",
						"spring.activemq.nonBlockingRedelivery=true",
						"spring.activemq.sendTimeout=1000",
						"spring.activemq.packages.trust-all=false",
						"spring.activemq.packages.trusted=com.example.acme")
				.run((context) -> {
					assertThat(context.getBeansOfType(ActiveMQConnectionFactory.class))
							.hasSize(1);
					ActiveMQConnectionFactory connectionFactory = context
							.getBean(ActiveMQConnectionFactory.class);
					assertThat(connectionFactory.getUserName()).isEqualTo("foo");
					assertThat(connectionFactory.getPassword()).isEqualTo("bar");
					assertThat(connectionFactory.getCloseTimeout()).isEqualTo(500);
					assertThat(connectionFactory.isNonBlockingRedelivery()).isTrue();
					assertThat(connectionFactory.getSendTimeout()).isEqualTo(1000);
					assertThat(connectionFactory.isTrustAllPackages()).isFalse();
					assertThat(connectionFactory.getTrustedPackages())
							.containsExactly("com.example.acme");
				});
	}

	@Test
	public void defaultPoolConnectionFactoryIsApplied() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
				.withPropertyValues("spring.activemq.pool.enabled=true")
				.run((context) -> {
					assertThat(context.getBeansOfType(JmsPoolConnectionFactory.class))
							.hasSize(1);
					JmsPoolConnectionFactory connectionFactory = context
							.getBean(JmsPoolConnectionFactory.class);
					JmsPoolConnectionFactory defaultFactory = new JmsPoolConnectionFactory();
					assertThat(connectionFactory.isBlockIfSessionPoolIsFull())
							.isEqualTo(defaultFactory.isBlockIfSessionPoolIsFull());
					assertThat(connectionFactory.getBlockIfSessionPoolIsFullTimeout())
							.isEqualTo(
									defaultFactory.getBlockIfSessionPoolIsFullTimeout());
					assertThat(connectionFactory.getConnectionIdleTimeout())
							.isEqualTo(defaultFactory.getConnectionIdleTimeout());
					assertThat(connectionFactory.getMaxConnections())
							.isEqualTo(defaultFactory.getMaxConnections());
					assertThat(connectionFactory.getMaxSessionsPerConnection())
							.isEqualTo(defaultFactory.getMaxSessionsPerConnection());
					assertThat(connectionFactory.getConnectionCheckInterval())
							.isEqualTo(defaultFactory.getConnectionCheckInterval());
					assertThat(connectionFactory.isUseAnonymousProducers())
							.isEqualTo(defaultFactory.isUseAnonymousProducers());
				});
	}

	@Test
	public void customPoolConnectionFactoryIsApplied() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
				.withPropertyValues("spring.activemq.pool.enabled=true",
						"spring.activemq.pool.blockIfFull=false",
						"spring.activemq.pool.blockIfFullTimeout=64",
						"spring.activemq.pool.idleTimeout=512",
						"spring.activemq.pool.maxConnections=256",
						"spring.activemq.pool.maxSessionsPerConnection=1024",
						"spring.activemq.pool.timeBetweenExpirationCheck=2048",
						"spring.activemq.pool.useAnonymousProducers=false")
				.run((context) -> {
					assertThat(context.getBeansOfType(JmsPoolConnectionFactory.class))
							.hasSize(1);
					JmsPoolConnectionFactory connectionFactory = context
							.getBean(JmsPoolConnectionFactory.class);
					assertThat(connectionFactory.isBlockIfSessionPoolIsFull()).isFalse();
					assertThat(connectionFactory.getBlockIfSessionPoolIsFullTimeout())
							.isEqualTo(64);
					assertThat(connectionFactory.getConnectionIdleTimeout())
							.isEqualTo(512);
					assertThat(connectionFactory.getMaxConnections()).isEqualTo(256);
					assertThat(connectionFactory.getMaxSessionsPerConnection())
							.isEqualTo(1024);
					assertThat(connectionFactory.getConnectionCheckInterval())
							.isEqualTo(2048);
					assertThat(connectionFactory.isUseAnonymousProducers()).isFalse();
				});
	}

	@Test
	@Deprecated
	public void customPoolConnectionFactoryIsAppliedWithDeprecatedSettings() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
				.withPropertyValues("spring.activemq.pool.enabled=true",
						"spring.activemq.pool.maximumActiveSessionPerConnection=1024")
				.run((context) -> {
					assertThat(context.getBeansOfType(JmsPoolConnectionFactory.class))
							.hasSize(1);
					JmsPoolConnectionFactory connectionFactory = context
							.getBean(JmsPoolConnectionFactory.class);
					assertThat(connectionFactory.getMaxSessionsPerConnection())
							.isEqualTo(1024);
				});
	}

	@Test
	public void poolConnectionFactoryConfiguration() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
				.withPropertyValues("spring.activemq.pool.enabled:true")
				.run((context) -> {
					ConnectionFactory factory = context.getBean(ConnectionFactory.class);
					assertThat(factory).isInstanceOf(JmsPoolConnectionFactory.class);
					context.getSourceApplicationContext().close();
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

	@Configuration
	static class CustomizerConfiguration {

		@Bean
		public ActiveMQConnectionFactoryCustomizer activeMQConnectionFactoryCustomizer() {
			return (factory) -> {
				factory.setBrokerURL(
						"vm://localhost?useJmx=false&broker.persistent=false");
				factory.setUserName("foobar");
			};
		}

	}

}
