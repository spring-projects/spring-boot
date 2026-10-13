/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.testcontainers;

import com.redis.testcontainers.RedisStackContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
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
 * @author Eddú Meléndez
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class RedisStackContainerConnectionDetailsFactoryTests {

	@Container
	@ServiceConnection
	static final RedisStackContainer redis = TestImage.forContainer(RedisStackContainer.class);

	@Autowired(required = false)
	private DataRedisConnectionDetails connectionDetails;

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
	@ImportAutoConfiguration(DataRedisAutoConfiguration.class)
	static class TestConfiguration {

	}

}
