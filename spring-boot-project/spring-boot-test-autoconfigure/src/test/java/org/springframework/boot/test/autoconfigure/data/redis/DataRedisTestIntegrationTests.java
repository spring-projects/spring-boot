/*
 * Copyright 2012-2019 the original author or authors.
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

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.testcontainers.RedisContainer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link DataRedisTest}.
 *
 * @author Jayaram Pradhan
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = DataRedisTestIntegrationTests.Initializer.class)
@DataRedisTest
public class DataRedisTestIntegrationTests {

	@ClassRule
	public static RedisContainer redis = new RedisContainer();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Autowired
	private RedisOperations<Object, Object> operations;

	@Autowired
	private ExampleRepository exampleRepository;

	@Autowired
	private ApplicationContext applicationContext;

	private static final Charset CHARSET = StandardCharsets.UTF_8;

	@Test
	public void testRepository() {
		PersonHash personHash = new PersonHash();
		personHash.setDescription("Look, new @DataRedisTest!");
		assertThat(personHash.getId()).isNull();
		PersonHash savedEntity = this.exampleRepository.save(personHash);
		assertThat(savedEntity.getId()).isNotNull();
		assertThat(this.operations.execute((RedisConnection connection) -> connection
				.exists(("persons:" + savedEntity.getId()).getBytes(CHARSET)))).isTrue();
		this.exampleRepository.deleteAll();
	}

	@Test
	public void didNotInjectExampleService() {
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.applicationContext.getBean(ExampleService.class);
	}

	static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
			TestPropertyValues.of("spring.redis.port=" + redis.getMappedPort())
					.applyTo(configurableApplicationContext.getEnvironment());
		}

	}

}
