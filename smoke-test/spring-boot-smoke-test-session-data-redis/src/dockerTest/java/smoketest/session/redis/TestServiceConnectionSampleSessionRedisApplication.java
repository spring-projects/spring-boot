/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.session.redis;

import com.redis.testcontainers.RedisContainer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Bean;

public class TestServiceConnectionSampleSessionRedisApplication {

	public static void main(String[] args) {
		SpringApplication.from(SampleSessionRedisApplication::main).with(ContainerConfiguration.class).run(args);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class ContainerConfiguration {

		@Bean
		@ServiceConnection
		RedisContainer redisContainer() {
			return TestImage.forContainer(RedisContainer.class);
		}

	}

}
