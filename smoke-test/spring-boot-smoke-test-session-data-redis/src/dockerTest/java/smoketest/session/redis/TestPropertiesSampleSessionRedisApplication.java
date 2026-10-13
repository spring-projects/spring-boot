/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.session.redis;

import com.redis.testcontainers.RedisContainer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;

public class TestPropertiesSampleSessionRedisApplication {

	public static void main(String[] args) {
		SpringApplication.from(SampleSessionRedisApplication::main).with(ContainerConfiguration.class).run(args);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class ContainerConfiguration {

		@Bean
		RedisContainer redisContainer(DynamicPropertyRegistry properties) {
			RedisContainer container = TestImage.forContainer(RedisContainer.class);
			properties.add("spring.data.redis.host", container::getHost);
			properties.add("spring.data.redis.port", container::getFirstMappedPort);
			return container;
		}

	}

}
