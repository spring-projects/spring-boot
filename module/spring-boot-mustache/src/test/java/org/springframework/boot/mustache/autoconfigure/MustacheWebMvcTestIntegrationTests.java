/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mustache.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Integration tests for Mustache with {@link WebMvcTest @WebMvcTest}.
 *
 * @author Andy Wilkinson
 */
@WebMvcTest
class MustacheWebMvcTestIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void mustacheAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(MustacheAutoConfiguration.class));
	}

	@SpringBootConfiguration
	static class TestConfiguration {

	}

}
