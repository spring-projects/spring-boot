/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.boot.webflux.autoconfigure.error.ErrorWebFluxAutoConfiguration;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Tests for the auto-configuration imported by {@link WebFluxTest @WebFluxTest}.
 *
 * @author Stephane Nicoll
 * @author Artsiom Yudovin
 * @author Ali Dehghani
 * @author Madhura Bhave
 */
@WebFluxTest
class WebFluxTestAutoConfigurationIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void messageSourceAutoConfigurationIsImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(MessageSourceAutoConfiguration.class));
	}

	@Test
	void validationAutoConfigurationIsImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(ValidationAutoConfiguration.class));
	}

	@Test
	void errorWebFluxAutoConfigurationIsImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(ErrorWebFluxAutoConfiguration.class));
	}

}
