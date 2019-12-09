/*
 * Copyright 2012-2019 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestDatabaseAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class TestDatabaseAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TestDatabaseAutoConfiguration.class));

	@Test
	void replaceWithNoDataSourceAvailable() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(DataSource.class));
	}

	@Test
	void replaceWithUniqueDatabase() {
		this.contextRunner.withUserConfiguration(ExistingDataSourceConfiguration.class).run((context) -> {
			DataSource datasource = context.getBean(DataSource.class);
			JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
			jdbcTemplate.execute("create table example (id int, name varchar);");
			this.contextRunner.withUserConfiguration(ExistingDataSourceConfiguration.class).run((secondContext) -> {
				DataSource anotherDatasource = secondContext.getBean(DataSource.class);
				JdbcTemplate anotherJdbcTemplate = new JdbcTemplate(anotherDatasource);
				anotherJdbcTemplate.execute("create table example (id int, name varchar);");
			});
		});
	}

	@Test
	void replaceUserConfiguredHsqlWithH2UsingDefaultUrlOptions() {
		this.contextRunner.withUserConfiguration(ExistingDataSourceConfiguration.class)
				.withPropertyValues("spring.test.database.connection=H2").run((context) -> {
					DataSource datasource = context.getBean(DataSource.class);
					SimpleDriverDataSource actualDatasource = datasource.unwrap(SimpleDriverDataSource.class);
					String url = actualDatasource.getUrl();
					assertThat(url).matches("jdbc:h2:mem:[a-zA-Z0-9-]*;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false");
				});
	}

	@Test
	void replaceUserConfiguredHsqlWithH2UsingCustomUrlOptions() {
		this.contextRunner.withUserConfiguration(ExistingDataSourceConfiguration.class)
				.withPropertyValues("spring.test.database.connection=H2", "spring.test.database.urlOptions=MODE=MySQL").run((context) -> {
			DataSource datasource = context.getBean(DataSource.class);
			SimpleDriverDataSource actualDatasource = datasource.unwrap(SimpleDriverDataSource.class);
			String url = actualDatasource.getUrl();
			assertThat(url).matches("jdbc:h2:mem:[a-zA-Z0-9-]*;MODE=MySQL");
		});
	}

	@Test
	void replaceAutoConfiguredH2WithHsqlUsingDefaultUrlOptions() {
		this.contextRunner.withPropertyValues("spring.test.database.connection=HSQL",
				"spring.test.database.replace=AUTO_CONFIGURED")
				.run((context) -> {
					DataSource datasource = context.getBean(DataSource.class);
					String url = datasource.getConnection().getMetaData().getURL();
					assertThat(url).matches("jdbc:hsqldb:mem:[a-zA-Z0-9-]*");
				});
	}

	@Test
	void replaceAutoConfiguredH2WithHsqlUsingCustomUrlOptions() {
		this.contextRunner.withPropertyValues("spring.test.database.connection=HSQL",
				"spring.test.database.replace=AUTO_CONFIGURED", "spring.test.database.urlOptions=MODE=MySQL")
				.run((context) -> {
					DataSource datasource = context.getBean(DataSource.class);
					String url = datasource.getConnection().getMetaData().getURL();
					assertThat(url).matches("jdbc:hsqldb:mem:[a-zA-Z0-9-]*;MODE=MySQL");
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class ExistingDataSourceConfiguration {

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.HSQL).build();
		}

	}

}
