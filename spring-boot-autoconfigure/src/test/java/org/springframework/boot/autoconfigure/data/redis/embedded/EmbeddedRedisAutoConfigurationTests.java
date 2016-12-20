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

package org.springframework.boot.autoconfigure.data.redis.embedded;

import java.io.File;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmbeddedRedisAutoConfiguration}.
 *
 * @author Alexey Zhokhov
 */
public class EmbeddedRedisAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultVersion() {
		assertVersionConfiguration(null, "3.2.1");
	}

	@Test
	public void randomlyAllocatedPortIsAvailableWhenCreatingRedisClient() {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, "spring.redis.port=0");
		this.context.register(EmbeddedRedisAutoConfiguration.class,
				JedisClientConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(Jedis.class).getClient().getPort())
				.isEqualTo(Integer.valueOf(
						this.context.getEnvironment().getProperty("local.redis.port")));
	}

	@Test
	public void portIsAvailableInParentContext() {
		ConfigurableApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.refresh();
		try {
			this.context = new AnnotationConfigApplicationContext();
			this.context.setParent(parent);
			EnvironmentTestUtils.addEnvironment(this.context, "spring.redis.port=0");
			this.context.register(EmbeddedRedisAutoConfiguration.class,
					JedisClientConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class);
			this.context.refresh();
			assertThat(parent.getEnvironment().getProperty("local.redis.port"))
					.isNotNull();
		}
		finally {
			parent.close();
		}
	}

	@Test
	public void redisWritesToCustomDatabaseDir() {
		File customDatabaseDir = new File("target/custom-redis-database-dir");
		FileSystemUtils.deleteRecursively(customDatabaseDir);
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, "spring.redis.port=0",
				"spring.redis.embedded.storage.databaseDir="
						+ customDatabaseDir.getPath());
		this.context.register(EmbeddedRedisAutoConfiguration.class,
				JedisClientConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(customDatabaseDir).isDirectory();
	}

	private void assertVersionConfiguration(String configuredVersion,
			String expectedVersion) {
		this.context = new AnnotationConfigApplicationContext();
		int redisPort = SocketUtils.findAvailableTcpPort();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.redis.port=" + redisPort);
		if (configuredVersion != null) {
			EnvironmentTestUtils.addEnvironment(this.context,
					"spring.redis.embedded.version=" + configuredVersion);
		}
		this.context.register(RedisAutoConfiguration.class,
				EmbeddedRedisAutoConfiguration.class);
		this.context.refresh();
		RedisTemplate<String, String> redis = (RedisTemplate<String, String>) this.context
				.getBean("redisTemplate");
		String redisVersion = redis.execute(new RedisCallback<String>() {
			@Override
			public String doInRedis(RedisConnection redisConnection)
					throws DataAccessException {
				return redisConnection.info("server").getProperty("redis_version");
			}
		});
		assertThat(redisVersion).isEqualTo(expectedVersion);
	}

	@Configuration
	static class JedisClientConfiguration {

		@Bean
		public Jedis jedis(@Value("${local.redis.port}") int port)
				throws UnknownHostException {
			return new Jedis("localhost", port);
		}

	}

}
