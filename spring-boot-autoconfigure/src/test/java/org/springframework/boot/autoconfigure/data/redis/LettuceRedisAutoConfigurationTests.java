/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.redis;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.connection.lettuce.DefaultLettucePool;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link RedisAutoConfiguration} using Lettuce as client.
 *
 * @author Mark Paluch
 */
public class LettuceRedisAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Before
	public void setup() {
		this.context = new AnnotationConfigApplicationContext();
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testOverrideRedisConfiguration() throws Exception {
		load("spring.redis.host:foo", "spring.redis.database:1");
		assertThat(this.context.getBean(LettuceConnectionFactory.class).getHostName())
				.isEqualTo("foo");
		assertThat(this.context.getBean(LettuceConnectionFactory.class).getDatabase())
				.isEqualTo(1);
	}

	@Test
	public void testOverrideUrlRedisConfiguration() throws Exception {
		load("spring.redis.host:foo", "spring.redis.password:xyz",
				"spring.redis.port:1000", "spring.redis.ssl:true",
				"spring.redis.url:redis://user:password@example:33");
		assertThat(this.context.getBean(LettuceConnectionFactory.class).getHostName())
				.isEqualTo("example");
		assertThat(this.context.getBean(LettuceConnectionFactory.class).getPort())
				.isEqualTo(33);
		assertThat(this.context.getBean(LettuceConnectionFactory.class).getPassword())
				.isEqualTo("password");
		assertThat(this.context.getBean(LettuceConnectionFactory.class).isUseSsl())
				.isEqualTo(true);
	}

	@Test
	public void testSslRedisConfiguration() throws Exception {
		load("spring.redis.host:foo", "spring.redis.ssl:true");
		assertThat(this.context.getBean(LettuceConnectionFactory.class).getHostName())
				.isEqualTo("foo");
		assertThat(this.context.getBean(LettuceConnectionFactory.class).isUseSsl())
				.isTrue();
	}

	@Test
	public void testRedisConfigurationWithPool() throws Exception {
		load("spring.redis.host:foo", "spring.redis.lettuce.pool.max-idle:1");
		assertThat(this.context.getBean(LettuceConnectionFactory.class).getHostName())
				.isEqualTo("foo");
		assertThat(getDefaultLettucePool(
				this.context.getBean(LettuceConnectionFactory.class)).getHostName())
						.isEqualTo("foo");
		assertThat(getDefaultLettucePool(
				this.context.getBean(LettuceConnectionFactory.class)).getPoolConfig()
						.getMaxIdle()).isEqualTo(1);
	}

	@Test
	public void testRedisConfigurationWithTimeout() throws Exception {
		load("spring.redis.host:foo", "spring.redis.timeout:100");
		assertThat(this.context.getBean(LettuceConnectionFactory.class).getHostName())
				.isEqualTo("foo");
		assertThat(this.context.getBean(LettuceConnectionFactory.class).getTimeout())
				.isEqualTo(100);
	}

	@Test
	public void testRedisConfigurationWithSentinel() throws Exception {
		List<String> sentinels = Arrays.asList("127.0.0.1:26379", "127.0.0.1:26380");
		if (isAtLeastOneNodeAvailable(sentinels)) {
			load("spring.redis.sentinel.master:mymaster", "spring.redis.sentinel.nodes:"
					+ StringUtils.collectionToCommaDelimitedString(sentinels));
			assertThat(this.context.getBean(LettuceConnectionFactory.class)
					.isRedisSentinelAware()).isTrue();
		}
	}

	@Test
	public void testRedisConfigurationWithCluster() throws Exception {
		List<String> clusterNodes = Arrays.asList("127.0.0.1:27379", "127.0.0.1:27380");
		load("spring.redis.cluster.nodes[0]:" + clusterNodes.get(0),
				"spring.redis.cluster.nodes[1]:" + clusterNodes.get(1));
		assertThat(this.context.getBean(LettuceConnectionFactory.class)
				.getClusterConnection()).isNotNull();
	}

	private DefaultLettucePool getDefaultLettucePool(LettuceConnectionFactory factory) {
		return (DefaultLettucePool) ReflectionTestUtils.getField(factory, "pool");
	}

	private boolean isAtLeastOneNodeAvailable(List<String> nodes) {
		for (String node : nodes) {
			if (isAvailable(node)) {
				return true;
			}
		}

		return false;
	}

	private boolean isAvailable(String node) {
		RedisClient redisClient = null;
		try {
			String[] hostAndPort = node.split(":");
			redisClient = RedisClient.create(new RedisURI(hostAndPort[0],
					Integer.valueOf(hostAndPort[1]), 10, TimeUnit.SECONDS));
			StatefulRedisConnection<String, String> connection = redisClient.connect();
			connection.sync().ping();
			connection.close();
			return true;
		}
		catch (Exception ex) {
			return false;
		}
		finally {
			if (redisClient != null) {
				try {
					redisClient.shutdown(0, 0, TimeUnit.SECONDS);
				}
				catch (Exception ex) {
					// Continue
				}
			}
		}
	}

	private void load(String... environment) {
		this.context = doLoad(environment);
	}

	private AnnotationConfigApplicationContext doLoad(String... environment) {

		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.register(
				RedisAutoConfiguration.LettuceRedisConnectionConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				EnableRedisPropertiesConfiguration.class);
		applicationContext.refresh();
		return applicationContext;
	}

	@EnableConfigurationProperties(RedisProperties.class)
	private static class EnableRedisPropertiesConfiguration {

	}

}
