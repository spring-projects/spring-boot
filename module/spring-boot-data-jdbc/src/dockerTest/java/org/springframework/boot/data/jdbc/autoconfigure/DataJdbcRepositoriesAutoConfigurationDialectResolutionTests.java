/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.jdbc.autoconfigure;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.mysql.MySQLContainer;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.data.jdbc.domain.city.City;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.data.jdbc.core.dialect.JdbcMySqlDialect;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.dialect.MariaDbDialect;
import org.springframework.util.function.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataJdbcRepositoriesAutoConfiguration} when the configured dialect
 * requires resolution using a database connection.
 *
 * @author Andy Wilkinson
 */
@Testcontainers(disabledWithoutDocker = true)
class DataJdbcRepositoriesAutoConfigurationDialectResolutionTests {

	@Test
	void resolvesMariaDbDialect() {
		withContainer(MariaDBContainer.class, (runner) -> {
			runner.withPropertyValues("spring.data.jdbc.dialect=maria").run((context) -> {
				Dialect dialect = context.getBean(Dialect.class);
				assertThat(dialect).isInstanceOf(MariaDbDialect.class);
			});
		});
	}

	@Test
	void resolvesMySqlDialect() {
		withContainer(MySQLContainer.class, (runner) -> {
			runner.withPropertyValues("spring.data.jdbc.dialect=mysql").run((context) -> {
				Dialect dialect = context.getBean(Dialect.class);
				assertThat(dialect).isInstanceOf(JdbcMySqlDialect.class);
			});
		});
	}

	private <C extends JdbcDatabaseContainer<?>> void withContainer(Class<C> containerType,
			ThrowingConsumer<ApplicationContextRunner> callback) {
		C container = TestImage.container(containerType);
		try {
			container.start();
			ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
						DataJdbcRepositoriesAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
						DataSourceTransactionManagerAutoConfiguration.class))
				.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.datasource.url=" + container.getJdbcUrl(),
						"spring.datasource.username=" + container.getUsername(),
						"spring.datasource.password=" + container.getPassword());
			callback.accept(contextRunner);
		}
		finally {
			container.close();
		}
	}

	@TestAutoConfigurationPackage(City.class)
	static class TestConfiguration {

	}

}
