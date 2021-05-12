/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.function.Function;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.data.empty.EmptyDataPackage;
import org.springframework.boot.autoconfigure.data.jdbc.city.City;
import org.springframework.boot.autoconfigure.data.jdbc.city.CityRepository;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.repository.Repository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JdbcRepositoriesAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
class JdbcRepositoriesAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(JdbcRepositoriesAutoConfiguration.class));

	@Test
	void backsOffWithNoDataSource() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(AbstractJdbcConfiguration.class));
	}

	@Test
	void backsOffWithNoJdbcOperations() {
		this.contextRunner.with(database()).withUserConfiguration(TestConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(DataSource.class);
			assertThat(context).doesNotHaveBean(AbstractJdbcConfiguration.class);
		});
	}

	@Test
	void backsOffWithNoTransactionManager() {
		this.contextRunner.with(database())
				.withConfiguration(AutoConfigurations.of(JdbcTemplateAutoConfiguration.class))
				.withUserConfiguration(TestConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(DataSource.class);
					assertThat(context).hasSingleBean(NamedParameterJdbcOperations.class);
					assertThat(context).doesNotHaveBean(AbstractJdbcConfiguration.class);
				});
	}

	@Test
	void basicAutoConfiguration() {
		this.contextRunner.with(database())
				.withConfiguration(AutoConfigurations.of(JdbcTemplateAutoConfiguration.class,
						DataSourceTransactionManagerAutoConfiguration.class))
				.withUserConfiguration(TestConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(AbstractJdbcConfiguration.class);
					assertThat(context).hasSingleBean(CityRepository.class);
					assertThat(context.getBean(CityRepository.class).findById(2000L)).isPresent();
				});
	}

	@Test
	void autoConfigurationWithNoRepositories() {
		this.contextRunner.with(database())
				.withConfiguration(AutoConfigurations.of(JdbcTemplateAutoConfiguration.class,
						DataSourceTransactionManagerAutoConfiguration.class))
				.withUserConfiguration(EmptyConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(AbstractJdbcConfiguration.class);
					assertThat(context).doesNotHaveBean(Repository.class);
				});
	}

	@Test
	void honoursUsersEnableJdbcRepositoriesConfiguration() {
		this.contextRunner.with(database())
				.withConfiguration(AutoConfigurations.of(JdbcTemplateAutoConfiguration.class,
						DataSourceTransactionManagerAutoConfiguration.class))
				.withUserConfiguration(EnableRepositoriesConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(AbstractJdbcConfiguration.class);
					assertThat(context).hasSingleBean(CityRepository.class);
					assertThat(context.getBean(CityRepository.class).findById(2000L)).isPresent();
				});
	}

	private Function<ApplicationContextRunner, ApplicationContextRunner> database() {
		return (runner) -> runner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
				.withPropertyValues("spring.datasource.schema=classpath:data-city-schema.sql",
						"spring.datasource.data=classpath:city.sql", "spring.datasource.generate-unique-name:true");
	}

	@TestAutoConfigurationPackage(City.class)
	static class TestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	static class EmptyConfiguration {

	}

	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	@EnableJdbcRepositories(basePackageClasses = City.class)
	static class EnableRepositoriesConfiguration {

	}

}
