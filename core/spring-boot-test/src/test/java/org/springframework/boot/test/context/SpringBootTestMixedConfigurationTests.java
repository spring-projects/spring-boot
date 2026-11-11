/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTestMixedConfigurationTests.Config;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest}.
 *
 * @author Dave Syer
 */
@DirtiesContext
@SpringBootTest
@ContextConfiguration(classes = Config.class, locations = "classpath:test.groovy")
class SpringBootTestMixedConfigurationTests {

	@Autowired
	private String foo;

	@Autowired
	private Config config;

	@Test
	void mixedConfigClasses() {
		assertThat(this.foo).isNotNull();
		assertThat(this.config).isNotNull();
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
