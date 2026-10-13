/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mustache.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Integration tests for Mustache with {@link WebFluxTest @WebFluxTest}.
 *
 * @author Andy Wilkinson
 */
@WebFluxTest
class MustacheWebFluxTestIntegrationTests {

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
