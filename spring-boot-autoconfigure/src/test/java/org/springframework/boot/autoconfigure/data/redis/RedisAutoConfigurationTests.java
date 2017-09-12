/*
 * Copyright 2012-2017 the original author or authors.
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

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
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
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
public class RedisAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDefaultRedisConfiguration() {
		load();
		assertThat(this.context.getBean("redisTemplate", RedisOperations.class))
				.isNotNull();
		assertThat(this.context.getBean(StringRedisTemplate.class)).isNotNull();
	}

	@Test
	public void testOverrideRedisConfiguration() {
		load("spring.redis.host:foo", "spring.redis.database:1",
				"spring.redis.lettuce.shutdown-timeout:500");
		LettuceConnectionFactory cf = this.context
				.getBean(LettuceConnectionFactory.class);
		assertThat(cf.getHostName()).isEqualTo("foo");
		assertThat(cf.getDatabase()).isEqualTo(1);
		assertThat(cf.getPassword()).isNull();
		assertThat(cf.isUseSsl()).isFalse();
		assertThat(cf.getShutdownTimeout()).isEqualTo(500);
	}

	@Test
	public void testCustomizeRedisConfiguration() {
		load(CustomConfiguration.class);
		LettuceConnectionFactory cf = this.context
				.getBean(LettuceConnectionFactory.class);
		assertThat(cf.isUseSsl()).isTrue();
	}

	@Test
	public void testRedisUrlConfiguration() throws Exception {
		load("spring.redis.host:foo",
				"spring.redis.url:redis://user:password@example:33");
		LettuceConnectionFactory cf = this.context
				.getBean(LettuceConnectionFactory.class);
		assertThat(cf.getHostName()).isEqualTo("example");
		assertThat(cf.getPort()).isEqualTo(33);
		assertThat(cf.getPassword()).isEqualTo("password");
		assertThat(cf.isUseSsl()).isFalse();
	}

	@Test
	public void testOverrideUrlRedisConfiguration() {
		load("spring.redis.host:foo", "spring.redis.password:xyz",
				"spring.redis.port:1000", "spring.redis.ssl:false",
				"spring.redis.url:rediss://user:password@example:33");
		LettuceConnectionFactory cf = this.context
				.getBean(LettuceConnectionFactory.class);
		assertThat(cf.getHostName()).isEqualTo("example");
		assertThat(cf.getPort()).isEqualTo(33);
		assertThat(cf.getPassword()).isEqualTo("password");
		assertThat(cf.isUseSsl()).isTrue();
	}

	@Test
	public void testRedisConfigurationWithPool() throws Exception {
		load("spring.redis.host:foo", "spring.redis.lettuce.pool.min-idle:1",
				"spring.redis.lettuce.pool.max-idle:4",
				"spring.redis.lettuce.pool.max-active:16",
				"spring.redis.lettuce.pool.max-wait:2000",
				"spring.redis.lettuce.shutdown-timeout:1000");
		LettuceConnectionFactory cf = this.context
				.getBean(LettuceConnectionFactory.class);
		assertThat(cf.getHostName()).isEqualTo("foo");
		assertThat(getPoolingClientConfiguration(cf).getPoolConfig().getMinIdle())
				.isEqualTo(1);
		assertThat(getPoolingClientConfiguration(cf).getPoolConfig().getMaxIdle())
				.isEqualTo(4);
		assertThat(getPoolingClientConfiguration(cf).getPoolConfig().getMaxTotal())
				.isEqualTo(16);
		assertThat(getPoolingClientConfiguration(cf).getPoolConfig().getMaxWaitMillis())
				.isEqualTo(2000);
		assertThat(cf.getShutdownTimeout()).isEqualTo(1000);
	}

	@Test
	public void testRedisConfigurationWithTimeout() throws Exception {
		load("spring.redis.host:foo", "spring.redis.timeout:100");
		LettuceConnectionFactory cf = this.context
				.getBean(LettuceConnectionFactory.class);
		assertThat(cf.getHostName()).isEqualTo("foo");
		assertThat(cf.getTimeout()).isEqualTo(100);
	}

	@Test
	public void testRedisConfigurationWithSentinel() throws Exception {
		List<String> sentinels = Arrays.asList("127.0.0.1:26379", "127.0.0.1:26380");
		load("spring.redis.sentinel.master:mymaster", "spring.redis.sentinel.nodes:"
				+ StringUtils.collectionToCommaDelimitedString(sentinels));
		assertThat(this.context.getBean(LettuceConnectionFactory.class)
				.isRedisSentinelAware()).isTrue();
	}

	@Test
	public void testRedisConfigurationWithSentinelAndPassword() throws Exception {
		List<String> sentinels = Arrays.asList("127.0.0.1:26379", "127.0.0.1:26380");
		load("spring.redis.password=password", "spring.redis.sentinel.master:mymaster",
				"spring.redis.sentinel.nodes:"
						+ StringUtils.collectionToCommaDelimitedString(sentinels));
		assertThat(this.context.getBean(LettuceConnectionFactory.class).getPassword())
				.isEqualTo("password");
	}

	@Test
	public void testRedisConfigurationWithCluster() throws Exception {
		List<String> clusterNodes = Arrays.asList("127.0.0.1:27379", "127.0.0.1:27380");
		load("spring.redis.cluster.nodes[0]:" + clusterNodes.get(0),
				"spring.redis.cluster.nodes[1]:" + clusterNodes.get(1));
		assertThat(this.context.getBean(LettuceConnectionFactory.class)
				.getClusterConnection()).isNotNull();
	}

	@Test
	public void testRedisConfigurationWithClusterAndPassword() throws Exception {
		List<String> clusterNodes = Arrays.asList("127.0.0.1:27379", "127.0.0.1:27380");
		load("spring.redis.password=password",
				"spring.redis.cluster.nodes[0]:" + clusterNodes.get(0),
				"spring.redis.cluster.nodes[1]:" + clusterNodes.get(1));
		assertThat(this.context.getBean(LettuceConnectionFactory.class).getPassword())
				.isEqualTo("password");
	}

	private LettucePoolingClientConfiguration getPoolingClientConfiguration(
			LettuceConnectionFactory factory) {
		return (LettucePoolingClientConfiguration) ReflectionTestUtils.getField(factory,
				"clientConfiguration");
	}

	private void load(String... environment) {
		load(null, environment);
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(environment).applyTo(ctx);
		if (config != null) {
			ctx.register(config);
		}
		ctx.register(RedisAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@Configuration
	static class CustomConfiguration {

		@Bean
		LettuceClientConfigurationBuilderCustomizer customizer() {
			return LettuceClientConfigurationBuilder::useSsl;
		}

	}

}
