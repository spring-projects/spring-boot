/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.testcontainers;

import java.time.Duration;

import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.boot.testsupport.junit.DisabledOnOs;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OracleXeR2dbcContainerConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
@DisabledOnOs(os = { OS.LINUX, OS.MAC }, architecture = "aarch64",
		disabledReason = "The Oracle image has no ARM support")
class OracleXeR2dbcContainerConnectionDetailsFactoryIntegrationTests {

	@Container
	@ServiceConnection
	static final OracleContainer oracle = TestImage.forContainer(OracleContainer.class);

	@Autowired
	ConnectionFactory connectionFactory;

	@Test
	void connectionCanBeMadeToOracleContainer() {
		String validationQuery = DatabaseDriver.ORACLE.getValidationQuery();
		assertThat(validationQuery).isNotNull();
		Object result = DatabaseClient.create(this.connectionFactory)
			.sql(validationQuery)
			.map((row, metadata) -> row.get(0))
			.first()
			.block(Duration.ofSeconds(30));
		assertThat(result).isEqualTo("Hello");
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(R2dbcAutoConfiguration.class)
	static class TestConfiguration {

	}

}
