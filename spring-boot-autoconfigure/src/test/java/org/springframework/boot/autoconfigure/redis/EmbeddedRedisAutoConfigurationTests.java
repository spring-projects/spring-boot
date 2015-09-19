/*
 * Copyright 2012-2015 the original author or authors.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.SocketUtils;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link EmbeddedRedisAutoConfiguration}.
 *
 * @author Amer Aljovic
 */
public class EmbeddedRedisAutoConfigurationTests {

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
		int redisPort = SocketUtils.findAvailableTcpPort();
		EnvironmentTestUtils.addEnvironment(this.context, "spring.redis.port=" + redisPort);
		this.context.register(RedisAutoConfiguration.class, EmbeddedRedisAutoConfiguration.class);
		this.context.refresh();
		StringRedisTemplate redis = this.context.getBean(StringRedisTemplate.class);
		RedisConnection connection = redis.getConnectionFactory().getConnection();
		assertNotNull(connection);
	}

}
