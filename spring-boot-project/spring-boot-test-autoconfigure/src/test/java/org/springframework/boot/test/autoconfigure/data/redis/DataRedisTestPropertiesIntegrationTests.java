/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.autoconfigure.data.redis;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.testcontainers.RedisContainer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link DataRedisTest#properties properties} attribute of
 * {@link DataRedisTest @DataRedisTest}.
 *
 * @author Artsiom Yudovin
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = DataRedisTestPropertiesIntegrationTests.Initializer.class)
@DataRedisTest(properties = "spring.profiles.active=test")
public class DataRedisTestPropertiesIntegrationTests {

	@ClassRule
	public static RedisContainer redis = new RedisContainer();

	@Autowired
	private Environment environment;

	@Test
	public void environmentWithNewProfile() {
		assertThat(this.environment.getActiveProfiles()).containsExactly("test");
	}

	static class Initializer
			implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(
				ConfigurableApplicationContext configurableApplicationContext) {
			TestPropertyValues.of("spring.redis.port=" + redis.getMappedPort())
					.applyTo(configurableApplicationContext.getEnvironment());
		}

	}

}
