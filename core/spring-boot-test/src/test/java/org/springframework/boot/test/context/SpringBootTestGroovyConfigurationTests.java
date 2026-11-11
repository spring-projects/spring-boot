/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} (detectDefaultConfigurationClasses).
 *
 * @author Dave Syer
 */
@DirtiesContext
@SpringBootTest
@ContextConfiguration(locations = "classpath:test.groovy")
class SpringBootTestGroovyConfigurationTests {

	@Autowired
	private String foo;

	@Test
	void groovyConfigLoaded() {
		assertThat(this.foo).isNotNull();
	}

}
