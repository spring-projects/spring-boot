/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.testcontainers.properties;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.properties.TestcontainersPropertySourceAutoConfigurationWithSpringBootTestIntegrationTest.TestConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.test.context.DynamicPropertyRegistry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestcontainersPropertySourceAutoConfiguration} when combined with
 * {@link SpringBootTest @SpringBootTest}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@SpringBootTest(classes = TestConfig.class,
		properties = "spring.testcontainers.dynamic-property-registry-injection=allow")
class TestcontainersPropertySourceAutoConfigurationWithSpringBootTestIntegrationTest {

	@Autowired
	private Environment environment;

	@Test
	void injectsRegistryIntoBeanMethod() {
		assertThat(this.environment.getProperty("from.bean.method")).isEqualTo("one");
	}

	@Test
	void callsRegistrars() {
		assertThat(this.environment.getProperty("from.registrar")).isEqualTo("two");
	}

	@TestConfiguration
	@ImportAutoConfiguration(TestcontainersPropertySourceAutoConfiguration.class)
	@SpringBootConfiguration
	static class TestConfig {

		@Bean
		String example(DynamicPropertyRegistry registry) {
			registry.add("from.bean.method", () -> "one");
			return "Hello";
		}

		@Bean
		DynamicPropertyRegistrar propertyRegistrar() {
			return (registry) -> registry.add("from.registrar", () -> "two");
		}

	}

}
