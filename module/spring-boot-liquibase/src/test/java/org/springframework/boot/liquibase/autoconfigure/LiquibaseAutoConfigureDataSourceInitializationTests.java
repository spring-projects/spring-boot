/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.liquibase.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureDataSourceInitialization;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Tests for Liquibase using
 * {@link AutoConfigureDataSourceInitialization @AutoConfigureDataSourceInitialization}.
 *
 * @author Andy Wilkinson
 */
@SpringBootTest
@AutoConfigureDataSourceInitialization
class LiquibaseAutoConfigureDataSourceInitializationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void liquibaseIsIncludedInAutoConfigureDataSourceInitialization() {
		assertThat(this.context).has(importedAutoConfiguration(LiquibaseAutoConfiguration.class));
	}

	@SpringBootConfiguration
	static class TestConfiguration {

	}

}
