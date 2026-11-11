/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} (detectDefaultConfigurationClasses).
 *
 * @author Dave Syer
 */
@ExtendWith(SpringExtension.class)
@DirtiesContext
class SpringBootTestDefaultConfigurationTests {

	@Autowired
	private Config config;

	@Test
	void nestedConfigClasses() {
		assertThat(this.config).isNotNull();
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
