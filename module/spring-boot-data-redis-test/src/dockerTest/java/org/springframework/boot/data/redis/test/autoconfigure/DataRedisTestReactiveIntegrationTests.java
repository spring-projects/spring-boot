/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.test.autoconfigure;

import java.time.Duration;
import java.util.UUID;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.ReactiveRedisOperations;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration test for {@link DataRedisTest @DataRedisTest} using reactive operations.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@Testcontainers(disabledWithoutDocker = true)
@DataRedisTest
class DataRedisTestReactiveIntegrationTests {

	@Container
	@ServiceConnection
	static RedisContainer redis = TestImage.forContainer(RedisContainer.class);

	@Autowired
	private ReactiveRedisOperations<Object, Object> operations;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testRepository() {
		String id = UUID.randomUUID().toString();
		StepVerifier.create(this.operations.opsForValue().set(id, "Hello World"))
			.expectNext(Boolean.TRUE)
			.expectComplete()
			.verify(Duration.ofSeconds(30));
		StepVerifier.create(this.operations.opsForValue().get(id))
			.expectNext("Hello World")
			.expectComplete()
			.verify(Duration.ofSeconds(30));
		StepVerifier.create(this.operations.execute((action) -> action.serverCommands().flushDb()))
			.expectNext("OK")
			.expectComplete()
			.verify(Duration.ofSeconds(30));
	}

	@Test
	void didNotInjectExampleService() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(ExampleService.class));
	}

}
