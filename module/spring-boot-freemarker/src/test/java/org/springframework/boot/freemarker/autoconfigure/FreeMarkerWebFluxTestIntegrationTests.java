/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.freemarker.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Integration tests for FreeMarker with {@link WebFluxTest @WebFluxTest}.
 *
 * @author Andy Wilkinson
 */
@WebFluxTest
class FreeMarkerWebFluxTestIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void freemarkerAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(FreeMarkerAutoConfiguration.class));
	}

	@SpringBootConfiguration
	static class TestConfiguration {

	}

}
