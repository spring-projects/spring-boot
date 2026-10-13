/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.session.redis;

import com.redis.testcontainers.RedisContainer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;

public class TestServiceConnectionImportSampleSessionRedisApplication {

	public static void main(String[] args) {
		SpringApplication.from(SampleSessionRedisApplication::main).with(ContainerConfiguration.class).run(args);
	}

	@ImportTestcontainers
	static class ContainerConfiguration {

		@ServiceConnection // We don't need a name here because we have the container
		static RedisContainer redisContainer = TestImage.forContainer(RedisContainer.class);

	}

}
