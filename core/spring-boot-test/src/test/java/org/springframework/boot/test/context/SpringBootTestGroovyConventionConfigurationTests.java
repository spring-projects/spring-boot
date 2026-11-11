/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} finding groovy config.
 *
 * @author Phillip Webb
 */
@SpringBootTest
@DirtiesContext
class SpringBootTestGroovyConventionConfigurationTests {

	@Autowired
	private String foo;

	@Test
	void groovyConfigLoaded() {
		assertThat(this.foo).isEqualTo("World");
	}

}
