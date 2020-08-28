/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.redis;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration.JedisClientConfigurationBuilder;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RedisAutoConfiguration} when Lettuce is not on the classpath.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
class RedisAutoConfigurationJedisTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.withClassLoader(new FilteredClassLoader(RedisClient.class));

	@Test
	void connectionFactoryDefaultsToJedis() {
		this.contextRunner.run((context) -> assertThat(context.getBean("redisConnectionFactory"))
				.isInstanceOf(JedisConnectionFactory.class));
	}

	@Test
	void connectionFactoryIsNotCreatedWhenLettuceIsSelected() {
		this.contextRunner.withPropertyValues("spring.redis.client-type=lettuce")
				.run((context) -> assertThat(context).doesNotHaveBean(RedisConnectionFactory.class));
	}

	@Test
	void testOverrideRedisConfiguration() {
		this.contextRunner.withPropertyValues("spring.redis.host:foo", "spring.redis.database:1").run((context) -> {
			JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
			assertThat(cf.getHostName()).isEqualTo("foo");
			assertThat(cf.getDatabase()).isEqualTo(1);
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
	void testRedisUrlConfiguration() {
		this.contextRunner
				.withPropertyValues("spring.redis.host:foo", "spring.redis.url:redis://user:password@example:33")
				.run((context) -> {
					JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("example");
					assertThat(cf.getPort()).isEqualTo(33);
					assertThat(cf.getPassword()).isEqualTo("password");
					assertThat(cf.isUseSsl()).isFalse();
				});
	}

	@Test
	void testOverrideUrlRedisConfiguration() {
		this.contextRunner
				.withPropertyValues("spring.redis.host:foo", "spring.redis.password:xyz", "spring.redis.port:1000",
						"spring.redis.ssl:false", "spring.redis.url:rediss://user:password@example:33")
				.run((context) -> {
					JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("example");
					assertThat(cf.getPort()).isEqualTo(33);
					assertThat(cf.getPassword()).isEqualTo("password");
					assertThat(cf.isUseSsl()).isTrue();
				});
	}

	@Test
	void testPasswordInUrlWithColon() {
		this.contextRunner.withPropertyValues("spring.redis.url:redis://:pass:word@example:33").run((context) -> {
			assertThat(context.getBean(JedisConnectionFactory.class).getHostName()).isEqualTo("example");
			assertThat(context.getBean(JedisConnectionFactory.class).getPort()).isEqualTo(33);
			assertThat(context.getBean(JedisConnectionFactory.class).getPassword()).isEqualTo("pass:word");
		});
	}

	@Test
	void testPasswordInUrlStartsWithColon() {
		this.contextRunner.withPropertyValues("spring.redis.url:redis://user::pass:word@example:33").run((context) -> {
			assertThat(context.getBean(JedisConnectionFactory.class).getHostName()).isEqualTo("example");
			assertThat(context.getBean(JedisConnectionFactory.class).getPort()).isEqualTo(33);
			assertThat(context.getBean(JedisConnectionFactory.class).getPassword()).isEqualTo(":pass:word");
		});
	}

	@Test
	void testRedisConfigurationWithPool() {
		this.contextRunner.withPropertyValues("spring.redis.host:foo", "spring.redis.jedis.pool.min-idle:1",
				"spring.redis.jedis.pool.max-idle:4", "spring.redis.jedis.pool.max-active:16",
				"spring.redis.jedis.pool.max-wait:2000", "spring.redis.jedis.pool.time-between-eviction-runs:30000")
				.run((context) -> {
					JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("foo");
					assertThat(cf.getPoolConfig().getMinIdle()).isEqualTo(1);
					assertThat(cf.getPoolConfig().getMaxIdle()).isEqualTo(4);
					assertThat(cf.getPoolConfig().getMaxTotal()).isEqualTo(16);
					assertThat(cf.getPoolConfig().getMaxWaitMillis()).isEqualTo(2000);
					assertThat(cf.getPoolConfig().getTimeBetweenEvictionRunsMillis()).isEqualTo(30000);
				});
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("timeoutArguments")
	void testRedisConfigurationWithTimeout(List<String> properties, long readTimeout, long connectionTimeout) {
		this.contextRunner.withPropertyValues("spring.redis.host:foo")
				.withPropertyValues(properties.toArray(new String[0])).run((context) -> {
					JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("foo");
					assertThat(cf.getTimeout()).isEqualTo(readTimeout);
					assertThat(cf.getClientConfiguration().getConnectTimeout().toMillis()).isEqualTo(connectionTimeout);
				});
	}

	static Stream<Arguments> timeoutArguments() {
		return Stream.of(Arguments.arguments(Collections.emptyList(), 2000, 2000),
				Arguments.arguments(Collections.singletonList("spring.redis.timeout:100"), 100, 100),
				Arguments.arguments(Collections.singletonList("spring.redis.read-timeout:100"), 100, 2000),
				Arguments.arguments(Collections.singletonList("spring.redis.connection-timeout:100"), 2000, 100),
				Arguments.arguments(Arrays.asList("spring.redis.timeout:100", "spring.redis.read-timeout:500"), 500,
						100),
				Arguments.arguments(Arrays.asList("spring.redis.timeout:100", "spring.redis.connection-timeout:500"),
						100, 500),
				Arguments.arguments(
						Arrays.asList("spring.redis.read-timeout:250", "spring.redis.connection-timeout:1000"), 250,
						1000),
				Arguments.arguments(Arrays.asList("spring.redis.timeout:100", "spring.redis.read-timeout:250",
						"spring.redis.connection-timeout:1000"), 250, 1000));
	}

	@Test
	void testRedisConfigurationWithClientName() {
		this.contextRunner.withPropertyValues("spring.redis.host:foo", "spring.redis.client-name:spring-boot")
				.run((context) -> {
					JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("foo");
					assertThat(cf.getClientName()).isEqualTo("spring-boot");
				});
	}

	@Test
	void testRedisConfigurationWithSentinel() {
		this.contextRunner
				.withPropertyValues("spring.redis.sentinel.master:mymaster",
						"spring.redis.sentinel.nodes:127.0.0.1:26379,127.0.0.1:26380")
				.withUserConfiguration(JedisConnectionFactoryCaptorConfiguration.class).run((context) -> {
					assertThat(context).hasFailed();
					assertThat(JedisConnectionFactoryCaptor.connectionFactory.isRedisSentinelAware()).isTrue();
				});
	}

	@Test
	void testRedisConfigurationWithSentinelAndPassword() {
		this.contextRunner
				.withPropertyValues("spring.redis.password=password", "spring.redis.sentinel.master:mymaster",
						"spring.redis.sentinel.nodes:127.0.0.1:26379,127.0.0.1:26380")
				.withUserConfiguration(JedisConnectionFactoryCaptorConfiguration.class).run((context) -> {
					assertThat(context).hasFailed();
					assertThat(JedisConnectionFactoryCaptor.connectionFactory.isRedisSentinelAware()).isTrue();
					assertThat(JedisConnectionFactoryCaptor.connectionFactory.getPassword()).isEqualTo("password");
				});
	}

	@Test
	void testRedisConfigurationWithCluster() {
		this.contextRunner.withPropertyValues("spring.redis.cluster.nodes=127.0.0.1:27379,127.0.0.1:27380")
				.run((context) -> assertThat(context.getBean(JedisConnectionFactory.class).getClusterConnection())
						.isNotNull());
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConfiguration {

		@Bean
		JedisClientConfigurationBuilderCustomizer customizer() {
			return JedisClientConfigurationBuilder::useSsl;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JedisConnectionFactoryCaptorConfiguration {

		@Bean
		JedisConnectionFactoryCaptor jedisConnectionFactoryCaptor() {
			return new JedisConnectionFactoryCaptor();
		}

	}

	static class JedisConnectionFactoryCaptor implements BeanPostProcessor {

		static JedisConnectionFactory connectionFactory;

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof JedisConnectionFactory) {
				connectionFactory = (JedisConnectionFactory) bean;
			}
			return bean;
		}

	}

}
