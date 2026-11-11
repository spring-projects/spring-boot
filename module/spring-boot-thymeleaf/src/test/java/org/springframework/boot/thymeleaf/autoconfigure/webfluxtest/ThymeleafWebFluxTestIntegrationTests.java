/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.thymeleaf.autoconfigure.webfluxtest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Integration tests for Thymeleaf with {@link WebFluxTest @WebFluxTest}.
 *
 * @author Andy Wilkinson
 */
@WebFluxTest
class ThymeleafWebFluxTestIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void thymeleafAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(ThymeleafAutoConfiguration.class));
	}

	@Test
	void idialectIsIncluded() {
		assertThatNoException().isThrownBy(() -> this.applicationContext.getBean(ExampleDialect.class));
	}

}
