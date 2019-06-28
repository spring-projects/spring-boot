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

package org.springframework.boot.autoconfigure.data.jdbc;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.data.empty.EmptyDataPackage;
import org.springframework.boot.autoconfigure.data.jdbc.city.City;
import org.springframework.boot.autoconfigure.data.jdbc.city.CityRepository;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.jdbc.repository.config.JdbcRepositoryConfigExtension;
import org.springframework.data.repository.Repository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JdbcRepositoriesAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class JdbcRepositoriesAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(JdbcRepositoriesAutoConfiguration.class));

	@Test
	void backsOffWithNoDataSource() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(JdbcRepositoryConfigExtension.class));
	}

	@Test
	void backsOffWithNoJdbcOperations() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class, TestConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(DataSource.class);
					assertThat(context).doesNotHaveBean(JdbcRepositoryConfigExtension.class);
				});
	}

	@Test
	void basicAutoConfiguration() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(JdbcTemplateAutoConfiguration.class, DataSourceAutoConfiguration.class))
				.withUserConfiguration(TestConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.datasource.schema=classpath:data-jdbc-schema.sql",
						"spring.datasource.data=classpath:city.sql", "spring.datasource.generate-unique-name:true")
				.run((context) -> {
					assertThat(context).hasSingleBean(AbstractJdbcConfiguration.class);
					assertThat(context).hasSingleBean(CityRepository.class);
					assertThat(context.getBean(CityRepository.class).findById(2000L)).isPresent();
				});
	}

	@Test
	void autoConfigurationWithNoRepositories() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JdbcTemplateAutoConfiguration.class))
				.withUserConfiguration(EmbeddedDataSourceConfiguration.class, EmptyConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(AbstractJdbcConfiguration.class);
					assertThat(context).doesNotHaveBean(Repository.class);
				});
	}

	@Test
	void honoursUsersEnableJdbcRepositoriesConfiguration() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(JdbcTemplateAutoConfiguration.class, DataSourceAutoConfiguration.class))
				.withUserConfiguration(EnableRepositoriesConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.datasource.schema=classpath:data-jdbc-schema.sql",
						"spring.datasource.data=classpath:city.sql", "spring.datasource.generate-unique-name:true")
				.run((context) -> {
					assertThat(context).hasSingleBean(AbstractJdbcConfiguration.class);
					assertThat(context).hasSingleBean(CityRepository.class);
					assertThat(context.getBean(CityRepository.class).findById(2000L)).isPresent();
				});
	}

	@TestAutoConfigurationPackage(City.class)
	private static class TestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	protected static class EmptyConfiguration {

	}

	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	@EnableJdbcRepositories(basePackageClasses = City.class)
	private static class EnableRepositoriesConfiguration {

	}

}
