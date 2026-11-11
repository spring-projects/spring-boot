/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.test.autoconfigure;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link AutoConfigureTestDatabase} with Testcontainers and a
 * {@link ServiceConnection @ServiceConnection}.
 *
 * @author Phillip Webb
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NON_TEST)
@Testcontainers(disabledWithoutDocker = true)
@OverrideAutoConfiguration(enabled = false)
class AutoConfigureTestDatabaseServiceConnectionIntegrationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = TestImage.forContainer(PostgreSQLContainer.class);

	@Autowired
	private DataSource dataSource;

	@Test
	void dataSourceIsNotReplaced() {
		assertThat(this.dataSource).isInstanceOf(HikariDataSource.class).isNotInstanceOf(EmbeddedDatabase.class);
	}

	@Configuration
	@ImportAutoConfiguration(DataSourceAutoConfiguration.class)
	static class Config {

	}

}
