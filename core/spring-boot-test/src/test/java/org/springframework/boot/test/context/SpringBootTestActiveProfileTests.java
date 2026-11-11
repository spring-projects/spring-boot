/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} with active profiles. See gh-1469.
 *
 * @author Phillip Webb
 */
@DirtiesContext
@SpringBootTest("spring.config.name=enableother")
@ActiveProfiles("override")
class SpringBootTestActiveProfileTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void profiles() {
		assertThat(this.context.getEnvironment().getActiveProfiles()).containsExactly("override");
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
