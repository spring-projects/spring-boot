/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.jdbc;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabaseTestcontainersJdbcUrlIntegrationTests.InitializeDatasourceUrl;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link AutoConfigureTestDatabase} with Testcontainers and a
 * {@link ServiceConnection @ServiceConnection}.
 *
 * @author Phillip Webb
 */
@SpringBootTest
@ContextConfiguration(initializers = InitializeDatasourceUrl.class)
@AutoConfigureTestDatabase(replace = Replace.NON_TEST)
@Testcontainers(disabledWithoutDocker = true)
@OverrideAutoConfiguration(enabled = false)
class AutoConfigureTestDatabaseTestcontainersJdbcUrlIntegrationTests {

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

	static class InitializeDatasourceUrl implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
					"spring.datasource.url=jdbc:tc:postgis:" + TestImage.POSTGRESQL.getTag() + ":///");
		}

	}

}
