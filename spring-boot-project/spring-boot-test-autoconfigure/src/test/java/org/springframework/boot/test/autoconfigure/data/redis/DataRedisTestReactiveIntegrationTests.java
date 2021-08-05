/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.data.redis;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testsupport.testcontainers.RedisContainer;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration test for {@link DataRedisTest @DataRedisTest} using reactive operations.
 *
 * @author Stephane Nicoll
 */
@Testcontainers(disabledWithoutDocker = true)
@DataRedisTest
class DataRedisTestReactiveIntegrationTests {

	@Container
	static RedisContainer redis = new RedisContainer();

	@Autowired
	private ReactiveRedisOperations<Object, Object> operations;

	@Autowired
	private ApplicationContext applicationContext;

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.redis.host", redis::getHost);
		registry.add("spring.redis.port", redis::getFirstMappedPort);
	}

	@Test
	void testRepository() {
		String id = UUID.randomUUID().toString();
		StepVerifier.create(this.operations.opsForValue().set(id, "Hello World")).expectNext(Boolean.TRUE)
				.verifyComplete();
		StepVerifier.create(this.operations.opsForValue().get(id)).expectNext("Hello World").verifyComplete();
		StepVerifier.create(this.operations.execute((action) -> action.serverCommands().flushDb())).expectNext("OK")
				.verifyComplete();
	}

	@Test
	void didNotInjectExampleService() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.applicationContext.getBean(ExampleService.class));
	}

}
