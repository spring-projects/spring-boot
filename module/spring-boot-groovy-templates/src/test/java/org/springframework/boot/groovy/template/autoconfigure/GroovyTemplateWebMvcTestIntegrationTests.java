/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.groovy.template.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Integration tests for Groovy Templates with {@link WebMvcTest @WebMvcTest}.
 *
 * @author Andy Wilkinson
 */
@WebMvcTest
class GroovyTemplateWebMvcTestIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void groovyTemplateAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(GroovyTemplateAutoConfiguration.class));
	}

	@SpringBootConfiguration
	static class TestConfiguration {

	}

}
