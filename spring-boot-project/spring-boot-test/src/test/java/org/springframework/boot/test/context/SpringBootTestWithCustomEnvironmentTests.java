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

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SpringBootTest @SpringBootTest} with a custom
 * {@link Environment}.
 *
 * @author Madhura Bhave
 */
@SpringBootTest
@ActiveProfiles({ "test1", "test2" })
@ContextConfiguration(loader = SpringBootTestWithCustomEnvironmentTests.Loader.class)
class SpringBootTestWithCustomEnvironmentTests {

	@Autowired
	private Environment environment;

	@Test
	void getActiveProfiles() {
		assertThat(this.environment).isInstanceOf(MockEnvironment.class);
		assertThat(this.environment.getActiveProfiles()).containsOnly("test1", "test2");
	}

	@Configuration
	static class Config {

	}

	static class Loader extends SpringBootContextLoader {

		@Override
		protected ConfigurableEnvironment getEnvironment() {
			return new MockEnvironment();
		}

	}

}
