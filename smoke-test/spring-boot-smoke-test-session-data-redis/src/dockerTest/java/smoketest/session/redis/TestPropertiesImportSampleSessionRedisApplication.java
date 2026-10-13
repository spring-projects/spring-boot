/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.session.redis;

import com.redis.testcontainers.RedisContainer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public class TestPropertiesImportSampleSessionRedisApplication {

	public static void main(String[] args) {
		SpringApplication.from(SampleSessionRedisApplication::main).with(ContainerConfiguration.class).run(args);
	}

	@ImportTestcontainers
	static class ContainerConfiguration {

		static RedisContainer container = TestImage.forContainer(RedisContainer.class);

		@DynamicPropertySource
		static void containerProperties(DynamicPropertyRegistry properties) {
			properties.add("spring.data.redis.host", container::getHost);
			properties.add("spring.data.redis.port", container::getFirstMappedPort);
		}

	}

}
