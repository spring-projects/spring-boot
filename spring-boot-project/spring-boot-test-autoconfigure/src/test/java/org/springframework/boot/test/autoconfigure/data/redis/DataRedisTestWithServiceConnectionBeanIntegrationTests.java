/*
 * Copyright 2012-2023 the original author or authors.
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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.testcontainers.RedisContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisOperations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link DataRedisTest @DataRedisTest}.
 *
 * @author Jayaram Pradhan
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Eddú Meléndez
 */
@Testcontainers(disabledWithoutDocker = true)
@DataRedisTest
class DataRedisTestWithServiceConnectionBeanIntegrationTests {

	private static final Charset CHARSET = StandardCharsets.UTF_8;

	@Autowired
	private RedisOperations<Object, Object> operations;

	@Autowired
	private ExampleRepository exampleRepository;

	@Test
	void testRepository() {
		PersonHash personHash = new PersonHash();
		personHash.setDescription("Look, new @DataRedisTest!");
		assertThat(personHash.getId()).isNull();
		PersonHash savedEntity = this.exampleRepository.save(personHash);
		assertThat(savedEntity.getId()).isNotNull();
		assertThat(this.operations
			.execute((org.springframework.data.redis.connection.RedisConnection connection) -> connection.keyCommands()
				.exists(("persons:" + savedEntity.getId()).getBytes(CHARSET))))
			.isTrue();
		this.exampleRepository.deleteAll();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class ContainerConfig {

		@Bean
		@ServiceConnection
		RedisContainer redis() {
			return new RedisContainer();
		}

	}

}
