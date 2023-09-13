/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.testcontainers.service.connection.r2dbc;

import java.time.Duration;

import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.junit.DisabledOnOs;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OracleR2dbcContainerConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
@DisabledOnOs(os = { OS.LINUX, OS.MAC }, architecture = "aarch64",
		disabledReason = "The Oracle image has no ARM support")
class OracleR2dbcContainerConnectionDetailsFactoryTests {

	@Container
	@ServiceConnection
	static final OracleContainer oracle = new OracleContainer(DockerImageNames.oracleXe())
		.withStartupTimeout(Duration.ofMinutes(2));

	@Autowired
	ConnectionFactory connectionFactory;

	@Test
	void connectionCanBeMadeToOracleContainer() {
		Object result = DatabaseClient.create(this.connectionFactory)
			.sql(DatabaseDriver.ORACLE.getValidationQuery())
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
