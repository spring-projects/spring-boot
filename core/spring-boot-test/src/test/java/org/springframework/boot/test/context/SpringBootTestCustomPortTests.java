/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link SpringBootTest @SpringBootTest} with a custom inline server.port in a
 * non-embedded web environment.
 *
 * @author Stephane Nicoll
 */
@SpringBootTest(properties = "server.port=12345")
class SpringBootTestCustomPortTests {

	@Autowired
	private Environment environment;

	@Test
	void validatePortIsNotOverwritten() {
		String port = this.environment.getProperty("server.port");
		assertThat(port).isEqualTo("12345");
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
