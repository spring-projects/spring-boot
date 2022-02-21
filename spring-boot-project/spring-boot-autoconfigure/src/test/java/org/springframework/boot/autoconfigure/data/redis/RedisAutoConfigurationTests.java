/*
 * Copyright 2012-2022 the original author or authors.
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

import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions.RefreshTrigger;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.tracing.Tracing;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Pool;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RedisAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Christoph Strobl
 * @author Eddú Meléndez
 * @author Marco Aust
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @author Alen Turkovic
 * @author Scott Frederick
 * @author Weix Sun
 */
class RedisAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class));

	@Test
	void testDefaultRedisConfiguration() {
		this.contextRunner.run((context) -> {
			assertThat(context.getBean("redisTemplate")).isInstanceOf(RedisOperations.class);
			assertThat(context).hasSingleBean(StringRedisTemplate.class);
			assertThat(context).hasSingleBean(RedisConnectionFactory.class);
			assertThat(context.getBean(RedisConnectionFactory.class)).isInstanceOf(LettuceConnectionFactory.class);
		});
	}

	@Test
	void testOverrideRedisConfiguration() {
		this.contextRunner.withPropertyValues("spring.redis.host:foo", "spring.redis.database:1",
				"spring.redis.lettuce.shutdown-timeout:500").run((context) -> {
					LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("foo");
					assertThat(cf.getDatabase()).isEqualTo(1);
					assertThat(getUserName(cf)).isNull();
					assertThat(cf.getPassword()).isNull();
					assertThat(cf.isUseSsl()).isFalse();
					assertThat(cf.getShutdownTimeout()).isEqualTo(500);
				});
	}

	@Test
	void testCustomizeClientResources() {
		Tracing tracing = mock(Tracing.class);
		this.contextRunner.withBean(ClientResourcesBuilderCustomizer.class, () -> (builder) -> builder.tracing(tracing))
				.run((context) -> {
					DefaultClientResources clientResources = context.getBean(DefaultClientResources.class);
					assertThat(clientResources.tracing()).isEqualTo(tracing);
				});
	}

	@Test
	void testCustomizeRedisConfiguration() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
			assertThat(cf.isUseSsl()).isTrue();
		});
	}

	@Test
	void testRedisUrlConfiguration() {
		this.contextRunner
				.withPropertyValues("spring.redis.host:foo", "spring.redis.url:redis://user:password@example:33")
				.run((context) -> {
					LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
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
				.withPropertyValues("spring.redis.host:foo", "spring.redis.password:xyz", "spring.redis.port:1000",
						"spring.redis.ssl:false", "spring.redis.url:rediss://user:password@example:33")
				.run((context) -> {
					LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("example");
					assertThat(cf.getPort()).isEqualTo(33);
					assertThat(getUserName(cf)).isEqualTo("user");
					assertThat(cf.getPassword()).isEqualTo("password");
					assertThat(cf.isUseSsl()).isTrue();
				});
	}

	@Test
	void testPasswordInUrlWithColon() {
		this.contextRunner.withPropertyValues("spring.redis.url:redis://:pass:word@example:33").run((context) -> {
			LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
			assertThat(cf.getHostName()).isEqualTo("example");
			assertThat(cf.getPort()).isEqualTo(33);
			assertThat(getUserName(cf)).isEqualTo("");
			assertThat(cf.getPassword()).isEqualTo("pass:word");
		});
	}

	@Test
	void testPasswordInUrlStartsWithColon() {
		this.contextRunner.withPropertyValues("spring.redis.url:redis://user::pass:word@example:33").run((context) -> {
			LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
			assertThat(cf.getHostName()).isEqualTo("example");
			assertThat(cf.getPort()).isEqualTo(33);
			assertThat(getUserName(cf)).isEqualTo("user");
			assertThat(cf.getPassword()).isEqualTo(":pass:word");
		});
	}

	@Test
	void testRedisConfigurationUsePoolByDefault() {
		Pool defaultPool = new RedisProperties().getLettuce().getPool();
		this.contextRunner.withPropertyValues("spring.redis.host:foo").run((context) -> {
			LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
			assertThat(cf.getHostName()).isEqualTo("foo");
			GenericObjectPoolConfig<?> poolConfig = getPoolingClientConfiguration(cf).getPoolConfig();
			assertThat(poolConfig.getMinIdle()).isEqualTo(defaultPool.getMinIdle());
			assertThat(poolConfig.getMaxIdle()).isEqualTo(defaultPool.getMaxIdle());
			assertThat(poolConfig.getMaxTotal()).isEqualTo(defaultPool.getMaxActive());
			assertThat(poolConfig.getMaxWaitDuration()).isEqualTo(defaultPool.getMaxWait());
		});
	}

	@Test
	void testRedisConfigurationWithCustomPoolSettings() {
		this.contextRunner.withPropertyValues("spring.redis.host:foo", "spring.redis.lettuce.pool.min-idle:1",
				"spring.redis.lettuce.pool.max-idle:4", "spring.redis.lettuce.pool.max-active:16",
				"spring.redis.lettuce.pool.max-wait:2000", "spring.redis.lettuce.pool.time-between-eviction-runs:30000",
				"spring.redis.lettuce.shutdown-timeout:1000").run((context) -> {
					LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("foo");
					GenericObjectPoolConfig<?> poolConfig = getPoolingClientConfiguration(cf).getPoolConfig();
					assertThat(poolConfig.getMinIdle()).isEqualTo(1);
					assertThat(poolConfig.getMaxIdle()).isEqualTo(4);
					assertThat(poolConfig.getMaxTotal()).isEqualTo(16);
					assertThat(poolConfig.getMaxWaitDuration()).isEqualTo(Duration.ofSeconds(2));
					assertThat(poolConfig.getDurationBetweenEvictionRuns()).isEqualTo(Duration.ofSeconds(30));
					assertThat(cf.getShutdownTimeout()).isEqualTo(1000);
				});
	}

	@Test
	void testRedisConfigurationDisabledPool() {
		this.contextRunner.withPropertyValues("spring.redis.host:foo", "spring.redis.lettuce.pool.enabled:false")
				.run((context) -> {
					LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("foo");
					assertThat(cf.getClientConfiguration()).isNotInstanceOf(LettucePoolingClientConfiguration.class);
				});
	}

	@Test
	void testRedisConfigurationWithTimeoutAndConnectTimeout() {
		this.contextRunner.withPropertyValues("spring.redis.host:foo", "spring.redis.timeout:250",
				"spring.redis.connect-timeout:1000").run((context) -> {
					LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("foo");
					assertThat(cf.getTimeout()).isEqualTo(250);
					assertThat(cf.getClientConfiguration().getClientOptions().get().getSocketOptions()
							.getConnectTimeout().toMillis()).isEqualTo(1000);
				});
	}

	@Test
	void testRedisConfigurationWithDefaultTimeouts() {
		this.contextRunner.withPropertyValues("spring.redis.host:foo").run((context) -> {
			LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
			assertThat(cf.getHostName()).isEqualTo("foo");
			assertThat(cf.getTimeout()).isEqualTo(60000);
			assertThat(cf.getClientConfiguration().getClientOptions().get().getSocketOptions().getConnectTimeout()
					.toMillis()).isEqualTo(10000);
		});
	}

	@Test
	void testRedisConfigurationWithCustomBean() {
		this.contextRunner.withUserConfiguration(RedisStandaloneConfig.class).run((context) -> {
			LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
			assertThat(cf.getHostName()).isEqualTo("foo");
		});
	}

	@Test
	void testRedisConfigurationWithClientName() {
		this.contextRunner.withPropertyValues("spring.redis.host:foo", "spring.redis.client-name:spring-boot")
				.run((context) -> {
					LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("foo");
					assertThat(cf.getClientName()).isEqualTo("spring-boot");
				});
	}

	@Test
	void connectionFactoryWithJedisClientType() {
		this.contextRunner.withPropertyValues("spring.redis.client-type:jedis").run((context) -> {
			assertThat(context).hasSingleBean(RedisConnectionFactory.class);
			assertThat(context.getBean(RedisConnectionFactory.class)).isInstanceOf(JedisConnectionFactory.class);
		});
	}

	@Test
	void connectionFactoryWithLettuceClientType() {
		this.contextRunner.withPropertyValues("spring.redis.client-type:lettuce").run((context) -> {
			assertThat(context).hasSingleBean(RedisConnectionFactory.class);
			assertThat(context.getBean(RedisConnectionFactory.class)).isInstanceOf(LettuceConnectionFactory.class);
		});
	}

	@Test
	void testRedisConfigurationWithSentinel() {
		List<String> sentinels = Arrays.asList("127.0.0.1:26379", "127.0.0.1:26380");
		this.contextRunner
				.withPropertyValues("spring.redis.sentinel.master:mymaster",
						"spring.redis.sentinel.nodes:" + StringUtils.collectionToCommaDelimitedString(sentinels))
				.run((context) -> assertThat(context.getBean(LettuceConnectionFactory.class).isRedisSentinelAware())
						.isTrue());
	}

	@Test
	void testRedisConfigurationWithSentinelAndDatabase() {
		this.contextRunner.withPropertyValues("spring.redis.database:1", "spring.redis.sentinel.master:mymaster",
				"spring.redis.sentinel.nodes:127.0.0.1:26379, 127.0.0.1:26380").run((context) -> {
					LettuceConnectionFactory connectionFactory = context.getBean(LettuceConnectionFactory.class);
					assertThat(connectionFactory.getDatabase()).isEqualTo(1);
					assertThat(connectionFactory.isRedisSentinelAware()).isTrue();
				});
	}

	@Test
	void testRedisConfigurationWithSentinelAndAuthentication() {
		this.contextRunner
				.withPropertyValues("spring.redis.username=user", "spring.redis.password=password",
						"spring.redis.sentinel.master:mymaster",
						"spring.redis.sentinel.nodes:127.0.0.1:26379,  127.0.0.1:26380")
				.run(assertSentinelConfiguration("user", "password", (sentinelConfiguration) -> {
					assertThat(sentinelConfiguration.getSentinelPassword().isPresent()).isFalse();
					Set<RedisNode> sentinels = sentinelConfiguration.getSentinels();
					assertThat(sentinels.stream().map(Object::toString).collect(Collectors.toSet()))
							.contains("127.0.0.1:26379", "127.0.0.1:26380");
				}));
	}

	@Test
	void testRedisConfigurationWithSentinelPasswordAndDataNodePassword() {
		this.contextRunner
				.withPropertyValues("spring.redis.password=password", "spring.redis.sentinel.password=secret",
						"spring.redis.sentinel.master:mymaster",
						"spring.redis.sentinel.nodes:127.0.0.1:26379,  127.0.0.1:26380")
				.run(assertSentinelConfiguration(null, "password", (sentinelConfiguration) -> {
					assertThat(sentinelConfiguration.getSentinelUsername()).isNull();
					assertThat(new String(sentinelConfiguration.getSentinelPassword().get())).isEqualTo("secret");
					Set<RedisNode> sentinels = sentinelConfiguration.getSentinels();
					assertThat(sentinels.stream().map(Object::toString).collect(Collectors.toSet()))
							.contains("127.0.0.1:26379", "127.0.0.1:26380");
				}));
	}

	@Test
	void testRedisConfigurationWithSentinelAuthenticationAndDataNodeAuthentication() {
		this.contextRunner
				.withPropertyValues("spring.redis.username=username", "spring.redis.password=password",
						"spring.redis.sentinel.username=sentinel", "spring.redis.sentinel.password=secret",
						"spring.redis.sentinel.master:mymaster",
						"spring.redis.sentinel.nodes:127.0.0.1:26379,  127.0.0.1:26380")
				.run(assertSentinelConfiguration("username", "password", (sentinelConfiguration) -> {
					assertThat(sentinelConfiguration.getSentinelUsername()).isEqualTo("sentinel");
					assertThat(new String(sentinelConfiguration.getSentinelPassword().get())).isEqualTo("secret");
					Set<RedisNode> sentinels = sentinelConfiguration.getSentinels();
					assertThat(sentinels.stream().map(Object::toString).collect(Collectors.toSet()))
							.contains("127.0.0.1:26379", "127.0.0.1:26380");
				}));
	}

	private ContextConsumer<AssertableApplicationContext> assertSentinelConfiguration(String userName, String password,
			Consumer<RedisSentinelConfiguration> sentinelConfiguration) {
		return (context) -> {
			LettuceConnectionFactory connectionFactory = context.getBean(LettuceConnectionFactory.class);
			assertThat(getUserName(connectionFactory)).isEqualTo(userName);
			assertThat(connectionFactory.getPassword()).isEqualTo(password);
			assertThat(connectionFactory.getSentinelConfiguration()).satisfies(sentinelConfiguration);
		};
	}

	@Test
	void testRedisSentinelUrlConfiguration() {
		this.contextRunner
				.withPropertyValues(
						"spring.redis.url=redis-sentinel://username:password@127.0.0.1:26379,127.0.0.1:26380/mymaster")
				.run((context) -> assertThatIllegalStateException()
						.isThrownBy(() -> context.getBean(LettuceConnectionFactory.class))
						.withRootCauseInstanceOf(RedisUrlSyntaxException.class).havingRootCause().withMessageContaining(
								"Invalid Redis URL 'redis-sentinel://username:password@127.0.0.1:26379,127.0.0.1:26380/mymaster'"));
	}

	@Test
	void testRedisConfigurationWithCluster() {
		List<String> clusterNodes = Arrays.asList("127.0.0.1:27379", "127.0.0.1:27380");
		this.contextRunner.withPropertyValues("spring.redis.cluster.nodes[0]:" + clusterNodes.get(0),
				"spring.redis.cluster.nodes[1]:" + clusterNodes.get(1)).run((context) -> {
					RedisClusterConfiguration clusterConfiguration = context.getBean(LettuceConnectionFactory.class)
							.getClusterConfiguration();
					assertThat(clusterConfiguration.getClusterNodes()).hasSize(2);
					assertThat(clusterConfiguration.getClusterNodes())
							.extracting((node) -> node.getHost() + ":" + node.getPort())
							.containsExactlyInAnyOrder("127.0.0.1:27379", "127.0.0.1:27380");
				});

	}

	@Test
	void testRedisConfigurationWithClusterAndAuthentication() {
		List<String> clusterNodes = Arrays.asList("127.0.0.1:27379", "127.0.0.1:27380");
		this.contextRunner.withPropertyValues("spring.redis.username=user", "spring.redis.password=password",
				"spring.redis.cluster.nodes[0]:" + clusterNodes.get(0),
				"spring.redis.cluster.nodes[1]:" + clusterNodes.get(1)).run((context) -> {
					LettuceConnectionFactory connectionFactory = context.getBean(LettuceConnectionFactory.class);
					assertThat(getUserName(connectionFactory)).isEqualTo("user");
					assertThat(connectionFactory.getPassword()).isEqualTo("password");
				}

		);
	}

	@Test
	void testRedisConfigurationCreateClientOptionsByDefault() {
		this.contextRunner.run(assertClientOptions(ClientOptions.class, (options) -> {
			assertThat(options.getTimeoutOptions().isApplyConnectionTimeout()).isTrue();
			assertThat(options.getTimeoutOptions().isTimeoutCommands()).isTrue();
		}));
	}

	@Test
	void testRedisConfigurationWithClusterCreateClusterClientOptions() {
		this.contextRunner.withPropertyValues("spring.redis.cluster.nodes=127.0.0.1:27379,127.0.0.1:27380")
				.run(assertClientOptions(ClusterClientOptions.class, (options) -> {
					assertThat(options.getTimeoutOptions().isApplyConnectionTimeout()).isTrue();
					assertThat(options.getTimeoutOptions().isTimeoutCommands()).isTrue();
				}));
	}

	@Test
	void testRedisConfigurationWithClusterRefreshPeriod() {
		this.contextRunner
				.withPropertyValues("spring.redis.cluster.nodes=127.0.0.1:27379,127.0.0.1:27380",
						"spring.redis.lettuce.cluster.refresh.period=30s")
				.run(assertClientOptions(ClusterClientOptions.class,
						(options) -> assertThat(options.getTopologyRefreshOptions().getRefreshPeriod())
								.hasSeconds(30)));
	}

	@Test
	void testRedisConfigurationWithClusterAdaptiveRefresh() {
		this.contextRunner
				.withPropertyValues("spring.redis.cluster.nodes=127.0.0.1:27379,127.0.0.1:27380",
						"spring.redis.lettuce.cluster.refresh.adaptive=true")
				.run(assertClientOptions(ClusterClientOptions.class,
						(options) -> assertThat(options.getTopologyRefreshOptions().getAdaptiveRefreshTriggers())
								.isEqualTo(EnumSet.allOf(RefreshTrigger.class))));
	}

	@Test
	void testRedisConfigurationWithClusterRefreshPeriodHasNoEffectWithNonClusteredConfiguration() {
		this.contextRunner.withPropertyValues("spring.redis.cluster.refresh.period=30s").run(assertClientOptions(
				ClientOptions.class, (options) -> assertThat(options.getClass()).isEqualTo(ClientOptions.class)));
	}

	@Test
	void testRedisConfigurationWithClusterDynamicRefreshSourcesEnabled() {
		this.contextRunner
				.withPropertyValues("spring.redis.cluster.nodes=127.0.0.1:27379,127.0.0.1:27380",
						"spring.redis.lettuce.cluster.refresh.dynamic-refresh-sources=true")
				.run(assertClientOptions(ClusterClientOptions.class,
						(options) -> assertThat(options.getTopologyRefreshOptions().useDynamicRefreshSources())
								.isTrue()));
	}

	@Test
	void testRedisConfigurationWithClusterDynamicRefreshSourcesDisabled() {
		this.contextRunner
				.withPropertyValues("spring.redis.cluster.nodes=127.0.0.1:27379,127.0.0.1:27380",
						"spring.redis.lettuce.cluster.refresh.dynamic-refresh-sources=false")
				.run(assertClientOptions(ClusterClientOptions.class,
						(options) -> assertThat(options.getTopologyRefreshOptions().useDynamicRefreshSources())
								.isFalse()));
	}

	@Test
	void testRedisConfigurationWithClusterDynamicSourcesUnspecifiedUsesDefault() {
		this.contextRunner
				.withPropertyValues("spring.redis.cluster.nodes=127.0.0.1:27379,127.0.0.1:27380",
						"spring.redis.lettuce.cluster.refresh.dynamic-sources=")
				.run(assertClientOptions(ClusterClientOptions.class,
						(options) -> assertThat(options.getTopologyRefreshOptions().useDynamicRefreshSources())
								.isEqualTo(ClusterTopologyRefreshOptions.DEFAULT_DYNAMIC_REFRESH_SOURCES)));
	}

	private <T extends ClientOptions> ContextConsumer<AssertableApplicationContext> assertClientOptions(
			Class<T> expectedType, Consumer<T> options) {
		return (context) -> {
			LettuceClientConfiguration clientConfiguration = context.getBean(LettuceConnectionFactory.class)
					.getClientConfiguration();
			assertThat(clientConfiguration.getClientOptions()).isPresent();
			ClientOptions clientOptions = clientConfiguration.getClientOptions().get();
			assertThat(clientOptions.getClass()).isEqualTo(expectedType);
			options.accept(expectedType.cast(clientOptions));
		};
	}

	private LettucePoolingClientConfiguration getPoolingClientConfiguration(LettuceConnectionFactory factory) {
		return (LettucePoolingClientConfiguration) factory.getClientConfiguration();
	}

	private String getUserName(LettuceConnectionFactory factory) {
		return ReflectionTestUtils.invokeMethod(factory, "getRedisUsername");
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConfiguration {

		@Bean
		LettuceClientConfigurationBuilderCustomizer customizer() {
			return LettuceClientConfigurationBuilder::useSsl;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RedisStandaloneConfig {

		@Bean
		RedisStandaloneConfiguration standaloneConfiguration() {
			RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
			config.setHostName("foo");
			return config;
		}

	}

}
