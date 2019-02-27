/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.jdbc;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JdbcTemplateAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @author Dan Zheng
 */
public class JdbcTemplateAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.datasource.initialization-mode=never",
					"spring.datasource.generate-unique-name=true")
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					JdbcTemplateAutoConfiguration.class));

	@Test
	public void testJdbcTemplateExists() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(JdbcOperations.class);
			JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
			assertThat(jdbcTemplate.getDataSource())
					.isEqualTo(context.getBean(DataSource.class));
			assertThat(jdbcTemplate.getFetchSize()).isEqualTo(-1);
			assertThat(jdbcTemplate.getQueryTimeout()).isEqualTo(-1);
			assertThat(jdbcTemplate.getMaxRows()).isEqualTo(-1);
		});
	}

	@Test
	public void testJdbcTemplateWithCustomProperties() {
		this.contextRunner.withPropertyValues("spring.jdbc.template.fetch-size:100",
				"spring.jdbc.template.query-timeout:60",
				"spring.jdbc.template.max-rows:1000").run((context) -> {
					assertThat(context).hasSingleBean(JdbcOperations.class);
					JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
					assertThat(jdbcTemplate.getDataSource()).isNotNull();
					assertThat(jdbcTemplate.getFetchSize()).isEqualTo(100);
					assertThat(jdbcTemplate.getQueryTimeout()).isEqualTo(60);
					assertThat(jdbcTemplate.getMaxRows()).isEqualTo(1000);
				});
	}

	@Test
	public void testJdbcTemplateExistsWithCustomDataSource() {
		this.contextRunner.withUserConfiguration(TestDataSourceConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(JdbcOperations.class);
					JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
					assertThat(jdbcTemplate.getDataSource())
							.isEqualTo(context.getBean("customDataSource"));
				});
	}

	@Test
	public void testNamedParameterJdbcTemplateExists() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(NamedParameterJdbcOperations.class);
			NamedParameterJdbcTemplate namedParameterJdbcTemplate = context
					.getBean(NamedParameterJdbcTemplate.class);
			assertThat(namedParameterJdbcTemplate.getJdbcOperations())
					.isEqualTo(context.getBean(JdbcOperations.class));
		});
	}

	@Test
	public void testMultiDataSource() {
		this.contextRunner.withUserConfiguration(MultiDataSourceConfiguration.class)
				.run((context) -> {
					assertThat(context).doesNotHaveBean(JdbcOperations.class);
					assertThat(context)
							.doesNotHaveBean(NamedParameterJdbcOperations.class);
				});
	}

	@Test
	public void testMultiJdbcTemplate() {
		this.contextRunner.withUserConfiguration(MultiJdbcTemplateConfiguration.class)
				.run((context) -> assertThat(context)
						.doesNotHaveBean(NamedParameterJdbcOperations.class));
	}

	@Test
	public void testMultiDataSourceUsingPrimary() {
		this.contextRunner
				.withUserConfiguration(MultiDataSourceUsingPrimaryConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(JdbcOperations.class);
					assertThat(context).hasSingleBean(NamedParameterJdbcOperations.class);
					assertThat(context.getBean(JdbcTemplate.class).getDataSource())
							.isEqualTo(context.getBean("test1DataSource"));
				});
	}

	@Test
	public void testMultiJdbcTemplateUsingPrimary() {
		this.contextRunner
				.withUserConfiguration(MultiJdbcTemplateUsingPrimaryConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(NamedParameterJdbcOperations.class);
					assertThat(context.getBean(NamedParameterJdbcTemplate.class)
							.getJdbcOperations())
									.isEqualTo(context.getBean("test1Template"));
				});
	}

	@Test
	public void testExistingCustomJdbcTemplate() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(JdbcOperations.class);
					assertThat(context.getBean(JdbcOperations.class))
							.isEqualTo(context.getBean("customJdbcOperations"));
				});
	}

	@Test
	public void testExistingCustomNamedParameterJdbcTemplate() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(NamedParameterJdbcOperations.class);
					assertThat(context.getBean(NamedParameterJdbcOperations.class))
							.isEqualTo(context
									.getBean("customNamedParameterJdbcOperations"));
				});
	}

	@Test
	public void testDependencyToDataSourceInitialization() {
		this.contextRunner.withUserConfiguration(DataSourceInitializationValidator.class)
				.withPropertyValues("spring.datasource.initialization-mode=always")
				.run((context) -> {
					assertThat(context).hasNotFailed();
					assertThat(context
							.getBean(DataSourceInitializationValidator.class).count)
									.isEqualTo(1);
				});
	}

	@Test
	public void testDependencyToFlyway() {
		this.contextRunner.withUserConfiguration(DataSourceMigrationValidator.class)
				.withPropertyValues("spring.flyway.locations:classpath:db/city")
				.withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
				.run((context) -> {
					assertThat(context).hasNotFailed();
					assertThat(context.getBean(DataSourceMigrationValidator.class).count)
							.isEqualTo(0);
				});
	}

	@Test
	public void testDependencyToFlywayWithJdbcTemplateMixed() {
		this.contextRunner
				.withUserConfiguration(NamedParameterDataSourceMigrationValidator.class)
				.withPropertyValues("spring.flyway.locations:classpath:db/city_np")
				.withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
				.run((context) -> {
					assertThat(context).hasNotFailed();
					assertThat(context.getBean(JdbcTemplate.class)).isNotNull();
					assertThat(context.getBean(
							NamedParameterDataSourceMigrationValidator.class).count)
									.isEqualTo(1);
				});
	}

	@Test
	public void testDependencyToFlywayWithOnlyNamedParameterJdbcTemplate() {
		ApplicationContextRunner contextRunner1 = new ApplicationContextRunner()
				.withPropertyValues("spring.datasource.initialization-mode=never",
						"spring.datasource.generate-unique-name=true")
				.withConfiguration(
						AutoConfigurations.of(DataSourceAutoConfiguration.class,
								JdbcTemplateAutoConfiguration.class,
								OnlyNamedParameterJdbcTemplateAutoConfiguration.class));
		contextRunner1
				.withUserConfiguration(NamedParameterDataSourceMigrationValidator.class)
				.withPropertyValues("spring.flyway.locations:classpath:db/city_np")
				.withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
				.run((context) -> {
					assertThat(context).hasNotFailed();
					assertThat(context.containsBean("jdbcTemplate")).isFalse();
					try {
						JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
						fail("org.springframework.boot.autoconfigure.jdbc.JdcTemplate should not exist in the application context");
					}
					catch (NoSuchBeanDefinitionException ex) {

					}
					assertThat(context.getBean(NamedParameterJdbcTemplate.class))
							.isNotNull();
					assertThat(context.getBean(
							NamedParameterDataSourceMigrationValidator.class).count)
									.isEqualTo(1);
				});
	}

	@Test
	public void testDependencyToLiquibase() {
		this.contextRunner.withUserConfiguration(DataSourceMigrationValidator.class)
				.withPropertyValues(
						"spring.liquibase.changeLog:classpath:db/changelog/db.changelog-city.yaml")
				.withConfiguration(
						AutoConfigurations.of(LiquibaseAutoConfiguration.class))
				.run((context) -> {
					assertThat(context).hasNotFailed();
					assertThat(context.getBean(DataSourceMigrationValidator.class).count)
							.isEqualTo(0);
				});
	}

	@Test
	public void testDependencyToLiquibaseWithJdbcTemplateMixed() {
		this.contextRunner
				.withUserConfiguration(NamedParameterDataSourceMigrationValidator.class)
				.withPropertyValues(
						"spring.liquibase.changeLog:classpath:db/changelog/db.changelog-city-np.yaml")
				.withConfiguration(
						AutoConfigurations.of(LiquibaseAutoConfiguration.class))
				.run((context) -> {
					assertThat(context).hasNotFailed();
					assertThat(context.getBean(JdbcTemplate.class)).isNotNull();
					assertThat(context.getBean(
							NamedParameterDataSourceMigrationValidator.class).count)
									.isEqualTo(1);
				});
	}

	@Test
	public void testDependencyToLiquibaseWithOnlyNamedParameterJdbcTemplate() {
		this.contextRunner
				.withUserConfiguration(NamedParameterDataSourceMigrationValidator.class)
				.withPropertyValues(
						"spring.liquibase.changeLog:classpath:db/changelog/db.changelog-city-np.yaml")
				.withConfiguration(AutoConfigurations.of(
						OnlyNamedParameterJdbcTemplateAutoConfiguration.class,
						LiquibaseAutoConfiguration.class))
				.run((context) -> {
					assertThat(context).hasNotFailed();
					assertThat(context.containsBean("jdbcTemplate")).isFalse();
					try {
						JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
						fail("org.springframework.boot.autoconfigure.jdbc.JdcTemplate should not exist in the application context");
					}
					catch (NoSuchBeanDefinitionException ex) {

					}
					assertThat(context.getBean(NamedParameterJdbcTemplate.class))
							.isNotNull();
					assertThat(context.getBean(
							NamedParameterDataSourceMigrationValidator.class).count)
									.isEqualTo(1);
				});
	}

	@Configuration
	static class CustomConfiguration {

		@Bean
		public JdbcOperations customJdbcOperations(DataSource dataSource) {
			return new JdbcTemplate(dataSource);
		}

		@Bean
		public NamedParameterJdbcOperations customNamedParameterJdbcOperations(
				DataSource dataSource) {
			return new NamedParameterJdbcTemplate(dataSource);
		}

	}

	@Configuration
	static class TestDataSourceConfiguration {

		@Bean
		public DataSource customDataSource() {
			return new TestDataSource();
		}

	}

	@Configuration
	static class MultiJdbcTemplateConfiguration {

		@Bean
		public JdbcTemplate test1Template() {
			return mock(JdbcTemplate.class);
		}

		@Bean
		public JdbcTemplate test2Template() {
			return mock(JdbcTemplate.class);
		}

	}

	@Configuration
	static class MultiJdbcTemplateUsingPrimaryConfiguration {

		@Bean
		@Primary
		public JdbcTemplate test1Template() {
			return mock(JdbcTemplate.class);
		}

		@Bean
		public JdbcTemplate test2Template() {
			return mock(JdbcTemplate.class);
		}

	}

	static class DataSourceInitializationValidator {

		private final Integer count;

		DataSourceInitializationValidator(JdbcTemplate jdbcTemplate) {
			this.count = jdbcTemplate.queryForObject("SELECT COUNT(*) from BAR",
					Integer.class);
		}

	}

	static class DataSourceMigrationValidator {

		private final Integer count;

		DataSourceMigrationValidator(JdbcTemplate jdbcTemplate) {
			this.count = jdbcTemplate.queryForObject("SELECT COUNT(*) from CITY",
					Integer.class);
		}

	}

	static class NamedParameterDataSourceMigrationValidator {

		private final Integer count;

		NamedParameterDataSourceMigrationValidator(
				NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
			String sql = "SELECT COUNT(*) from CITY WHERE id = :id";
			Map<String, Long> param = new HashMap<>();
			param.put("id", 1L);
			this.count = namedParameterJdbcTemplate.queryForObject(sql, param,
					Integer.class);
		}

	}

	@Configuration
	@ConditionalOnClass({ DataSource.class })
	@ConditionalOnSingleCandidate(DataSource.class)
	@AutoConfigureAfter({ DataSourceAutoConfiguration.class,
			JdbcTemplateAutoConfiguration.class })
	@AutoConfigureBefore({ FlywayAutoConfiguration.class,
			LiquibaseAutoConfiguration.class })
	@EnableConfigurationProperties(JdbcProperties.class)
	static class OnlyNamedParameterJdbcTemplateAutoConfiguration
			implements BeanDefinitionRegistryPostProcessor {

		@Bean
		public NamedParameterJdbcTemplate myNamedParameterJdbcTemplate(
				DataSource dataSource) {
			return new NamedParameterJdbcTemplate(dataSource);
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
				throws BeansException {
			// do nothing
		}

		/**
		 * <p>
		 * we should remove the jdbc template bean definition to keep only
		 * NamedParameterJdbcTemplate is registerd in the bean container
		 * </p>
		 * @param registry the bean definition registry.
		 * @throws BeansException if the bean registry have any exception.
		 */
		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
				throws BeansException {
			String[] excludeBeanNames = new String[] { "jdbcTemplate",
					"namedParameterJdbcTemplate" };
			for (String beanName : excludeBeanNames) {
				BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
				if (beanDefinition != null) {
					registry.removeBeanDefinition(beanName);
				}
			}

		}

	}

}
