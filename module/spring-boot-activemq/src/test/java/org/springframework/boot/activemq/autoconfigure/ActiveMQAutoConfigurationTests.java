/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.activemq.autoconfigure;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQSslConnectionFactory;
import org.apache.activemq.ActiveMQXASslConnectionFactory;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.jms.XAConnectionFactoryWrapper;
import org.springframework.boot.jms.autoconfigure.JmsAutoConfiguration;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;
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
 * @author Armen Petrosyan
 */
class ActiveMQAutoConfigurationTests {

	private static final String[] SSL_BUNDLE_PROPERTIES = {
			"spring.ssl.bundle.jks.test-bundle.keystore.location=classpath:test.jks",
			"spring.ssl.bundle.jks.test-bundle.keystore.password=secret",
			"spring.ssl.bundle.jks.test-bundle.key.password=password",
			"spring.ssl.bundle.jks.test-bundle.truststore.location=classpath:test.jks",
			"spring.ssl.bundle.jks.test-bundle.truststore.password=secret" };

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ActiveMQAutoConfiguration.class, JmsAutoConfiguration.class,
				SslAutoConfiguration.class));

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

	@Test
	@WithPackageResources("test.jks")
	void sslBundleIsAppliedToConnectionFactory() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("spring.activemq.broker-url=ssl://localhost:61617", "spring.activemq.user=springuser",
					"spring.activemq.password=spring", "spring.activemq.ssl.bundle=test-bundle")
			.withPropertyValues(SSL_BUNDLE_PROPERTIES)
			.run((context) -> {
				ConnectionFactory targetConnectionFactory = context.getBean(CachingConnectionFactory.class)
					.getTargetConnectionFactory();
				assertThat(targetConnectionFactory).isInstanceOfSatisfying(ActiveMQSslConnectionFactory.class,
						(connectionFactory) -> {
							assertThat(connectionFactory.getBrokerURL()).isEqualTo("ssl://localhost:61617");
							assertThat(connectionFactory.getUserName()).isEqualTo("springuser");
							assertThat(connectionFactory.getPassword()).isEqualTo("spring");
							assertThatSslBundleIsApplied(connectionFactory);
						});
			});
	}

	@Test
	@WithPackageResources("test.jks")
	void sslBundleIsAppliedToPooledConnectionFactory() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("spring.activemq.pool.enabled=true", "spring.activemq.ssl.bundle=test-bundle")
			.withPropertyValues(SSL_BUNDLE_PROPERTIES)
			.run((context) -> assertThat(context.getBean(JmsPoolConnectionFactory.class).getConnectionFactory())
				.isInstanceOfSatisfying(ActiveMQSslConnectionFactory.class, this::assertThatSslBundleIsApplied));
	}

	@Test
	@WithPackageResources("test.jks")
	void sslBundleIsAppliedToXaConnectionFactory() {
		this.contextRunner.withUserConfiguration(XAConnectionFactoryWrapperConfiguration.class)
			.withPropertyValues("spring.activemq.ssl.bundle=test-bundle")
			.withPropertyValues(SSL_BUNDLE_PROPERTIES)
			.run((context) -> {
				assertThat(context.getBean("jmsConnectionFactory"))
					.isInstanceOfSatisfying(ActiveMQXASslConnectionFactory.class, this::assertThatSslBundleIsApplied);
				assertThat(context.getBean("nonXaJmsConnectionFactory"))
					.isInstanceOfSatisfying(ActiveMQSslConnectionFactory.class, this::assertThatSslBundleIsApplied);
			});
	}

	@Test
	@WithPackageResources("test.jks")
	void sslBundleFromConnectionDetailsIsApplied() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class, SslConnectionDetailsConfiguration.class)
			.withPropertyValues(SSL_BUNDLE_PROPERTIES)
			.run((context) -> assertThat(context.getBean(CachingConnectionFactory.class).getTargetConnectionFactory())
				.isInstanceOfSatisfying(ActiveMQSslConnectionFactory.class, this::assertThatSslBundleIsApplied));
	}

	@Test
	void connectionFactoryDoesNotUseSslByDefault() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.run((context) -> assertThat(context.getBean(CachingConnectionFactory.class).getTargetConnectionFactory())
				.isExactlyInstanceOf(ActiveMQConnectionFactory.class));
	}

	@Test
	void sslEnabledWithoutBundleUsesSslConnectionFactory() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("spring.activemq.ssl.enabled=true")
			.run((context) -> assertThat(context.getBean(CachingConnectionFactory.class).getTargetConnectionFactory())
				.isInstanceOfSatisfying(ActiveMQSslConnectionFactory.class,
						(connectionFactory) -> assertThat(connectionFactory)
							.extracting("trustManager", InstanceOfAssertFactories.array(TrustManager[].class))
							.isNotEmpty()));
	}

	@Test
	@WithPackageResources("test.jks")
	void sslDisabledWithBundleUsesNonSslConnectionFactory() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("spring.activemq.ssl.enabled=false", "spring.activemq.ssl.bundle=test-bundle")
			.withPropertyValues(SSL_BUNDLE_PROPERTIES)
			.run((context) -> assertThat(context.getBean(CachingConnectionFactory.class).getTargetConnectionFactory())
				.isExactlyInstanceOf(ActiveMQConnectionFactory.class));
	}

	@Test
	void sslWithInvalidBundleFails() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("spring.activemq.ssl.bundle=invalid")
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).rootCause()
					.isInstanceOf(NoSuchSslBundleException.class)
					.hasMessageContaining("invalid");
			});
	}

	@Test
	@WithPackageResources("test.jks")
	void sslBundleWithOptionsFails() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("spring.activemq.ssl.bundle=test-bundle",
					"spring.ssl.bundle.jks.test-bundle.options.ciphers=TLS_AES_128_GCM_SHA256")
			.withPropertyValues(SSL_BUNDLE_PROPERTIES)
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).rootCause()
					.hasMessage("SSL Options cannot be specified with ActiveMQ");
			});
	}

	private void assertThatSslBundleIsApplied(ActiveMQSslConnectionFactory connectionFactory) {
		assertThat(connectionFactory).extracting("keyManager", InstanceOfAssertFactories.array(KeyManager[].class))
			.singleElement()
			.isInstanceOfSatisfying(X509KeyManager.class,
					(keyManager) -> assertThat(keyManager.getPrivateKey("test-alias")).isNotNull());
		assertThat(connectionFactory).extracting("trustManager", InstanceOfAssertFactories.array(TrustManager[].class))
			.singleElement()
			.isInstanceOfSatisfying(X509TrustManager.class,
					(trustManager) -> assertThat(trustManager.getAcceptedIssuers()).isNotEmpty()
						.allSatisfy((certificate) -> assertThat(certificate.getSubjectX500Principal().getName())
							.startsWith("CN=localhost")));
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
	static class XAConnectionFactoryWrapperConfiguration {

		@Bean
		XAConnectionFactoryWrapper xaConnectionFactoryWrapper() {
			return (connectionFactory) -> (ConnectionFactory) connectionFactory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SslConnectionDetailsConfiguration {

		@Bean
		ActiveMQConnectionDetails activemqConnectionDetails(SslBundles sslBundles) {
			return new ActiveMQConnectionDetails() {

				@Override
				public String getBrokerUrl() {
					return "ssl://localhost:61617";
				}

				@Override
				public String getUser() {
					return "springuser";
				}

				@Override
				public String getPassword() {
					return "spring";
				}

				@Override
				public SslBundle getSslBundle() {
					return sslBundles.getBundle("test-bundle");
				}

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
