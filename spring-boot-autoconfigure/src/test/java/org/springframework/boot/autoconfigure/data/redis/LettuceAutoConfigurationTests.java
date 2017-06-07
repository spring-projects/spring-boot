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
import org.junit.runner.RunWith;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.junit.runner.classpath.ClassPathExclusions;
import org.springframework.boot.junit.runner.classpath.ModifiedClassPathRunner;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link LettuceConnectionConfiguration} without pooling.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("commons-pool2-*.jar")
public class LettuceAutoConfigurationTests {

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
		assertThat(this.context.getBean("redisConnectionFactory",
				LettuceConnectionFactory.class)).isNotNull();
	}

	@Test
	public void testOverrideRedisConfiguration() {
		load("spring.redis.host:foo", "spring.redis.database:1");
		LettuceConnectionFactory cf = this.context
				.getBean(LettuceConnectionFactory.class);
		assertThat(cf.getHostName()).isEqualTo("foo");
		assertThat(cf.getDatabase()).isEqualTo(1);
		assertThat(cf.getPassword()).isNull();
		assertThat(cf.isUseSsl()).isFalse();
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
	public void testRedisConfigurationWithPoolingConfigured() throws Exception {
		load("spring.redis.lettuce.pool.min-idle:1");
		assertThat(this.context.getBeanNamesForType(LettuceConnectionFactory.class))
				.isEmpty();
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
	public void testRedisConfigurationWithCluster() throws Exception {
		List<String> clusterNodes = Arrays.asList("127.0.0.1:27379", "127.0.0.1:27380");
		load("spring.redis.cluster.nodes[0]:" + clusterNodes.get(0),
				"spring.redis.cluster.nodes[1]:" + clusterNodes.get(1));
		assertThat(this.context.getBean(LettuceConnectionFactory.class)
				.getClusterConnection()).isNotNull();
	}

	private void load(String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(ctx, environment);
		ctx.register(EnableRedisProperties.class);
		ctx.register(LettuceConnectionConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@EnableConfigurationProperties(RedisProperties.class)
	static class EnableRedisProperties {

	}

}
