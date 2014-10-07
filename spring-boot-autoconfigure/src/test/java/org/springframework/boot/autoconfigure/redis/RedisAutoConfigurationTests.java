/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.redis;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import redis.clients.jedis.Jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link RedisAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Christoph Strobl
 */
public class RedisAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Test
	public void testDefaultRedisConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(RedisAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean("redisTemplate", RedisOperations.class));
		assertNotNull(this.context.getBean(StringRedisTemplate.class));
	}

	@Test
	public void testOverrideRedisConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, "spring.redis.host:foo");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.redis.database:1");
		this.context.register(RedisAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertEquals("foo", this.context.getBean(JedisConnectionFactory.class)
				.getHostName());
		assertEquals(1, this.context.getBean(JedisConnectionFactory.class).getDatabase());
	}

	@Test
	public void testRedisConfigurationWithPool() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, "spring.redis.host:foo");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.redis.pool.max-idle:1");
		this.context.register(RedisAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertEquals("foo", this.context.getBean(JedisConnectionFactory.class)
				.getHostName());
		assertEquals(1, this.context.getBean(JedisConnectionFactory.class)
				.getPoolConfig().getMaxIdle());
	}

	@Test
	public void testRedisConfigurationWithSentinel() throws Exception {
		List<String> sentinels = Arrays.asList("127.0.0.1:26379", "127.0.0.1:26380");

		if (isAtLeastOneSentinelAvailable(sentinels)) {
			this.context = new AnnotationConfigApplicationContext();
			EnvironmentTestUtils.addEnvironment(this.context,
					"spring.redis.sentinel.master:mymaster");
			EnvironmentTestUtils.addEnvironment(
					this.context,
					"spring.redis.sentinel.nodes:"
							+ StringUtils.collectionToCommaDelimitedString(sentinels));
			this.context.register(RedisAutoConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class);
			this.context.refresh();

			assertTrue(this.context.getBean(JedisConnectionFactory.class)
					.isRedisSentinelAware());
		}
	}

	private boolean isAtLeastOneSentinelAvailable(List<String> sentinels) {
		for (String sentinel : sentinels) {
			if (isSentinelAvailable(sentinel)) {
				return true;
			}
		}

		return false;
	}

	private boolean isSentinelAvailable(String node) {
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

}
