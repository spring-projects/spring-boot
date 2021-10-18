/*
 * Copyright 2012-2021 the original author or authors.
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisSocketConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link RedisAutoConfiguration} when using {@code Lettuce} and configuring via the
 * {@code spring.redis.url} property.
 *
 * @author Chris Bono
 */
class RedisAutoConfigurationLettuceUrlTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class));


	private String getUserName(LettuceConnectionFactory factory) {
		return ReflectionTestUtils.invokeMethod(factory, "getRedisUsername");
	}

	@Nested
	class RedisStandaloneUrlWithStandardScheme {

		@Test
		void withMinimalFields() {
			RedisAutoConfigurationLettuceUrlTests.this.contextRunner.withPropertyValues("spring.redis.url=redis://host1:6379").run((context) -> {
				LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("host1");
				assertThat(cf.getPort()).isEqualTo(6379);
				assertThat(getUserName(cf)).isNullOrEmpty();
				assertThat(cf.getPassword()).isNullOrEmpty();
				assertThat(cf.isUseSsl()).isFalse();
			});
		}

		@Test
		void withAllFields() {
			RedisAutoConfigurationLettuceUrlTests.this.contextRunner.withPropertyValues("spring.redis.url=redis://user:password@host1:33").run((context) -> {
				LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("host1");
				assertThat(cf.getPort()).isEqualTo(33);
				assertThat(getUserName(cf)).isEqualTo("user");
				assertThat(cf.getPassword()).isEqualTo("password");
				assertThat(cf.isUseSsl()).isFalse();
			});
		}

		@Test
		void withSsl() {
			RedisAutoConfigurationLettuceUrlTests.this.contextRunner.withPropertyValues("spring.redis.url=rediss://user:password@host1:33").run((context) -> {
				LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("host1");
				assertThat(cf.getPort()).isEqualTo(33);
				assertThat(getUserName(cf)).isEqualTo("user");
				assertThat(cf.getPassword()).isEqualTo("password");
				assertThat(cf.isUseSsl()).isTrue();
			});
		}

		@Test
		void withoutUsernameWithPasswordContainingColon() {
			RedisAutoConfigurationLettuceUrlTests.this.contextRunner.withPropertyValues("spring.redis.url=redis://:pass:word@host1:33").run((context) -> {
				LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("host1");
				assertThat(cf.getPort()).isEqualTo(33);
				assertThat(getUserName(cf)).isNullOrEmpty();
				assertThat(cf.getPassword()).isEqualTo("pass:word");
			});
		}

		@Test
		void withUsernameWithPasswordStartsWithColonAndContainsColon() {
			RedisAutoConfigurationLettuceUrlTests.this.contextRunner.withPropertyValues("spring.redis.url=redis://user::pass:word@host1:33").run((context) -> {
				LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("host1");
				assertThat(cf.getPort()).isEqualTo(33);
				assertThat(getUserName(cf)).isEqualTo("user");
				assertThat(cf.getPassword()).isEqualTo(":pass:word");
			});
		}

	}

	@Nested
	class RedisStandaloneUrlWithNonStandardScheme {

		@Test
		void withAltSsl() {
			RedisAutoConfigurationLettuceUrlTests.this.contextRunner.withPropertyValues("spring.redis.url=redis+ssl://user:password@host1:33/7").run((context) -> {
				LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("host1");
				assertThat(cf.getPort()).isEqualTo(33);
				assertThat(getUserName(cf)).isEqualTo("user");
				assertThat(cf.getPassword()).isEqualTo("password");
				assertThat(cf.getDatabase()).isEqualTo(7);
				assertThat(cf.isUseSsl()).isTrue();
			});
		}

		@Test
		void withAltTls() {
			RedisAutoConfigurationLettuceUrlTests.this.contextRunner.withPropertyValues("spring.redis.url=redis+tls://user:password@host1:33/7").run((context) -> {
				LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("host1");
				assertThat(cf.getPort()).isEqualTo(33);
				assertThat(getUserName(cf)).isEqualTo("user");
				assertThat(cf.getPassword()).isEqualTo("password");
				assertThat(cf.getDatabase()).isEqualTo(7);
				assertThat(cf.isUseSsl()).isTrue();
				assertThat(cf.isStartTls()).isTrue();
			});
		}

		@Test
		void withPropsSetOnUrlIgnoresPropsSetInConfig() {
			RedisAutoConfigurationLettuceUrlTests.this.contextRunner
					.withPropertyValues("spring.redis.url=redis+ssl://user:password@host1:33/7",
							"spring.redis.username=abc", "spring.redis.password=xyz", "spring.redis.host=foo",
							"spring.redis.port=1000", "spring.redis.database=2", "spring.redis.ssl=false")
					.run((context) -> {
						LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
						assertThat(cf.getHostName()).isEqualTo("host1");
						assertThat(cf.getPort()).isEqualTo(33);
						assertThat(getUserName(cf)).isEqualTo("user");
						assertThat(cf.getPassword()).isEqualTo("password");
						assertThat(cf.getDatabase()).isEqualTo(7);
						assertThat(cf.isUseSsl()).isTrue();
					});
		}

		@Test
		void withClientOptionPropsSetOnUrlOverridesAndRespectsPropsSetInConfig() {
			RedisAutoConfigurationLettuceUrlTests.this.contextRunner.withPropertyValues("spring.redis.url=redis+ssl://host1?timeout=47s&clientName=zuser",
					"spring.redis.timeout=1200", "spring.redis.connect-timeout=2400",
					"spring.redis.lettuce.shutdown-timeout=3600").run((context) -> {
						LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
						assertThat(cf.getClientName()).isEqualTo("zuser");
						assertThat(cf.getTimeout()).isEqualTo(47000);
						assertThat(cf.getShutdownTimeout()).isEqualTo(3600);
						assertThat(cf.getClientConfiguration().getClientOptions().get().getSocketOptions()
								.getConnectTimeout().toMillis()).isEqualTo(2400);
					});
		}

		@Test
		void withoutClientOptionPropsSetOnUrlUsesDefaultCientOptions() {
			RedisAutoConfigurationLettuceUrlTests.this.contextRunner.withPropertyValues("spring.redis.url=redis+ssl://host1").run((context) -> {
				LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
				assertThat(cf.getClientName()).isNullOrEmpty();
				assertThat(cf.getTimeout()).isEqualTo(60000);
				assertThat(cf.getShutdownTimeout()).isEqualTo(100);
				assertThat(cf.getClientConfiguration().getClientOptions().get().getSocketOptions().getConnectTimeout()
						.toMillis()).isEqualTo(10000);
			});
		}

	}

	@Nested
	class RedisSentinelUrl {

		@Test
		void withMinimalFields() {
			RedisAutoConfigurationLettuceUrlTests.this.contextRunner.withPropertyValues("spring.redis.url=redis-sentinel://127.0.0.1?sentinelMasterId=5150")
					.run((context) -> {
						LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
						assertThat(getUserName(cf)).isNullOrEmpty();
						assertThat(cf.getPassword()).isNullOrEmpty();
						assertThat(cf.getDatabase()).isEqualTo(0);
						assertThat(cf.isUseSsl()).isFalse();
						assertThat(cf.isRedisSentinelAware()).isTrue();
						RedisSentinelConfiguration sentinelConfiguration = cf.getSentinelConfiguration();
						assertThat(sentinelConfiguration.getSentinels()).flatMap(Object::toString)
								.containsExactlyInAnyOrder("127.0.0.1:26379");
						assertThat(sentinelConfiguration.getMaster().getName()).isEqualTo("5150");
						assertThat(sentinelConfiguration.getSentinelPassword().isPresent()).isFalse();
					});
		}

		@Test
		void withAllFields() {
			RedisAutoConfigurationLettuceUrlTests.this.contextRunner.withPropertyValues(
					"spring.redis.url=redis-sentinel://username:password@127.0.0.1:26379,127.0.0.1:26380/7?sentinelMasterId=5150",
					"spring.redis.sentinel.password: secret").run((context) -> {
						LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
						assertThat(getUserName(cf)).isEqualTo("username");
						assertThat(cf.getPassword()).isEqualTo("password");
						assertThat(cf.getDatabase()).isEqualTo(7);
						assertThat(cf.isUseSsl()).isFalse();
						assertThat(cf.isRedisSentinelAware()).isTrue();
						RedisSentinelConfiguration sentinelConfiguration = cf.getSentinelConfiguration();
						assertThat(sentinelConfiguration.getSentinels()).flatMap(Object::toString)
								.containsExactlyInAnyOrder("127.0.0.1:26379", "127.0.0.1:26380");
						assertThat(sentinelConfiguration.getMaster().getName()).isEqualTo("5150");
						assertThat(sentinelConfiguration.getSentinelPassword().get()).isEqualTo("secret".toCharArray());
					});

		}

		@Test
		void withSsl() {
			RedisAutoConfigurationLettuceUrlTests.this.contextRunner.withPropertyValues("spring.redis.url=rediss-sentinel://127.0.0.1?sentinelMasterId=5150")
					.run((context) -> {
						LettuceConnectionFactory connectionFactory = context.getBean(LettuceConnectionFactory.class);
						assertThat(connectionFactory.isRedisSentinelAware()).isTrue();
						assertThat(connectionFactory.isUseSsl()).isTrue();
					});
		}

		@Test
		void withPropsSetOnUrlIgnoresPropsSetInConfig() {
			RedisAutoConfigurationLettuceUrlTests.this.contextRunner.withPropertyValues(
					"spring.redis.url=redis-sentinel://username:password@127.0.0.1:26379,127.0.0.1:26380/7?sentinelMasterId=5150",
					"spring.redis.sentinel.master=mymaster", "spring.redis.sentinel.nodes=server1:111, server2:222")
					.run((context) -> {
						LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
						assertThat(getUserName(cf)).isEqualTo("username");
						assertThat(cf.getPassword()).isEqualTo("password");
						assertThat(cf.getDatabase()).isEqualTo(7);
						assertThat(cf.isUseSsl()).isFalse();
						assertThat(cf.isRedisSentinelAware()).isTrue();
						RedisSentinelConfiguration sentinelConfiguration = cf.getSentinelConfiguration();
						assertThat(sentinelConfiguration.getSentinels()).flatMap(Object::toString)
								.containsExactlyInAnyOrder("127.0.0.1:26379", "127.0.0.1:26380");
						assertThat(sentinelConfiguration.getMaster().getName()).isEqualTo("5150");
					});
		}

	}

	@Nested
	class RedisSocketUrl {

		@Test
		void withMinimalFields() {
			RedisAutoConfigurationLettuceUrlTests.this.contextRunner.withPropertyValues("spring.redis.url=redis-socket:///mysocket").run((context) -> {
				LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
				assertThat(getUserName(cf)).isNullOrEmpty();
				assertThat(cf.getPassword()).isNullOrEmpty();
				assertThat(cf.getDatabase()).isEqualTo(0);
				assertThat(cf.getSocketConfiguration()).extracting(RedisSocketConfiguration::getSocket)
						.isEqualTo("/mysocket");
			});
		}

		@Test
		void withAllFields() {
			RedisAutoConfigurationLettuceUrlTests.this.contextRunner.withPropertyValues("spring.redis.url=redis-socket://user:password@/mysocket?database=7")
					.run((context) -> {
						LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
						assertThat(getUserName(cf)).isEqualTo("user");
						assertThat(cf.getPassword()).isEqualTo("password");
						assertThat(cf.getDatabase()).isEqualTo(7);
						assertThat(cf.getSocketConfiguration()).extracting(RedisSocketConfiguration::getSocket)
								.isEqualTo("/mysocket");
					});
		}

		@Test
		void withAltScheme() {
			RedisAutoConfigurationLettuceUrlTests.this.contextRunner.withPropertyValues("spring.redis.url=redis+socket://user:password@/mysocket?database=7")
					.run((context) -> {
						LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
						assertThat(getUserName(cf)).isEqualTo("user");
						assertThat(cf.getPassword()).isEqualTo("password");
						assertThat(cf.getDatabase()).isEqualTo(7);
						assertThat(cf.getSocketConfiguration()).extracting(RedisSocketConfiguration::getSocket)
								.isEqualTo("/mysocket");
					});
		}

	}

}
