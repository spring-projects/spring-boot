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

package org.springframework.boot.autoconfigure.data.jdbc;

import java.util.function.Function;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.data.empty.EmptyDataPackage;
import org.springframework.boot.autoconfigure.data.jdbc.city.City;
import org.springframework.boot.autoconfigure.data.jdbc.city.CityRepository;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ManagedTypes;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.RelationalManagedTypes;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.repository.Repository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JdbcRepositoriesAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Mark Paluch
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
			.withUserConfiguration(TestConfiguration.class)
			.run((context) -> {
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
			.withUserConfiguration(TestConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(AbstractJdbcConfiguration.class);
				assertThat(context).hasSingleBean(CityRepository.class);
				assertThat(context.getBean(CityRepository.class).findById(2000L)).isPresent();
			});
	}

	@Test
	void entityScanShouldSetManagedTypes() {
		this.contextRunner.with(database())
			.withConfiguration(AutoConfigurations.of(JdbcTemplateAutoConfiguration.class,
					DataSourceTransactionManagerAutoConfiguration.class))
			.withUserConfiguration(TestConfiguration.class)
			.run((context) -> {
				JdbcMappingContext mappingContext = context.getBean(JdbcMappingContext.class);
				ManagedTypes managedTypes = (ManagedTypes) ReflectionTestUtils.getField(mappingContext, "managedTypes");
				assertThat(managedTypes.toList()).containsOnly(City.class);
			});
	}

	@Test
	void autoConfigurationWithNoRepositories() {
		this.contextRunner.with(database())
			.withConfiguration(AutoConfigurations.of(JdbcTemplateAutoConfiguration.class,
					DataSourceTransactionManagerAutoConfiguration.class))
			.withUserConfiguration(EmptyConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(AbstractJdbcConfiguration.class);
				assertThat(context).doesNotHaveBean(Repository.class);
			});
	}

	@Test
	void honoursUsersEnableJdbcRepositoriesConfiguration() {
		this.contextRunner.with(database())
			.withConfiguration(AutoConfigurations.of(JdbcTemplateAutoConfiguration.class,
					DataSourceTransactionManagerAutoConfiguration.class))
			.withUserConfiguration(EnableRepositoriesConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(AbstractJdbcConfiguration.class);
				assertThat(context).hasSingleBean(CityRepository.class);
				assertThat(context.getBean(CityRepository.class).findById(2000L)).isPresent();
			});
	}

	@Test
	void allowsUserToDefineCustomRelationalManagedTypes() {
		allowsUserToDefineCustomBean(RelationalManagedTypesConfiguration.class, RelationalManagedTypes.class,
				"customRelationalManagedTypes");
	}

	@Test
	void allowsUserToDefineCustomJdbcMappingContext() {
		allowsUserToDefineCustomBean(JdbcMappingContextConfiguration.class, JdbcMappingContext.class,
				"customJdbcMappingContext");
	}

	@Test
	void allowsUserToDefineCustomJdbcConverter() {
		allowsUserToDefineCustomBean(JdbcConverterConfiguration.class, JdbcConverter.class, "customJdbcConverter");
	}

	@Test
	void allowsUserToDefineCustomJdbcCustomConversions() {
		allowsUserToDefineCustomBean(JdbcCustomConversionsConfiguration.class, JdbcCustomConversions.class,
				"customJdbcCustomConversions");
	}

	@Test
	void allowsUserToDefineCustomJdbcAggregateTemplate() {
		allowsUserToDefineCustomBean(JdbcAggregateTemplateConfiguration.class, JdbcAggregateTemplate.class,
				"customJdbcAggregateTemplate");
	}

	@Test
	void allowsUserToDefineCustomDataAccessStrategy() {
		allowsUserToDefineCustomBean(DataAccessStrategyConfiguration.class, DataAccessStrategy.class,
				"customDataAccessStrategy");
	}

	@Test
	void allowsUserToDefineCustomDialect() {
		allowsUserToDefineCustomBean(DialectConfiguration.class, Dialect.class, "customDialect");
	}

	private void allowsUserToDefineCustomBean(Class<?> configuration, Class<?> beanType, String beanName) {
		this.contextRunner.with(database())
			.withConfiguration(AutoConfigurations.of(JdbcTemplateAutoConfiguration.class,
					DataSourceTransactionManagerAutoConfiguration.class))
			.withUserConfiguration(configuration, EmptyConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(beanType);
				assertThat(context).hasBean(beanName);
			});
	}

	private Function<ApplicationContextRunner, ApplicationContextRunner> database() {
		return (runner) -> runner
			.withConfiguration(
					AutoConfigurations.of(DataSourceAutoConfiguration.class, SqlInitializationAutoConfiguration.class))
			.withPropertyValues("spring.sql.init.schema-locations=classpath:data-city-schema.sql",
					"spring.sql.init.data-locations=classpath:city.sql", "spring.datasource.generate-unique-name:true");
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

	@Configuration(proxyBeanMethods = false)
	static class RelationalManagedTypesConfiguration {

		@Bean
		RelationalManagedTypes customRelationalManagedTypes() {
			return RelationalManagedTypes.empty();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JdbcMappingContextConfiguration {

		@Bean
		JdbcMappingContext customJdbcMappingContext() {
			return mock(JdbcMappingContext.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JdbcConverterConfiguration {

		@Bean
		JdbcConverter customJdbcConverter() {
			return mock(JdbcConverter.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JdbcCustomConversionsConfiguration {

		@Bean
		JdbcCustomConversions customJdbcCustomConversions() {
			return mock(JdbcCustomConversions.class, Answers.RETURNS_MOCKS);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JdbcAggregateTemplateConfiguration {

		@Bean
		JdbcAggregateTemplate customJdbcAggregateTemplate() {
			return mock(JdbcAggregateTemplate.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DataAccessStrategyConfiguration {

		@Bean
		DataAccessStrategy customDataAccessStrategy() {
			return mock(DataAccessStrategy.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DialectConfiguration {

		@Bean
		Dialect customDialect() {
			return mock(Dialect.class, Answers.RETURNS_MOCKS);
		}

	}

}
