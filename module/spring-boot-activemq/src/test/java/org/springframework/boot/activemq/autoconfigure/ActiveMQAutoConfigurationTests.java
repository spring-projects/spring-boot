/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.activemq.autoconfigure;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jms.autoconfigure.JmsAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
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
 * @author Eddú Meléndez
 */
class ActiveMQAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ActiveMQAutoConfiguration.class, JmsAutoConfiguration.class));

	@Test
	void brokerIsEmbeddedByDefault() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(CachingConnectionFactory.class).hasBean("jmsConnectionFactory");
			CachingConnectionFactory connectionFactory = context.getBean(CachingConnectionFactory.class);
			assertThat(context.getBean("jmsConnectionFactory")).isSameAs(connectionFactory);
			ConnectionFactory targetConnectionFactory = connectionFactory.getTargetConnectionFactory();
			assertThat(targetConnectionFactory).isInstanceOf(ActiveMQConnectionFactory.class);
			assertThat(((ActiveMQConnectionFactory) targetConnectionFactory).getBrokerURL())
				.isEqualTo("vm://localhost?broker.persistent=false");
		});
	}

	@Test
	void configurationBacksOffWhenCustomConnectionFactoryExists() {
		this.contextRunner.withUserConfiguration(CustomConnectionFactoryConfiguration.class)
			.run((context) -> assertThat(mockingDetails(context.getBean(ConnectionFactory.class)).isMock()).isTrue());
	}

	@Test
	void connectionFactoryIsCachedByDefault() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(ConnectionFactory.class)
				.hasSingleBean(CachingConnectionFactory.class)
				.hasBean("jmsConnectionFactory");
			CachingConnectionFactory connectionFactory = context.getBean(CachingConnectionFactory.class);
			assertThat(context.getBean("jmsConnectionFactory")).isSameAs(connectionFactory);
			assertThat(connectionFactory.getTargetConnectionFactory()).isInstanceOf(ActiveMQConnectionFactory.class);
			assertThat(connectionFactory.isCacheConsumers()).isFalse();
			assertThat(connectionFactory.isCacheProducers()).isTrue();
			assertThat(connectionFactory.getSessionCacheSize()).isEqualTo(1);
		});
	}

	@Test
	void connectionFactoryCachingCanBeCustomized() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("spring.jms.cache.consumers=true", "spring.jms.cache.producers=false",
					"spring.jms.cache.session-cache-size=10")
			.run((context) -> {
				assertThat(context).hasSingleBean(ConnectionFactory.class)
					.hasSingleBean(CachingConnectionFactory.class)
					.hasBean("jmsConnectionFactory");
				CachingConnectionFactory connectionFactory = context.getBean(CachingConnectionFactory.class);
				assertThat(context.getBean("jmsConnectionFactory")).isSameAs(connectionFactory);
				assertThat(connectionFactory.isCacheConsumers()).isTrue();
				assertThat(connectionFactory.isCacheProducers()).isFalse();
				assertThat(connectionFactory.getSessionCacheSize()).isEqualTo(10);
			});
	}

	@Test
	void connectionFactoryCachingCanBeDisabled() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("spring.jms.cache.enabled=false")
			.run((context) -> {
				assertThat(context).hasSingleBean(ConnectionFactory.class)
					.hasSingleBean(ActiveMQConnectionFactory.class)
					.hasBean("jmsConnectionFactory");
				ActiveMQConnectionFactory connectionFactory = context.getBean(ActiveMQConnectionFactory.class);
				assertThat(context.getBean("jmsConnectionFactory")).isSameAs(connectionFactory);
				ActiveMQConnectionFactory defaultFactory = new ActiveMQConnectionFactory(
						"vm://localhost?broker.persistent=false");
				assertThat(connectionFactory.getUserName()).isEqualTo(defaultFactory.getUserName());
				assertThat(connectionFactory.getPassword()).isEqualTo(defaultFactory.getPassword());
				assertThat(connectionFactory.getCloseTimeout()).isEqualTo(defaultFactory.getCloseTimeout());
				assertThat(connectionFactory.isNonBlockingRedelivery())
					.isEqualTo(defaultFactory.isNonBlockingRedelivery());
				assertThat(connectionFactory.getSendTimeout()).isEqualTo(defaultFactory.getSendTimeout());
				assertThat(connectionFactory.isTrustAllPackages()).isEqualTo(defaultFactory.isTrustAllPackages());
				assertThat(connectionFactory.getTrustedPackages())
					.containsExactly(StringUtils.toStringArray(defaultFactory.getTrustedPackages()));
			});
	}

	@Test
	void customConnectionFactoryIsApplied() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("spring.jms.cache.enabled=false",
					"spring.activemq.brokerUrl=vm://localhost?useJmx=false&broker.persistent=false",
					"spring.activemq.user=foo", "spring.activemq.password=bar", "spring.activemq.closeTimeout=500",
					"spring.activemq.nonBlockingRedelivery=true", "spring.activemq.sendTimeout=1000",
					"spring.activemq.packages.trust-all=false", "spring.activemq.packages.trusted=com.example.acme")
			.run((context) -> {
				assertThat(context).hasSingleBean(ConnectionFactory.class)
					.hasSingleBean(ActiveMQConnectionFactory.class)
					.hasBean("jmsConnectionFactory");
				ActiveMQConnectionFactory connectionFactory = context.getBean(ActiveMQConnectionFactory.class);
				assertThat(context.getBean("jmsConnectionFactory")).isSameAs(connectionFactory);
				assertThat(connectionFactory.getUserName()).isEqualTo("foo");
				assertThat(connectionFactory.getPassword()).isEqualTo("bar");
				assertThat(connectionFactory.getCloseTimeout()).isEqualTo(500);
				assertThat(connectionFactory.isNonBlockingRedelivery()).isTrue();
				assertThat(connectionFactory.getSendTimeout()).isEqualTo(1000);
				assertThat(connectionFactory.isTrustAllPackages()).isFalse();
				assertThat(connectionFactory.getTrustedPackages()).containsExactly("com.example.acme");
			});
	}

	@Test
	void defaultPoolConnectionFactoryIsApplied() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("spring.activemq.pool.enabled=true")
			.run((context) -> {
				assertThat(context).hasSingleBean(ConnectionFactory.class)
					.hasSingleBean(JmsPoolConnectionFactory.class)
					.hasBean("jmsConnectionFactory");
				JmsPoolConnectionFactory connectionFactory = context.getBean(JmsPoolConnectionFactory.class);
				assertThat(context.getBean("jmsConnectionFactory")).isSameAs(connectionFactory);
				JmsPoolConnectionFactory defaultFactory = new JmsPoolConnectionFactory();
				assertThat(connectionFactory.isBlockIfSessionPoolIsFull())
					.isEqualTo(defaultFactory.isBlockIfSessionPoolIsFull());
				assertThat(connectionFactory.getBlockIfSessionPoolIsFullTimeout())
					.isEqualTo(defaultFactory.getBlockIfSessionPoolIsFullTimeout());
				assertThat(connectionFactory.getConnectionIdleTimeout())
					.isEqualTo(defaultFactory.getConnectionIdleTimeout());
				assertThat(connectionFactory.getMaxConnections()).isEqualTo(defaultFactory.getMaxConnections());
				assertThat(connectionFactory.getMaxSessionsPerConnection())
					.isEqualTo(defaultFactory.getMaxSessionsPerConnection());
				assertThat(connectionFactory.getConnectionCheckInterval())
					.isEqualTo(defaultFactory.getConnectionCheckInterval());
				assertThat(connectionFactory.isUseAnonymousProducers())
					.isEqualTo(defaultFactory.isUseAnonymousProducers());
			});
	}

	@Test
	void customPoolConnectionFactoryIsApplied() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("spring.activemq.pool.enabled=true", "spring.activemq.pool.blockIfFull=false",
					"spring.activemq.pool.blockIfFullTimeout=64", "spring.activemq.pool.idleTimeout=512",
					"spring.activemq.pool.maxConnections=256", "spring.activemq.pool.maxSessionsPerConnection=1024",
					"spring.activemq.pool.timeBetweenExpirationCheck=2048",
					"spring.activemq.pool.useAnonymousProducers=false")
			.run((context) -> {
				assertThat(context).hasSingleBean(ConnectionFactory.class)
					.hasSingleBean(JmsPoolConnectionFactory.class)
					.hasBean("jmsConnectionFactory");
				JmsPoolConnectionFactory connectionFactory = context.getBean(JmsPoolConnectionFactory.class);
				assertThat(context.getBean("jmsConnectionFactory")).isSameAs(connectionFactory);
				assertThat(connectionFactory.isBlockIfSessionPoolIsFull()).isFalse();
				assertThat(connectionFactory.getBlockIfSessionPoolIsFullTimeout()).isEqualTo(64);
				assertThat(connectionFactory.getConnectionIdleTimeout()).isEqualTo(512);
				assertThat(connectionFactory.getMaxConnections()).isEqualTo(256);
				assertThat(connectionFactory.getMaxSessionsPerConnection()).isEqualTo(1024);
				assertThat(connectionFactory.getConnectionCheckInterval()).isEqualTo(2048);
				assertThat(connectionFactory.isUseAnonymousProducers()).isFalse();
			});
	}

	@Test
	void poolConnectionFactoryConfiguration() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("spring.activemq.pool.enabled:true")
			.run((context) -> {
				assertThat(context).hasSingleBean(ConnectionFactory.class)
					.hasSingleBean(JmsPoolConnectionFactory.class)
					.hasBean("jmsConnectionFactory");
				ConnectionFactory factory = context.getBean(ConnectionFactory.class);
				assertThat(context.getBean("jmsConnectionFactory")).isSameAs(factory);
				assertThat(factory).isInstanceOf(JmsPoolConnectionFactory.class);
				context.getSourceApplicationContext().close();
				assertThat(factory.createConnection()).isNull();
			});
	}

	@Test
	void cachingConnectionFactoryNotOnTheClasspathThenSimpleConnectionFactoryAutoConfigured() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(CachingConnectionFactory.class))
			.withPropertyValues("spring.activemq.pool.enabled=false", "spring.jms.cache.enabled=false")
			.run((context) -> {
				assertThat(context).hasSingleBean(ConnectionFactory.class)
					.hasSingleBean(ActiveMQConnectionFactory.class)
					.hasBean("jmsConnectionFactory");
				ActiveMQConnectionFactory connectionFactory = context.getBean(ActiveMQConnectionFactory.class);
				assertThat(context.getBean("jmsConnectionFactory")).isSameAs(connectionFactory);
			});
	}

	@Test
	void cachingConnectionFactoryNotOnTheClasspathAndCacheEnabledThenSimpleConnectionFactoryNotConfigured() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(CachingConnectionFactory.class))
			.withPropertyValues("spring.activemq.pool.enabled=false", "spring.jms.cache.enabled=true")
			.run((context) -> assertThat(context).doesNotHaveBean(ConnectionFactory.class)
				.doesNotHaveBean(ActiveMQConnectionFactory.class)
				.doesNotHaveBean("jmsConnectionFactory"));
	}

	@Test
	void definesPropertiesBasedConnectionDetailsByDefault() {
		this.contextRunner.run((context) -> assertThat(context)
			.hasSingleBean(ActiveMQAutoConfiguration.PropertiesActiveMQConnectionDetails.class));
	}

	@Test
	void testConnectionFactoryWithOverridesWhenUsingCustomConnectionDetails() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(CachingConnectionFactory.class))
			.withPropertyValues("spring.activemq.pool.enabled=false", "spring.jms.cache.enabled=false")
			.withUserConfiguration(TestConnectionDetailsConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(ActiveMQConnectionDetails.class)
					.doesNotHaveBean(ActiveMQAutoConfiguration.PropertiesActiveMQConnectionDetails.class);
				ActiveMQConnectionFactory connectionFactory = context.getBean(ActiveMQConnectionFactory.class);
				assertThat(connectionFactory.getBrokerURL()).isEqualTo("tcp://localhost:12345");
				assertThat(connectionFactory.getUserName()).isEqualTo("springuser");
				assertThat(connectionFactory.getPassword()).isEqualTo("spring");
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class EmptyConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConnectionFactoryConfiguration {

		@Bean
		ConnectionFactory connectionFactory() {
			return mock(ConnectionFactory.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomizerConfiguration {

		@Bean
		ActiveMQConnectionFactoryCustomizer activeMQConnectionFactoryCustomizer() {
			return (factory) -> {
				factory.setBrokerURL("vm://localhost?useJmx=false&broker.persistent=false");
				factory.setUserName("foobar");
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConnectionDetailsConfiguration {

		@Bean
		ActiveMQConnectionDetails activemqConnectionDetails() {
			return new ActiveMQConnectionDetails() {

				@Override
				public String getBrokerUrl() {
					return "tcp://localhost:12345";
				}

				@Override
				public String getUser() {
					return "springuser";
				}

				@Override
				public String getPassword() {
					return "spring";
				}

			};
		}

	}

}
