/*
 * Copyright 2012-2018 the original author or authors.
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
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RedisAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Christoph Strobl
 * @author Eddú Meléndez
 * @author Marco Aust
 */
public class RedisAutoConfigurationTests {

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
	public void testDefaultRedisConfiguration() throws Exception {
		load();
		assertThat(this.context.getBean("redisTemplate", RedisOperations.class))
				.isNotNull();
		assertThat(this.context.getBean(StringRedisTemplate.class)).isNotNull();
	}

	@Test
	public void testOverrideRedisConfiguration() throws Exception {
		load("spring.redis.host:foo", "spring.redis.database:1");
		assertThat(this.context.getBean(JedisConnectionFactory.class).getHostName())
				.isEqualTo("foo");
		assertThat(this.context.getBean(JedisConnectionFactory.class).getDatabase())
				.isEqualTo(1);
	}

	@Test
	public void testOverrideUrlRedisConfiguration() throws Exception {
		load("spring.redis.host:foo", "spring.redis.password:xyz",
				"spring.redis.port:1000", "spring.redis.ssl:true",
				"spring.redis.url:redis://user:password@example:33");
		assertThat(this.context.getBean(JedisConnectionFactory.class).getHostName())
				.isEqualTo("example");
		assertThat(this.context.getBean(JedisConnectionFactory.class).getPort())
				.isEqualTo(33);
		assertThat(this.context.getBean(JedisConnectionFactory.class).getPassword())
				.isEqualTo("password");
		assertThat(this.context.getBean(JedisConnectionFactory.class).isUseSsl())
				.isEqualTo(true);
	}

	@Test
	public void testPasswordInUrlWithColon() {
		load("spring.redis.url:redis://:pass:word@example:33");
		assertThat(this.context.getBean(JedisConnectionFactory.class).getHostName())
				.isEqualTo("example");
		assertThat(this.context.getBean(JedisConnectionFactory.class).getPort())
				.isEqualTo(33);
		assertThat(this.context.getBean(JedisConnectionFactory.class).getPassword())
				.isEqualTo("pass:word");
	}

	@Test
	public void testPasswordInUrlStartsWithColon() {
		load("spring.redis.url:redis://user::pass:word@example:33");
		assertThat(this.context.getBean(JedisConnectionFactory.class).getHostName())
				.isEqualTo("example");
		assertThat(this.context.getBean(JedisConnectionFactory.class).getPort())
				.isEqualTo(33);
		assertThat(this.context.getBean(JedisConnectionFactory.class).getPassword())
				.isEqualTo(":pass:word");
	}

	@Test
	public void testRedisConfigurationWithPool() throws Exception {
		load("spring.redis.host:foo", "spring.redis.pool.max-idle:1");
		assertThat(this.context.getBean(JedisConnectionFactory.class).getHostName())
				.isEqualTo("foo");
		assertThat(this.context.getBean(JedisConnectionFactory.class).getPoolConfig()
				.getMaxIdle()).isEqualTo(1);
	}

	@Test
	public void testRedisConfigurationWithTimeout() throws Exception {
		load("spring.redis.host:foo", "spring.redis.timeout:100");
		assertThat(this.context.getBean(JedisConnectionFactory.class).getHostName())
				.isEqualTo("foo");
		assertThat(this.context.getBean(JedisConnectionFactory.class).getTimeout())
				.isEqualTo(100);
	}

	@Test
	public void testRedisConfigurationWithSentinel() throws Exception {
		List<String> sentinels = Arrays.asList("127.0.0.1:26379", "127.0.0.1:26380");
		if (isAtLeastOneNodeAvailable(sentinels)) {
			load("spring.redis.sentinel.master:mymaster", "spring.redis.sentinel.nodes:"
					+ StringUtils.collectionToCommaDelimitedString(sentinels));
			assertThat(this.context.getBean(JedisConnectionFactory.class)
					.isRedisSentinelAware()).isTrue();
		}
	}

	@Test
	public void testRedisConfigurationWithCluster() throws Exception {
		List<String> clusterNodes = Arrays.asList("127.0.0.1:27379", "127.0.0.1:27380");
		if (isAtLeastOneNodeAvailable(clusterNodes)) {
			load("spring.redis.cluster.nodes[0]:" + clusterNodes.get(0),
					"spring.redis.cluster.nodes[1]:" + clusterNodes.get(1));
			assertThat(this.context.getBean(JedisConnectionFactory.class)
					.getClusterConnection()).isNotNull();
		}
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
		Jedis jedis = null;
		try {
			String[] hostAndPort = node.split(":");
			jedis = new Jedis(hostAndPort[0], Integer.valueOf(hostAndPort[1]));
			jedis.connect();
			jedis.ping();
			return true;
		}
		catch (Exception ex) {
			return false;
		}
		finally {
			if (jedis != null) {
				try {
					jedis.disconnect();
					jedis.close();
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
		applicationContext.register(RedisAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		applicationContext.refresh();
		return applicationContext;
	}

}
