/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.test.autoconfigure;

import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizationAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Integration tests for {@link JooqTest @JooqTest}.
 *
 * @author Michael Simons
 */
@JooqTest
@TestPropertySource(properties = { "spring.flyway.enabled=false", "spring.liquibase.enabled=false" })
class JooqTestIntegrationTests {

	@Autowired
	private DSLContext dsl;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testDSLContext() {
		assertThat(this.dsl.selectCount().from("INFORMATION_SCHEMA.TABLES").fetchOne(0, Integer.class))
			.isGreaterThan(0);
	}

	@Test
	void useDefinedDataSource() throws Exception {
		String product = this.dataSource.getConnection().getMetaData().getDatabaseProductName();
		assertThat(product).startsWith("HSQL");
		assertThat(this.dsl.configuration().dialect()).isEqualTo(SQLDialect.HSQLDB);
	}

	@Test
	void didNotInjectExampleComponent() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(ExampleComponent.class));
	}

	@Test
	void flywayAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(FlywayAutoConfiguration.class));
	}

	@Test
	void liquibaseAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(LiquibaseAutoConfiguration.class));
	}

	@Test
	void serviceConnectionAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(ServiceConnectionAutoConfiguration.class));
	}

	@Test
	void transactionAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(TransactionAutoConfiguration.class));
		assertThat(this.applicationContext)
			.has(importedAutoConfiguration(TransactionManagerCustomizationAutoConfiguration.class));
	}

}
