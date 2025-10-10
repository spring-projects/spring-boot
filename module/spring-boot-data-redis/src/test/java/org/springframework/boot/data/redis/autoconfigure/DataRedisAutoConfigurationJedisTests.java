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

package org.springframework.boot.data.redis.autoconfigure;

import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.assertj.SimpleAsyncTaskExecutorAssert;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration.JedisClientConfigurationBuilder;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataRedisAutoConfiguration} when Lettuce is not on the classpath.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @author Weix Sun
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
@ClassPathExclusions("lettuce-core-*.jar")
class DataRedisAutoConfigurationJedisTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration.class, SslAutoConfiguration.class));

	@Test
	void connectionFactoryDefaultsToJedis() {
		this.contextRunner.run((context) -> assertThat(context.getBean("redisConnectionFactory"))
			.isInstanceOf(JedisConnectionFactory.class));
	}

	@Test
	void connectionFactoryIsNotCreatedWhenLettuceIsSelected() {
		this.contextRunner.withPropertyValues("spring.data.redis.client-type=lettuce")
			.run((context) -> assertThat(context).doesNotHaveBean(RedisConnectionFactory.class));
	}

	@Test
	void testOverrideRedisConfiguration() {
		this.contextRunner.withPropertyValues("spring.data.redis.host:foo", "spring.data.redis.database:1")
			.run((context) -> {
				JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("foo");
				assertThat(cf.getDatabase()).isOne();
				assertThat(getUserName(cf)).isNull();
				assertThat(cf.getPassword()).isNull();
				assertThat(cf.isUseSsl()).isFalse();
			});
	}

	@Test
	void testCustomizeRedisConfiguration() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
			assertThat(cf.isUseSsl()).isTrue();
		});
	}

	@Test
	void usesConnectionDetailsIfAvailable() {
		this.contextRunner.withUserConfiguration(ConnectionDetailsConfiguration.class).run((context) -> {
			JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
			assertThat(cf.isUseSsl()).isFalse();
		});
	}

	@Test
	void testRedisUrlConfiguration() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.host:foo", "spring.data.redis.url:redis://user:password@example:33")
			.run((context) -> {
				JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("example");
				assertThat(cf.getPort()).isEqualTo(33);
				assertThat(getUserName(cf)).isEqualTo("user");
				assertThat(cf.getPassword()).isEqualTo("password");
				assertThat(cf.isUseSsl()).isFalse();
			});
	}

	@Test
	void testOverrideUrlRedisConfiguration() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.host:foo", "spring.data.redis.password:xyz",
					"spring.data.redis.port:1000", "spring.data.redis.ssl.enabled:false",
					"spring.data.redis.url:rediss://user:password@example:33")
			.run((context) -> {
				JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("example");
				assertThat(cf.getPort()).isEqualTo(33);
				assertThat(getUserName(cf)).isEqualTo("user");
				assertThat(cf.getPassword()).isEqualTo("password");
				assertThat(cf.isUseSsl()).isTrue();
			});
	}

	@Test
	void testPasswordInUrlWithColon() {
		this.contextRunner.withPropertyValues("spring.data.redis.url:redis://:pass:word@example:33").run((context) -> {
			JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
			assertThat(cf.getHostName()).isEqualTo("example");
			assertThat(cf.getPort()).isEqualTo(33);
			assertThat(getUserName(cf)).isEmpty();
			assertThat(cf.getPassword()).isEqualTo("pass:word");
		});
	}

	@Test
	void testPasswordInUrlStartsWithColon() {
		this.contextRunner.withPropertyValues("spring.data.redis.url:redis://user::pass:word@example:33")
			.run((context) -> {
				JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("example");
				assertThat(cf.getPort()).isEqualTo(33);
				assertThat(getUserName(cf)).isEqualTo("user");
				assertThat(cf.getPassword()).isEqualTo(":pass:word");
			});
	}

	@Test
	void testRedisConfigurationWithPool() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.host:foo", "spring.data.redis.jedis.pool.min-idle:1",
					"spring.data.redis.jedis.pool.max-idle:4", "spring.data.redis.jedis.pool.max-active:16",
					"spring.data.redis.jedis.pool.max-wait:2000",
					"spring.data.redis.jedis.pool.time-between-eviction-runs:30000")
			.withUserConfiguration(JedisDisableStartupConfiguration.class)
			.run((context) -> {
				JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("foo");
				assertThat(cf.getPoolConfig()).satisfies((poolConfig) -> {
					assertThat(poolConfig.getMinIdle()).isOne();
					assertThat(poolConfig.getMaxIdle()).isEqualTo(4);
					assertThat(poolConfig.getMaxTotal()).isEqualTo(16);
					assertThat(poolConfig.getMaxWaitDuration()).isEqualTo(Duration.ofSeconds(2));
					assertThat(poolConfig.getDurationBetweenEvictionRuns()).isEqualTo(Duration.ofSeconds(30));
				});
			});
	}

	@Test
	void testRedisConfigurationDisabledPool() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.host:foo", "spring.data.redis.jedis.pool.enabled:false")
			.run((context) -> {
				JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("foo");
				assertThat(cf.getClientConfiguration().isUsePooling()).isFalse();
			});
	}

	@Test
	void testRedisConfigurationWithTimeoutAndConnectTimeout() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.host:foo", "spring.data.redis.timeout:250",
					"spring.data.redis.connect-timeout:1000")
			.run((context) -> {
				JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("foo");
				assertThat(cf.getTimeout()).isEqualTo(250);
				assertThat(cf.getClientConfiguration().getConnectTimeout().toMillis()).isEqualTo(1000);
			});
	}

	@Test
	void testRedisConfigurationWithDefaultTimeouts() {
		this.contextRunner.withPropertyValues("spring.data.redis.host:foo").run((context) -> {
			JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
			assertThat(cf.getHostName()).isEqualTo("foo");
			assertThat(cf.getTimeout()).isEqualTo(2000);
			assertThat(cf.getClientConfiguration().getConnectTimeout().toMillis()).isEqualTo(2000);
		});
	}

	@Test
	void testRedisConfigurationWithClientName() {
		this.contextRunner.withPropertyValues("spring.data.redis.host:foo", "spring.data.redis.client-name:spring-boot")
			.run((context) -> {
				JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("foo");
				assertThat(cf.getClientName()).isEqualTo("spring-boot");
			});
	}

	@Test
	void testRedisConfigurationWithSentinel() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.sentinel.master:mymaster",
					"spring.data.redis.sentinel.nodes:127.0.0.1:26379,127.0.0.1:26380")
			.withUserConfiguration(JedisConnectionFactoryCaptorConfiguration.class)
			.run((context) -> {
				JedisConnectionFactory connectionFactory = JedisConnectionFactoryCaptor.connectionFactory;
				assertThat(connectionFactory).isNotNull();
				assertThat(connectionFactory.isRedisSentinelAware()).isTrue();
			});
	}

	@Test
	void testRedisConfigurationWithSentinelAndAuthentication() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.username=user", "spring.data.redis.password=password",
					"spring.data.redis.sentinel.master:mymaster",
					"spring.data.redis.sentinel.nodes:127.0.0.1:26379,127.0.0.1:26380")
			.withUserConfiguration(JedisConnectionFactoryCaptorConfiguration.class)
			.run((context) -> {
				JedisConnectionFactory connectionFactory = JedisConnectionFactoryCaptor.connectionFactory;
				assertThat(connectionFactory).isNotNull();
				assertThat(connectionFactory.isRedisSentinelAware()).isTrue();
				assertThat(getUserName(connectionFactory)).isEqualTo("user");
				assertThat(connectionFactory.getPassword()).isEqualTo("password");
			});
	}

	@Test
	void testRedisConfigurationWithCluster() {
		this.contextRunner.withPropertyValues("spring.data.redis.cluster.nodes=127.0.0.1:27379,127.0.0.1:27380")
			.withUserConfiguration(JedisConnectionFactoryCaptorConfiguration.class)
			.run((context) -> {
				JedisConnectionFactory connectionFactory = JedisConnectionFactoryCaptor.connectionFactory;
				assertThat(connectionFactory).isNotNull();
				assertThat(connectionFactory.isRedisClusterAware()).isTrue();
			});
	}

	@Test
	void testRedisConfigurationWitMasterReplica() {
		this.contextRunner.withPropertyValues("spring.data.redis.masterreplica.nodes=127.0.0.1:27379,127.0.0.1:27380")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.hasMessageContaining("'masterReplicaConfig' is not supported by Jedis"));
	}

	@Test
	void testRedisConfigurationWithSslEnabled() {
		this.contextRunner.withPropertyValues("spring.data.redis.ssl.enabled:true").run((context) -> {
			JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
			assertThat(cf.isUseSsl()).isTrue();
		});
	}

	@Test
	@WithPackageResources("test.jks")
	void testRedisConfigurationWithSslBundle() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.ssl.bundle:test-bundle",
					"spring.ssl.bundle.jks.test-bundle.keystore.location:classpath:test.jks",
					"spring.ssl.bundle.jks.test-bundle.keystore.password:secret",
					"spring.ssl.bundle.jks.test-bundle.key.password:password")
			.run((context) -> {
				JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
				assertThat(cf.isUseSsl()).isTrue();
			});
	}

	@Test
	void testRedisConfigurationWithSslDisabledAndBundle() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.ssl.enabled:false", "spring.data.redis.ssl.bundle:test-bundle")
			.run((context) -> {
				JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
				assertThat(cf.isUseSsl()).isFalse();
			});
	}

	@Test
	void shouldUsePlatformThreadsByDefault() {
		this.contextRunner.run((context) -> {
			JedisConnectionFactory factory = context.getBean(JedisConnectionFactory.class);
			assertThat(factory).extracting("executor").isNull();
		});
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void shouldUseVirtualThreadsIfEnabled() {
		this.contextRunner.withPropertyValues("spring.threads.virtual.enabled=true").run((context) -> {
			JedisConnectionFactory factory = context.getBean(JedisConnectionFactory.class);
			assertThat(factory).extracting("executor")
				.satisfies((executor) -> SimpleAsyncTaskExecutorAssert.assertThat((SimpleAsyncTaskExecutor) executor)
					.usesVirtualThreads());
		});
	}

	private @Nullable String getUserName(JedisConnectionFactory factory) {
		return ReflectionTestUtils.invokeMethod(factory, "getRedisUsername");
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConfiguration {

		@Bean
		JedisClientConfigurationBuilderCustomizer customizer() {
			return JedisClientConfigurationBuilder::useSsl;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConnectionDetailsConfiguration {

		@Bean
		DataRedisConnectionDetails redisConnectionDetails() {
			return new DataRedisConnectionDetails() {

				@Override
				public Standalone getStandalone() {
					return new Standalone() {

						@Override
						public String getHost() {
							return "localhost";
						}

						@Override
						public int getPort() {
							return 6379;
						}

					};
				}

			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JedisConnectionFactoryCaptorConfiguration {

		@Bean
		static JedisConnectionFactoryCaptor jedisConnectionFactoryCaptor() {
			return new JedisConnectionFactoryCaptor();
		}

	}

	static class JedisConnectionFactoryCaptor implements BeanPostProcessor {

		static @Nullable JedisConnectionFactory connectionFactory;

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			if (bean instanceof JedisConnectionFactory jedisConnectionFactory) {
				connectionFactory = jedisConnectionFactory;
			}
			return bean;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JedisDisableStartupConfiguration {

		@Bean
		static BeanPostProcessor jedisDisableStartup() {
			return new BeanPostProcessor() {

				@Override
				public Object postProcessBeforeInitialization(Object bean, String beanName) {
					if (bean instanceof JedisConnectionFactory jedisConnectionFactory) {
						jedisConnectionFactory.setEarlyStartup(false);
						jedisConnectionFactory.setAutoStartup(false);
					}
					return bean;
				}

			};
		}

	}

}
