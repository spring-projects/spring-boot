/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.testcontainers.service.connection.redis;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RedisContainerConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class RedisContainerConnectionDetailsFactoryTests {

	@Container
	@ServiceConnection
	static final RedisContainer redis = TestImage.container(RedisContainer.class);

	@Autowired(required = false)
	private RedisConnectionDetails connectionDetails;

	@Autowired
	private RedisConnectionFactory connectionFactory;

	@Test
	void connectionCanBeMadeToRedisContainer() {
		assertThat(this.connectionDetails).isNotNull();
		try (RedisConnection connection = this.connectionFactory.getConnection()) {
			assertThat(connection.commands().echo("Hello, World".getBytes())).isEqualTo("Hello, World".getBytes());
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(RedisAutoConfiguration.class)
	static class TestConfiguration {

	}

}
