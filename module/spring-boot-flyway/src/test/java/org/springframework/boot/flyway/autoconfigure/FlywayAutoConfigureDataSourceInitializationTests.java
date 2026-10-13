/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.flyway.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureDataSourceInitialization;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Tests for Flyway using
 * {@link AutoConfigureDataSourceInitialization @AutoConfigureDataSourceInitialization}.
 *
 * @author Andy Wilkinson
 */
@SpringBootTest
@AutoConfigureDataSourceInitialization
class FlywayAutoConfigureDataSourceInitializationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void flywayIsIncludedInAutoConfigureDataSourceInitialization() {
		assertThat(this.context).has(importedAutoConfiguration(FlywayAutoConfiguration.class));
	}

	@SpringBootConfiguration
	static class TestConfiguration {

	}

}
