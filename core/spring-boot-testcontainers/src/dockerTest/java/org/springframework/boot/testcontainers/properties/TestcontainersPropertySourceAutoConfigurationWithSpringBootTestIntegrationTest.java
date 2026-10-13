/*
 * Copyright 2012-present the original author or authors.
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
	void callsRegistrars() {
		assertThat(this.environment.getProperty("from.registrar")).isEqualTo("two");
	}

	@TestConfiguration
	@ImportAutoConfiguration(TestcontainersPropertySourceAutoConfiguration.class)
	@SpringBootConfiguration
	static class TestConfig {

		@Bean
		DynamicPropertyRegistrar propertyRegistrar() {
			return (registry) -> registry.add("from.registrar", () -> "two");
		}

	}

}
