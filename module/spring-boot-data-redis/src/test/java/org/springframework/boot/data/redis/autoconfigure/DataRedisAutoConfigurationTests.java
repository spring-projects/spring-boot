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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.ReadFrom.Nodes;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions.RefreshTrigger;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.tracing.Tracing;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties.Pool;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.testsupport.assertj.SimpleAsyncTaskExecutorAssert;
import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.connection.NamedNode;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.RedisStaticMasterReplicaConfiguration;
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
 * Tests for {@link DataRedisAutoConfiguration}.
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
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DataRedisAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration.class, SslAutoConfiguration.class));

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
		this.contextRunner
			.withPropertyValues("spring.data.redis.host:foo", "spring.data.redis.database:1",
					"spring.data.redis.lettuce.shutdown-timeout:500")
			.run((context) -> {
				LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("foo");
				assertThat(cf.getDatabase()).isOne();
				assertThat(getUserName(cf)).isNull();
				assertThat(cf.getPassword()).isNull();
				assertThat(cf.isUseSsl()).isFalse();
				assertThat(cf.getShutdownTimeout()).isEqualTo(500);
			});
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource
	void shouldConfigureLettuceReadFromProperty(String type, ReadFrom readFrom) {
		this.contextRunner.withPropertyValues("spring.data.redis.lettuce.read-from:" + type).run((context) -> {
			LettuceConnectionFactory factory = context.getBean(LettuceConnectionFactory.class);
			LettuceClientConfiguration configuration = factory.getClientConfiguration();
			assertThat(configuration.getReadFrom()).hasValue(readFrom);
		});
	}

	static Stream<Arguments> shouldConfigureLettuceReadFromProperty() {
		return Stream.of(Arguments.of("any", ReadFrom.ANY), Arguments.of("any-replica", ReadFrom.ANY_REPLICA),
				Arguments.of("lowest-latency", ReadFrom.LOWEST_LATENCY), Arguments.of("replica", ReadFrom.REPLICA),
				Arguments.of("replica-preferred", ReadFrom.REPLICA_PREFERRED),
				Arguments.of("upstream", ReadFrom.UPSTREAM),
				Arguments.of("upstream-preferred", ReadFrom.UPSTREAM_PREFERRED));
	}

	@Test
	void shouldConfigureLettuceRegexReadFromProperty() {
		RedisClusterNode node1 = createRedisNode("redis-node-1.region-1.example.com");
		RedisClusterNode node2 = createRedisNode("redis-node-2.region-1.example.com");
		RedisClusterNode node3 = createRedisNode("redis-node-1.region-2.example.com");
		RedisClusterNode node4 = createRedisNode("redis-node-2.region-2.example.com");
		this.contextRunner.withPropertyValues("spring.data.redis.lettuce.read-from:regex:.*region-1.*")
			.run((context) -> {
				LettuceConnectionFactory factory = context.getBean(LettuceConnectionFactory.class);
				LettuceClientConfiguration configuration = factory.getClientConfiguration();
				assertThat(configuration.getReadFrom()).hasValueSatisfying((readFrom) -> {
					List<RedisNodeDescription> result = readFrom.select(new RedisNodes(node1, node2, node3, node4));
					assertThat(result).hasSize(2).containsExactly(node1, node2);
				});
			});
	}

	@Test
	void shouldConfigureLettuceSubnetReadFromProperty() {
		RedisClusterNode nodeInSubnetIpv4 = createRedisNode("192.0.2.1");
		RedisClusterNode nodeNotInSubnetIpv4 = createRedisNode("198.51.100.1");
		RedisClusterNode nodeInSubnetIpv6 = createRedisNode("2001:db8:abcd:0000::1");
		RedisClusterNode nodeNotInSubnetIpv6 = createRedisNode("2001:db8:abcd:1000::");
		this.contextRunner
			.withPropertyValues("spring.data.redis.lettuce.read-from:subnet:192.0.2.0/24,2001:db8:abcd:0000::/52")
			.run((context) -> {
				LettuceConnectionFactory factory = context.getBean(LettuceConnectionFactory.class);
				LettuceClientConfiguration configuration = factory.getClientConfiguration();
				assertThat(configuration.getReadFrom()).hasValueSatisfying((readFrom) -> {
					List<RedisNodeDescription> result = readFrom.select(new RedisNodes(nodeInSubnetIpv4,
							nodeNotInSubnetIpv4, nodeInSubnetIpv6, nodeNotInSubnetIpv6));
					assertThat(result).hasSize(2).containsExactly(nodeInSubnetIpv4, nodeInSubnetIpv6);
				});
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
			assertThat(cf.getClientConfiguration().getClientOptions())
				.hasValueSatisfying((options) -> assertThat(options.isAutoReconnect()).isFalse());
		});
	}

	@Test
	void testRedisUrlConfiguration() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.host:foo", "spring.data.redis.url:redis://user:password@example:33")
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
			.withPropertyValues("spring.data.redis.host:foo", "spring.redis.data.user:alice",
					"spring.data.redis.password:xyz", "spring.data.redis.port:1000",
					"spring.data.redis.ssl.enabled:false", "spring.data.redis.url:rediss://user:password@example:33")
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
		this.contextRunner.withPropertyValues("spring.data.redis.url:redis://:pass:word@example:33").run((context) -> {
			LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
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
				LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("example");
				assertThat(cf.getPort()).isEqualTo(33);
				assertThat(getUserName(cf)).isEqualTo("user");
				assertThat(cf.getPassword()).isEqualTo(":pass:word");
			});
	}

	@Test
	void testRedisConfigurationUsePoolByDefault() {
		Pool defaultPool = new DataRedisProperties().getLettuce().getPool();
		this.contextRunner.withPropertyValues("spring.data.redis.host:foo").run((context) -> {
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
		this.contextRunner
			.withPropertyValues("spring.data.redis.host:foo", "spring.data.redis.lettuce.pool.min-idle:1",
					"spring.data.redis.lettuce.pool.max-idle:4", "spring.data.redis.lettuce.pool.max-active:16",
					"spring.data.redis.lettuce.pool.max-wait:2000",
					"spring.data.redis.lettuce.pool.time-between-eviction-runs:30000",
					"spring.data.redis.lettuce.shutdown-timeout:1000")
			.run((context) -> {
				LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("foo");
				GenericObjectPoolConfig<?> poolConfig = getPoolingClientConfiguration(cf).getPoolConfig();
				assertThat(poolConfig.getMinIdle()).isOne();
				assertThat(poolConfig.getMaxIdle()).isEqualTo(4);
				assertThat(poolConfig.getMaxTotal()).isEqualTo(16);
				assertThat(poolConfig.getMaxWaitDuration()).isEqualTo(Duration.ofSeconds(2));
				assertThat(poolConfig.getDurationBetweenEvictionRuns()).isEqualTo(Duration.ofSeconds(30));
				assertThat(cf.getShutdownTimeout()).isEqualTo(1000);
			});
	}

	@Test
	void testRedisConfigurationDisabledPool() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.host:foo", "spring.data.redis.lettuce.pool.enabled:false")
			.run((context) -> {
				LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("foo");
				assertThat(cf.getClientConfiguration()).isNotInstanceOf(LettucePoolingClientConfiguration.class);
			});
	}

	@Test
	void testRedisConfigurationWithTimeoutAndConnectTimeout() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.host:foo", "spring.data.redis.timeout:250",
					"spring.data.redis.connect-timeout:1000")
			.run((context) -> {
				LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("foo");
				assertThat(cf.getTimeout()).isEqualTo(250);
				assertThat(cf.getClientConfiguration()
					.getClientOptions()
					.get()
					.getSocketOptions()
					.getConnectTimeout()
					.toMillis()).isEqualTo(1000);
			});
	}

	@Test
	void testRedisConfigurationWithDefaultTimeouts() {
		this.contextRunner.withPropertyValues("spring.data.redis.host:foo").run((context) -> {
			LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
			assertThat(cf.getHostName()).isEqualTo("foo");
			assertThat(cf.getTimeout()).isEqualTo(60000);
			assertThat(cf.getClientConfiguration()
				.getClientOptions()
				.get()
				.getSocketOptions()
				.getConnectTimeout()
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
		this.contextRunner.withPropertyValues("spring.data.redis.host:foo", "spring.data.redis.client-name:spring-boot")
			.run((context) -> {
				LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
				assertThat(cf.getHostName()).isEqualTo("foo");
				assertThat(cf.getClientName()).isEqualTo("spring-boot");
			});
	}

	@Test
	void connectionFactoryWithJedisClientType() {
		this.contextRunner.withPropertyValues("spring.data.redis.client-type:jedis").run((context) -> {
			assertThat(context).hasSingleBean(RedisConnectionFactory.class);
			assertThat(context.getBean(RedisConnectionFactory.class)).isInstanceOf(JedisConnectionFactory.class);
		});
	}

	@Test
	void connectionFactoryWithLettuceClientType() {
		this.contextRunner.withPropertyValues("spring.data.redis.client-type:lettuce").run((context) -> {
			assertThat(context).hasSingleBean(RedisConnectionFactory.class);
			assertThat(context.getBean(RedisConnectionFactory.class)).isInstanceOf(LettuceConnectionFactory.class);
		});
	}

	@Test
	void testRedisConfigurationWithSentinel() {
		List<String> sentinels = Arrays.asList("127.0.0.1:26379", "127.0.0.1:26380");
		this.contextRunner
			.withPropertyValues("spring.data.redis.sentinel.master:mymaster",
					"spring.data.redis.sentinel.nodes:" + StringUtils.collectionToCommaDelimitedString(sentinels))
			.run((context) -> assertThat(context.getBean(LettuceConnectionFactory.class).isRedisSentinelAware())
				.isTrue());
	}

	@Test
	void testRedisConfigurationWithIpv6Sentinel() {
		List<String> sentinels = Arrays.asList("[0:0:0:0:0:0:0:1]:26379", "[0:0:0:0:0:0:0:1]:26380");
		this.contextRunner
			.withPropertyValues("spring.data.redis.sentinel.master:mymaster",
					"spring.data.redis.sentinel.nodes:" + StringUtils.collectionToCommaDelimitedString(sentinels))
			.run((context) -> {
				LettuceConnectionFactory connectionFactory = context.getBean(LettuceConnectionFactory.class);
				assertThat(connectionFactory.isRedisSentinelAware()).isTrue();
				RedisSentinelConfiguration sentinelConfiguration = connectionFactory.getSentinelConfiguration();
				assertThat(sentinelConfiguration).isNotNull();
				assertThat(sentinelConfiguration.getSentinels()).isNotNull()
					.containsExactlyInAnyOrder(new RedisNode("[0:0:0:0:0:0:0:1]", 26379),
							new RedisNode("[0:0:0:0:0:0:0:1]", 26380));
			});
	}

	@Test
	void testRedisConfigurationWithSentinelAndDatabase() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.database:1", "spring.data.redis.sentinel.master:mymaster",
					"spring.data.redis.sentinel.nodes:127.0.0.1:26379, 127.0.0.1:26380")
			.run((context) -> {
				LettuceConnectionFactory connectionFactory = context.getBean(LettuceConnectionFactory.class);
				assertThat(connectionFactory.getDatabase()).isOne();
				assertThat(connectionFactory.isRedisSentinelAware()).isTrue();
			});
	}

	@Test
	void testRedisConfigurationWithSentinelAndAuthentication() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.username=user", "spring.data.redis.password=password",
					"spring.data.redis.sentinel.master:mymaster",
					"spring.data.redis.sentinel.nodes:127.0.0.1:26379,  127.0.0.1:26380")
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
			.withPropertyValues("spring.data.redis.password=password", "spring.data.redis.sentinel.password=secret",
					"spring.data.redis.sentinel.master:mymaster",
					"spring.data.redis.sentinel.nodes:127.0.0.1:26379,  127.0.0.1:26380")
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
			.withPropertyValues("spring.data.redis.username=username", "spring.data.redis.password=password",
					"spring.data.redis.sentinel.username=sentinel", "spring.data.redis.sentinel.password=secret",
					"spring.data.redis.sentinel.master:mymaster",
					"spring.data.redis.sentinel.nodes:127.0.0.1:26379,  127.0.0.1:26380")
			.run(assertSentinelConfiguration("username", "password", (sentinelConfiguration) -> {
				assertThat(sentinelConfiguration.getSentinelUsername()).isEqualTo("sentinel");
				assertThat(new String(sentinelConfiguration.getSentinelPassword().get())).isEqualTo("secret");
				Set<RedisNode> sentinels = sentinelConfiguration.getSentinels();
				assertThat(sentinels.stream().map(Object::toString).collect(Collectors.toSet()))
					.contains("127.0.0.1:26379", "127.0.0.1:26380");
			}));
	}

	private ContextConsumer<AssertableApplicationContext> assertSentinelConfiguration(@Nullable String userName,
			String password, Consumer<RedisSentinelConfiguration> sentinelConfiguration) {
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
					"spring.data.redis.url=redis-sentinel://username:password@127.0.0.1:26379,127.0.0.1:26380/mymaster")
			.run((context) -> assertThatIllegalStateException()
				.isThrownBy(() -> context.getBean(LettuceConnectionFactory.class))
				.withRootCauseInstanceOf(DataRedisUrlSyntaxException.class)
				.havingRootCause()
				.withMessageContaining(
						"Invalid Redis URL 'redis-sentinel://username:password@127.0.0.1:26379,127.0.0.1:26380/mymaster'"));
	}

	@Test
	void testRedisConfigurationWithCluster() {
		List<String> clusterNodes = Arrays.asList("127.0.0.1:27379", "127.0.0.1:27380", "[::1]:27381");
		this.contextRunner
			.withPropertyValues("spring.data.redis.cluster.nodes[0]:" + clusterNodes.get(0),
					"spring.data.redis.cluster.nodes[1]:" + clusterNodes.get(1),
					"spring.data.redis.cluster.nodes[2]:" + clusterNodes.get(2))
			.run((context) -> {
				RedisClusterConfiguration clusterConfiguration = context.getBean(LettuceConnectionFactory.class)
					.getClusterConfiguration();
				assertThat(clusterConfiguration).isNotNull();
				assertThat(clusterConfiguration.getClusterNodes()).hasSize(3);
				assertThat(clusterConfiguration.getClusterNodes()).containsExactlyInAnyOrder(
						new RedisNode("127.0.0.1", 27379), new RedisNode("127.0.0.1", 27380),
						new RedisNode("[::1]", 27381));
			});
	}

	@Test
	void testRedisConfigurationWithClusterAndAuthentication() {
		List<String> clusterNodes = Arrays.asList("127.0.0.1:27379", "127.0.0.1:27380");
		this.contextRunner
			.withPropertyValues("spring.data.redis.username=user", "spring.data.redis.password=password",
					"spring.data.redis.cluster.nodes[0]:" + clusterNodes.get(0),
					"spring.data.redis.cluster.nodes[1]:" + clusterNodes.get(1))
			.run((context) -> {
				LettuceConnectionFactory connectionFactory = context.getBean(LettuceConnectionFactory.class);
				assertThat(getUserName(connectionFactory)).isEqualTo("user");
				assertThat(connectionFactory.getPassword()).isEqualTo("password");
			});
	}

	@Test
	void testRedisConfigurationWithMasterReplicaAndNoNode() {
		this.contextRunner.withPropertyValues("spring.data.redis.masterreplica.nodes=")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.hasMessage("At least one node is required for master-replica configuration"));
	}

	@Test
	void testRedisConfigurationWithMasterReplica() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.masterreplica.nodes=127.0.0.1:28319,127.0.0.1:28320,[::1]:28321")
			.run((context) -> {
				LettuceConnectionFactory connectionFactory = context.getBean(LettuceConnectionFactory.class);
				assertThat(connectionFactory.getSentinelConfiguration()).isNull();
				assertThat(connectionFactory.getClusterConfiguration()).isNull();
				assertThat(connectionFactory).extracting("configuration")
					.isInstanceOfSatisfying(RedisStaticMasterReplicaConfiguration.class,
							(masterReplicaConfiguration) -> assertThat(masterReplicaConfiguration.getNodes()
								.stream()
								.map((config) -> new RedisNode(config.getHostName(), config.getPort())))
								.containsExactly(new RedisNode("127.0.0.1", 28319), new RedisNode("127.0.0.1", 28320),
										new RedisNode("[::1]", 28321)));
			});
	}

	@Test
	void testRedisConfigurationWithMasterAndAuthentication() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.username=user", "spring.data.redis.password=password",
					"spring.data.redis.masterreplica.nodes=127.0.0.1:28319,127.0.0.1:28320")
			.run((context) -> {
				LettuceConnectionFactory connectionFactory = context.getBean(LettuceConnectionFactory.class);
				assertThat(getUserName(connectionFactory)).isEqualTo("user");
				assertThat(connectionFactory.getPassword()).isEqualTo("password");
				assertThat(connectionFactory).extracting("configuration")
					.isInstanceOf(RedisStaticMasterReplicaConfiguration.class);
			});
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
		this.contextRunner.withPropertyValues("spring.data.redis.cluster.nodes=127.0.0.1:27379,127.0.0.1:27380")
			.run(assertClientOptions(ClusterClientOptions.class, (options) -> {
				assertThat(options.getTimeoutOptions().isApplyConnectionTimeout()).isTrue();
				assertThat(options.getTimeoutOptions().isTimeoutCommands()).isTrue();
			}));
	}

	@Test
	void testRedisConfigurationWithClusterRefreshPeriod() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.cluster.nodes=127.0.0.1:27379,127.0.0.1:27380",
					"spring.data.redis.lettuce.cluster.refresh.period=30s")
			.run(assertClientOptions(ClusterClientOptions.class,
					(options) -> assertThat(options.getTopologyRefreshOptions().getRefreshPeriod()).hasSeconds(30)));
	}

	@Test
	void testRedisConfigurationWithClusterAdaptiveRefresh() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.cluster.nodes=127.0.0.1:27379,127.0.0.1:27380",
					"spring.data.redis.lettuce.cluster.refresh.adaptive=true")
			.run(assertClientOptions(ClusterClientOptions.class,
					(options) -> assertThat(options.getTopologyRefreshOptions().getAdaptiveRefreshTriggers())
						.isEqualTo(EnumSet.allOf(RefreshTrigger.class))));
	}

	@Test
	void testRedisConfigurationWithClusterRefreshPeriodHasNoEffectWithNonClusteredConfiguration() {
		this.contextRunner.withPropertyValues("spring.data.redis.cluster.refresh.period=30s")
			.run(assertClientOptions(ClientOptions.class,
					(options) -> assertThat(options.getClass()).isEqualTo(ClientOptions.class)));
	}

	@Test
	void testRedisConfigurationWithClusterDynamicRefreshSourcesEnabled() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.cluster.nodes=127.0.0.1:27379,127.0.0.1:27380",
					"spring.data.redis.lettuce.cluster.refresh.dynamic-refresh-sources=true")
			.run(assertClientOptions(ClusterClientOptions.class,
					(options) -> assertThat(options.getTopologyRefreshOptions().useDynamicRefreshSources()).isTrue()));
	}

	@Test
	void testRedisConfigurationWithClusterDynamicRefreshSourcesDisabled() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.cluster.nodes=127.0.0.1:27379,127.0.0.1:27380",
					"spring.data.redis.lettuce.cluster.refresh.dynamic-refresh-sources=false")
			.run(assertClientOptions(ClusterClientOptions.class,
					(options) -> assertThat(options.getTopologyRefreshOptions().useDynamicRefreshSources()).isFalse()));
	}

	@Test
	void testRedisConfigurationWithClusterDynamicSourcesUnspecifiedUsesDefault() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.cluster.nodes=127.0.0.1:27379,127.0.0.1:27380",
					"spring.data.redis.lettuce.cluster.refresh.dynamic-sources=")
			.run(assertClientOptions(ClusterClientOptions.class,
					(options) -> assertThat(options.getTopologyRefreshOptions().useDynamicRefreshSources()).isTrue()));
	}

	@Test
	void definesPropertiesBasedConnectionDetailsByDefault() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(PropertiesDataRedisConnectionDetails.class));
	}

	@Test
	void usesStandaloneFromCustomConnectionDetails() {
		this.contextRunner.withUserConfiguration(ConnectionDetailsStandaloneConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(DataRedisConnectionDetails.class)
				.doesNotHaveBean(PropertiesDataRedisConnectionDetails.class);
			LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
			assertThat(cf.isUseSsl()).isFalse();
			RedisStandaloneConfiguration configuration = cf.getStandaloneConfiguration();
			assertThat(configuration.getHostName()).isEqualTo("redis.example.com");
			assertThat(configuration.getPort()).isEqualTo(16379);
			assertThat(configuration.getDatabase()).isOne();
			assertThat(configuration.getUsername()).isEqualTo("user-1");
			assertThat(configuration.getPassword()).isEqualTo(RedisPassword.of("password-1"));
		});
	}

	@Test
	void usesSentinelFromCustomConnectionDetails() {
		this.contextRunner.withUserConfiguration(ConnectionDetailsSentinelConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(DataRedisConnectionDetails.class)
				.doesNotHaveBean(PropertiesDataRedisConnectionDetails.class);
			LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
			assertThat(cf.isUseSsl()).isFalse();
			RedisSentinelConfiguration configuration = cf.getSentinelConfiguration();
			assertThat(configuration).isNotNull();
			assertThat(configuration.getSentinelUsername()).isEqualTo("sentinel-1");
			assertThat(configuration.getSentinelPassword().get()).isEqualTo("secret-1".toCharArray());
			assertThat(configuration.getSentinels()).containsExactly(new RedisNode("node-1", 12345));
			assertThat(configuration.getUsername()).isEqualTo("user-1");
			assertThat(configuration.getPassword()).isEqualTo(RedisPassword.of("password-1"));
			assertThat(configuration.getDatabase()).isOne();
			NamedNode master = configuration.getMaster();
			assertThat(master).isNotNull();
			assertThat(master.getName()).isEqualTo("master.redis.example.com");
		});
	}

	@Test
	void usesClusterFromCustomConnectionDetails() {
		this.contextRunner.withUserConfiguration(ConnectionDetailsClusterConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(DataRedisConnectionDetails.class)
				.doesNotHaveBean(PropertiesDataRedisConnectionDetails.class);
			LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
			assertThat(cf.isUseSsl()).isFalse();
			RedisClusterConfiguration configuration = cf.getClusterConfiguration();
			assertThat(configuration).isNotNull();
			assertThat(configuration.getUsername()).isEqualTo("user-1");
			assertThat(configuration.getPassword().get()).isEqualTo("password-1".toCharArray());
			assertThat(configuration.getClusterNodes()).containsExactly(new RedisNode("node-1", 12345),
					new RedisNode("node-2", 23456));
		});
	}

	@Test
	void usesMasterReplicaFromCustomConnectionDetails() {
		this.contextRunner.withUserConfiguration(ConnectionDetailsMasterReplicaConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(DataRedisConnectionDetails.class)
				.doesNotHaveBean(PropertiesDataRedisConnectionDetails.class);
			LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
			assertThat(cf.isUseSsl()).isFalse();
			assertThat(cf).extracting("configuration")
				.isInstanceOfSatisfying(RedisStaticMasterReplicaConfiguration.class,
						(masterReplicationConfiguration) -> {
							assertThat(masterReplicationConfiguration.getUsername()).isEqualTo("user-1");
							assertThat(masterReplicationConfiguration.getPassword().get())
								.isEqualTo("password-1".toCharArray());
							assertThat(masterReplicationConfiguration.getNodes())
								.map((nodeConfiguration) -> new RedisNode(nodeConfiguration.getHostName(),
										nodeConfiguration.getPort()))
								.containsExactly(new RedisNode("node-1", 12345), new RedisNode("node-2", 23456));
						});
		});
	}

	@Test
	void testRedisConfigurationWithSslEnabled() {
		this.contextRunner.withPropertyValues("spring.data.redis.ssl.enabled:true").run((context) -> {
			LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
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
				LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
				assertThat(cf.isUseSsl()).isTrue();
			});
	}

	@Test
	void testRedisConfigurationWithSslDisabledBundle() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.ssl.enabled:false", "spring.data.redis.ssl.bundle:test-bundle")
			.run((context) -> {
				LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
				assertThat(cf.isUseSsl()).isFalse();
			});
	}

	@Test
	void shouldUsePlatformThreadsByDefault() {
		this.contextRunner.run((context) -> {
			LettuceConnectionFactory factory = context.getBean(LettuceConnectionFactory.class);
			assertThat(factory).extracting("executor").isNull();
		});
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void shouldUseVirtualThreadsIfEnabled() {
		this.contextRunner.withPropertyValues("spring.threads.virtual.enabled=true").run((context) -> {
			LettuceConnectionFactory factory = context.getBean(LettuceConnectionFactory.class);
			assertThat(factory).extracting("executor")
				.satisfies((executor) -> SimpleAsyncTaskExecutorAssert.assertThat((SimpleAsyncTaskExecutor) executor)
					.usesVirtualThreads());
		});
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

	private @Nullable String getUserName(LettuceConnectionFactory factory) {
		return ReflectionTestUtils.invokeMethod(factory, "getRedisUsername");
	}

	private RedisClusterNode createRedisNode(String host) {
		RedisClusterNode node = new RedisClusterNode();
		node.setUri(RedisURI.Builder.redis(host).build());
		return node;
	}

	private static final class RedisNodes implements Nodes {

		private final List<RedisNodeDescription> descriptions;

		RedisNodes(RedisNodeDescription... descriptions) {
			this.descriptions = List.of(descriptions);
		}

		@Override
		public List<RedisNodeDescription> getNodes() {
			return this.descriptions;
		}

		@Override
		public Iterator<RedisNodeDescription> iterator() {
			return this.descriptions.iterator();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConfiguration {

		@Bean
		LettuceClientConfigurationBuilderCustomizer customizer() {
			return LettuceClientConfigurationBuilder::useSsl;
		}

		@Bean
		LettuceClientOptionsBuilderCustomizer clientOptionsBuilderCustomizer() {
			return (builder) -> builder.autoReconnect(false);
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

	@Configuration(proxyBeanMethods = false)
	static class ConnectionDetailsStandaloneConfiguration {

		@Bean
		DataRedisConnectionDetails redisConnectionDetails() {
			return new DataRedisConnectionDetails() {

				@Override
				public String getUsername() {
					return "user-1";
				}

				@Override
				public String getPassword() {
					return "password-1";
				}

				@Override
				public Standalone getStandalone() {
					return new Standalone() {

						@Override
						public int getDatabase() {
							return 1;
						}

						@Override
						public String getHost() {
							return "redis.example.com";
						}

						@Override
						public int getPort() {
							return 16379;
						}

					};
				}

			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConnectionDetailsSentinelConfiguration {

		@Bean
		DataRedisConnectionDetails redisConnectionDetails() {
			return new DataRedisConnectionDetails() {

				@Override
				public String getUsername() {
					return "user-1";
				}

				@Override
				public String getPassword() {
					return "password-1";
				}

				@Override
				public Sentinel getSentinel() {
					return new Sentinel() {

						@Override
						public int getDatabase() {
							return 1;
						}

						@Override
						public String getMaster() {
							return "master.redis.example.com";
						}

						@Override
						public List<Node> getNodes() {
							return List.of(new Node("node-1", 12345));
						}

						@Override
						public String getUsername() {
							return "sentinel-1";
						}

						@Override
						public String getPassword() {
							return "secret-1";
						}

					};
				}

			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConnectionDetailsClusterConfiguration {

		@Bean
		DataRedisConnectionDetails redisConnectionDetails() {
			return new DataRedisConnectionDetails() {

				@Override
				public String getUsername() {
					return "user-1";
				}

				@Override
				public String getPassword() {
					return "password-1";
				}

				@Override
				public Cluster getCluster() {
					return new Cluster() {

						@Override
						public List<Node> getNodes() {
							return List.of(new Node("node-1", 12345), new Node("node-2", 23456));
						}

					};
				}

			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConnectionDetailsMasterReplicaConfiguration {

		@Bean
		DataRedisConnectionDetails redisConnectionDetails() {
			return new DataRedisConnectionDetails() {

				@Override
				public String getUsername() {
					return "user-1";
				}

				@Override
				public String getPassword() {
					return "password-1";
				}

				@Override
				public MasterReplica getMasterReplica() {
					return new MasterReplica() {

						@Override
						public List<Node> getNodes() {
							return List.of(new Node("node-1", 12345), new Node("node-2", 23456));
						}

					};
				}

			};
		}

	}

}
