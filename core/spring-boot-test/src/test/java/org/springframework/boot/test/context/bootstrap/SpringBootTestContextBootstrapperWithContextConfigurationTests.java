/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTestContextBootstrapper} + {@code @ContextConfiguration} (in
 * its own package so we can test detection).
 *
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
@ContextConfiguration
class SpringBootTestContextBootstrapperWithContextConfigurationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private SpringBootTestContextBootstrapperExampleConfig config;

	@Test
	void findConfigAutomatically() {
		assertThat(this.config).isNotNull();
	}

	@Test
	void contextWasCreatedViaSpringApplication() {
		assertThat(this.context.getId()).startsWith("application");
	}

}
