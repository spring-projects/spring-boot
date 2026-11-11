/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.test.autoconfigure;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AutoConfigureTestDatabase @AutoConfigureTestDatabase} when
 * there is no database.
 *
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension.class)
@AutoConfigureTestDatabase
class AutoConfigureTestDatabaseWithNoDatabaseIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void testContextLoads() {
		// gh-6897
		assertThat(this.context).isNotNull();
		assertThat(this.context.getBeanNamesForType(DataSource.class)).isNotEmpty();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class Config {

	}

}
