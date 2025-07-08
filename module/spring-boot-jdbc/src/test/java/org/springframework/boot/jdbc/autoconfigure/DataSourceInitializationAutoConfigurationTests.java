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

package org.springframework.boot.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.autoconfigure.init.ApplicationScriptDatabaseInitializer;
import org.springframework.boot.sql.init.AbstractScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.init.DatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DataSourceInitializationAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DataSourceInitializationAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataSourceInitializationAutoConfiguration.class))
		.withPropertyValues("spring.datasource.generate-unique-name:true");

	@Test
	void whenNoDataSourceIsAvailableThenAutoConfigurationBacksOff() {
		this.contextRunner
			.run((context) -> assertThat(context).doesNotHaveBean(AbstractScriptDatabaseInitializer.class));
	}

	@Test
	void whenDataSourceIsAvailableThenDataSourceInitializerIsAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(DataSourceScriptDatabaseInitializer.class));
	}

	@Test
	void whenDataSourceIsAvailableAndModeIsNeverThenInitializerIsNotAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withPropertyValues("spring.sql.init.mode:never")
			.run((context) -> assertThat(context).doesNotHaveBean(AbstractScriptDatabaseInitializer.class));
	}

	@Test
	void whenAnApplicationInitializerIsDefinedThenInitializerIsNotAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withUserConfiguration(ApplicationDatabaseInitializerConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(ApplicationScriptDatabaseInitializer.class)
				.hasBean("customInitializer"));
	}

	@Test
	void whenAnInitializerIsDefinedThenApplicationInitializerIsStillAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withUserConfiguration(DatabaseInitializerConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(ApplicationDataSourceScriptDatabaseInitializer.class)
				.hasBean("customInitializer"));
	}

	@Test
	void whenBeanIsAnnotatedAsDependingOnDatabaseInitializationThenItDependsOnDataSourceScriptDatabaseInitializer() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withUserConfiguration(DependsOnInitializedDatabaseConfiguration.class)
			.run((context) -> {
				ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
				BeanDefinition beanDefinition = beanFactory.getBeanDefinition(
						"dataSourceInitializationAutoConfigurationTests.DependsOnInitializedDatabaseConfiguration");
				assertThat(beanDefinition.getDependsOn())
					.containsExactlyInAnyOrder("dataSourceScriptDatabaseInitializer");
			});
	}

	@Test
	void whenADataSourceIsAvailableAndSpringJdbcIsNotThenAutoConfigurationBacksOff() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withClassLoader(new FilteredClassLoader(DatabasePopulator.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(DataSource.class);
				assertThat(context).doesNotHaveBean(AbstractScriptDatabaseInitializer.class);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class ApplicationDatabaseInitializerConfiguration {

		@Bean
		ApplicationScriptDatabaseInitializer customInitializer() {
			return mock(ApplicationScriptDatabaseInitializer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DatabaseInitializerConfiguration {

		@Bean
		DataSourceScriptDatabaseInitializer customInitializer() {
			return new DataSourceScriptDatabaseInitializer(null, new DatabaseInitializationSettings()) {

				@Override
				protected void runScripts(Scripts scripts) {
					// No-op
				}

				@Override
				protected boolean isEmbeddedDatabase() {
					return true;
				}

			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@DependsOnDatabaseInitialization
	static class DependsOnInitializedDatabaseConfiguration {

		DependsOnInitializedDatabaseConfiguration() {

		}

	}

}
