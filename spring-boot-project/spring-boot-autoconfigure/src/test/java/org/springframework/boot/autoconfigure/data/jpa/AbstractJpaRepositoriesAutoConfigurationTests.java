/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.jpa;

import javax.persistence.EntityManagerFactory;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.alt.elasticsearch.CityElasticsearchDbRepository;
import org.springframework.boot.autoconfigure.data.alt.jpa.CityJpaRepository;
import org.springframework.boot.autoconfigure.data.alt.mongo.CityMongoDbRepository;
import org.springframework.boot.autoconfigure.data.jpa.city.City;
import org.springframework.boot.autoconfigure.data.jpa.city.CityRepository;
import org.springframework.boot.autoconfigure.data.jpa.country.Country;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for {@link JpaRepositoriesAutoConfiguration} tests.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Scott Frederick
 * @author Stefano Cordio
 */
abstract class AbstractJpaRepositoriesAutoConfigurationTests {

	final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HibernateJpaAutoConfiguration.class,
					JpaRepositoriesAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class))
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class);

	@Test
	void testDefaultRepositoryConfiguration() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(CityRepository.class);
			assertThat(context).hasSingleBean(PlatformTransactionManager.class);
			assertThat(context).hasSingleBean(EntityManagerFactory.class);
			assertThat(context.getBean(LocalContainerEntityManagerFactoryBean.class).getBootstrapExecutor()).isNull();
		});
	}

	@Test
	void testOverrideRepositoryConfiguration() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(CityJpaRepository.class);
			assertThat(context).hasSingleBean(PlatformTransactionManager.class);
			assertThat(context).hasSingleBean(EntityManagerFactory.class);
		});
	}

	@Test
	void autoConfigurationShouldNotKickInEvenIfManualConfigDidNotCreateAnyRepositories() {
		this.contextRunner.withUserConfiguration(SortOfInvalidCustomConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(CityRepository.class));
	}

	@Test
	void whenBootstrapModeIsLazyWithMultipleAsyncExecutorBootstrapExecutorIsConfigured() {
		this.contextRunner.withUserConfiguration(MultipleAsyncTaskExecutorConfiguration.class)
				.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class,
						TaskSchedulingAutoConfiguration.class))
				.withPropertyValues("spring.data.jpa.repositories.bootstrap-mode=lazy")
				.run((context) -> assertThat(
						context.getBean(LocalContainerEntityManagerFactoryBean.class).getBootstrapExecutor())
								.isEqualTo(context.getBean("applicationTaskExecutor")));
	}

	@Test
	void whenBootstrapModeIsLazyWithSingleAsyncExecutorBootstrapExecutorIsConfigured() {
		this.contextRunner.withUserConfiguration(SingleAsyncTaskExecutorConfiguration.class)
				.withPropertyValues("spring.data.jpa.repositories.bootstrap-mode=lazy")
				.run((context) -> assertThat(
						context.getBean(LocalContainerEntityManagerFactoryBean.class).getBootstrapExecutor())
								.isEqualTo(context.getBean("testAsyncTaskExecutor")));
	}

	@Test
	void whenBootstrapModeIsDeferredBootstrapExecutorIsConfigured() {
		this.contextRunner.withUserConfiguration(MultipleAsyncTaskExecutorConfiguration.class)
				.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class,
						TaskSchedulingAutoConfiguration.class))
				.withPropertyValues("spring.data.jpa.repositories.bootstrap-mode=deferred")
				.run((context) -> assertThat(
						context.getBean(LocalContainerEntityManagerFactoryBean.class).getBootstrapExecutor())
								.isEqualTo(context.getBean("applicationTaskExecutor")));
	}

	@Test
	void whenBootstrapModeIsDefaultBootstrapExecutorIsNotConfigured() {
		this.contextRunner.withUserConfiguration(MultipleAsyncTaskExecutorConfiguration.class)
				.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class,
						TaskSchedulingAutoConfiguration.class))
				.withPropertyValues("spring.data.jpa.repositories.bootstrap-mode=default").run((context) -> assertThat(
						context.getBean(LocalContainerEntityManagerFactoryBean.class).getBootstrapExecutor()).isNull());
	}

	@Test
	void bootstrapModeIsDefaultByDefault() {
		this.contextRunner.withUserConfiguration(MultipleAsyncTaskExecutorConfiguration.class)
				.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class,
						TaskSchedulingAutoConfiguration.class))
				.run((context) -> assertThat(
						context.getBean(LocalContainerEntityManagerFactoryBean.class).getBootstrapExecutor()).isNull());
	}

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	@Import(TestConfiguration.class)
	static class MultipleAsyncTaskExecutorConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@Import(TestConfiguration.class)
	static class SingleAsyncTaskExecutorConfiguration {

		@Bean
		SimpleAsyncTaskExecutor testAsyncTaskExecutor() {
			return new SimpleAsyncTaskExecutor();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	static class TestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableJpaRepositories(
			basePackageClasses = org.springframework.boot.autoconfigure.data.alt.jpa.CityJpaRepository.class,
			excludeFilters = { @Filter(type = FilterType.ASSIGNABLE_TYPE, value = CityMongoDbRepository.class),
					@Filter(type = FilterType.ASSIGNABLE_TYPE, value = CityElasticsearchDbRepository.class) })
	@TestAutoConfigurationPackage(City.class)
	static class CustomConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	// To not find any repositories
	@EnableJpaRepositories("foo.bar")
	@TestAutoConfigurationPackage(City.class)
	static class SortOfInvalidCustomConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(Country.class)
	static class RevisionRepositoryConfiguration {

	}

}
