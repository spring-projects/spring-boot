/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SpringBootTestContextBootstrapper} (in its own package so
 * we can test detection).
 *
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
class SpringBootTestContextBootstrapperIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private SpringBootTestContextBootstrapperExampleConfig config;

	@Test
	void findConfigAutomatically() {
		assertThat(this.config).isNotNull();
	}

	@Test
	void contextWasCreatedViaSpringApplication() {
		assertThat(this.context.getId()).startsWith("application");
	}

	@Test
	void testConfigurationWasApplied() {
		assertThat(this.context.getBean(ExampleBean.class)).isNotNull();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestConfig {

		@Bean
		ExampleBean exampleBean() {
			return new ExampleBean();
		}

	}

	static class ExampleBean {

	}

}
