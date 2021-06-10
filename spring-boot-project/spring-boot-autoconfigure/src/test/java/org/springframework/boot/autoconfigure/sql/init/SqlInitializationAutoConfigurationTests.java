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

package org.springframework.boot.autoconfigure.sql.init;

import java.nio.charset.Charset;
import java.util.List;

import javax.sql.DataSource;

import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.r2dbc.init.R2dbcScriptDatabaseInitializer;
import org.springframework.boot.sql.init.AbstractScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SqlInitializationAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public class SqlInitializationAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SqlInitializationAutoConfiguration.class)).withPropertyValues(
					"spring.datasource.generate-unique-name:true", "spring.r2dbc.generate-unique-name:true");

	@Test
	void whenNoDataSourceOrConnectionFactoryIsAvailableThenAutoConfigurationBacksOff() {
		this.contextRunner
				.run((context) -> assertThat(context).doesNotHaveBean(AbstractScriptDatabaseInitializer.class));
	}

	@Test
	void whenConnectionFactoryIsAvailableThenR2dbcInitializerIsAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
				.run((context) -> assertThat(context).hasSingleBean(R2dbcScriptDatabaseInitializer.class));
	}

	@Test
	@Deprecated
	void whenConnectionFactoryIsAvailableAndInitializationIsDisabledThenInitializerIsNotAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
				.withPropertyValues("spring.sql.init.enabled:false")
				.run((context) -> assertThat(context).doesNotHaveBean(AbstractScriptDatabaseInitializer.class));
	}

	@Test
	void whenConnectionFactoryIsAvailableAndModeIsNeverThenInitializerIsNotAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
				.withInitializer(new ConditionEvaluationReportLoggingListener(LogLevel.INFO))
				.withPropertyValues("spring.sql.init.mode:never")
				.run((context) -> assertThat(context).doesNotHaveBean(AbstractScriptDatabaseInitializer.class));
	}

	@Test
	void whenDataSourceIsAvailableThenDataSourceInitializerIsAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
				.run((context) -> assertThat(context).hasSingleBean(DataSourceScriptDatabaseInitializer.class));
	}

	@Test
	@Deprecated
	void whenDataSourceIsAvailableAndInitializationIsDisabledThenInitializerIsNotAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
				.withPropertyValues("spring.sql.init.enabled:false")
				.run((context) -> assertThat(context).doesNotHaveBean(AbstractScriptDatabaseInitializer.class));
	}

	@Test
	void whenDataSourceIsAvailableAndModeIsNeverThenThenInitializerIsNotAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
				.withPropertyValues("spring.sql.init.mode:never")
				.run((context) -> assertThat(context).doesNotHaveBean(AbstractScriptDatabaseInitializer.class));
	}

	@Test
	void whenDataSourceAndConnectionFactoryAreAvailableThenOnlyR2dbcInitializerIsAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
				.withUserConfiguration(DataSourceAutoConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(ConnectionFactory.class)
						.hasSingleBean(DataSource.class).hasSingleBean(R2dbcScriptDatabaseInitializer.class)
						.doesNotHaveBean(DataSourceScriptDatabaseInitializer.class));
	}

	@Test
	void whenAnInitializerIsDefinedThenInitializerIsNotAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
				.withUserConfiguration(DataSourceAutoConfiguration.class, DatabaseInitializerConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(AbstractScriptDatabaseInitializer.class)
						.hasBean("customInitializer"));
	}

	@Test
	void whenBeanIsAnnotatedAsDependingOnDatabaseInitializationThenItDependsOnR2dbcScriptDatabaseInitializer() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
				.withUserConfiguration(DependsOnInitializedDatabaseConfiguration.class).run((context) -> {
					BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition(
							"sqlInitializationAutoConfigurationTests.DependsOnInitializedDatabaseConfiguration");
					assertThat(beanDefinition.getDependsOn())
							.containsExactlyInAnyOrder("r2dbcScriptDatabaseInitializer");
				});
	}

	@Test
	void whenBeanIsAnnotatedAsDependingOnDatabaseInitializationThenItDependsOnDataSourceScriptDatabaseInitializer() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
				.withUserConfiguration(DependsOnInitializedDatabaseConfiguration.class).run((context) -> {
					BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition(
							"sqlInitializationAutoConfigurationTests.DependsOnInitializedDatabaseConfiguration");
					assertThat(beanDefinition.getDependsOn())
							.containsExactlyInAnyOrder("dataSourceScriptDatabaseInitializer");
				});
	}

	@Test
	void whenADataSourceIsAvailableAndSpringJdbcIsNotThenAutoConfigurationBacksOff() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
				.withClassLoader(new FilteredClassLoader(DatabasePopulator.class)).run((context) -> {
					assertThat(context).hasSingleBean(DataSource.class);
					assertThat(context).doesNotHaveBean(AbstractScriptDatabaseInitializer.class);
				});
	}

	@Test
	void whenAConnectionFactoryIsAvailableAndSpringR2dbcIsNotThenAutoConfigurationBacksOff() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
				.withClassLoader(
						new FilteredClassLoader(org.springframework.r2dbc.connection.init.DatabasePopulator.class))
				.run((context) -> {
					assertThat(context).hasSingleBean(ConnectionFactory.class);
					assertThat(context).doesNotHaveBean(AbstractScriptDatabaseInitializer.class);
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class DatabaseInitializerConfiguration {

		@Bean
		AbstractScriptDatabaseInitializer customInitializer() {
			return new AbstractScriptDatabaseInitializer(new DatabaseInitializationSettings()) {

				@Override
				protected void runScripts(List<Resource> resources, boolean continueOnError, String separator,
						Charset encoding) {
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
