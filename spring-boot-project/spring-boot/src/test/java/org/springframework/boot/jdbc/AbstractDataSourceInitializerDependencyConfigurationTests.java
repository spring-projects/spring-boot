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

package org.springframework.boot.jdbc;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the configuration of dependencies on {@link AbstractDataSourceInitializer}
 * beans.
 *
 * @author Andy Wilkinson
 */
class AbstractDataSourceInitializerDependencyConfigurationTests {

	@Test
	void beanThatDependsOnDatabaseInitializationDependsOnAbstractDataSourceInitializerBeans() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				TestConfiguration.class)) {
			assertThat(context.getBeanFactory().getBeanDefinition("dependsOnDataSourceInitialization").getDependsOn())
					.contains("initializer");
		}
	}

	@Import(DatabaseInitializationDependencyConfigurer.class)
	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		DataSource dataSource() {
			return DataSourceBuilder.create().build();
		}

		@Bean
		@DependsOnDatabaseInitialization
		String dependsOnDataSourceInitialization() {
			return "test";
		}

		@Bean
		AbstractDataSourceInitializer initializer(DataSource dataSource, ResourceLoader resourceLoader) {
			return new AbstractDataSourceInitializer(dataSource, resourceLoader) {

				@Override
				protected String getSchemaLocation() {
					return null;
				}

				@Override
				protected DataSourceInitializationMode getMode() {
					return DataSourceInitializationMode.NEVER;
				}

			};
		}

	}

}
