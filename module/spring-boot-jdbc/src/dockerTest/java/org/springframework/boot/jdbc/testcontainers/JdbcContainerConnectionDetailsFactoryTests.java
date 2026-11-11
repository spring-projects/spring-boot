/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.testcontainers;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link JdbcContainerConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class JdbcContainerConnectionDetailsFactoryTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = TestImage.forContainer(PostgreSQLContainer.class);

	@Autowired(required = false)
	private JdbcConnectionDetails connectionDetails;

	@Autowired
	private DataSource dataSource;

	@Test
	void connectionCanBeMadeToJdbcContainer() {
		assertThat(this.connectionDetails).isNotNull();
		JdbcTemplate jdbc = new JdbcTemplate(this.dataSource);
		String validationQuery = DatabaseDriver.POSTGRESQL.getValidationQuery();
		assertThat(validationQuery).isNotNull();
		assertThatNoException().isThrownBy(() -> jdbc.execute(validationQuery));
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(DataSourceAutoConfiguration.class)
	static class TestConfiguration {

	}

}
