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

package org.springframework.boot.docs.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbcp2.BasicDataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Example configuration for configuring two data sources with what Spring Boot does in
 * auto-configuration.
 *
 * @author Stephane Nicoll
 */
public class CompleteTwoDataSourcesExample {

	/**
	 * A complete configuration that exposes two data sources.
	 */
	@Configuration
	public static class CompleteDataSourcesConfiguration {

		// tag::configuration[]
		@Bean
		@Primary
		@ConfigurationProperties("app.datasource.first")
		public DataSourceProperties firstDataSourceProperties() {
			return new DataSourceProperties();
		}

		@Bean
		@Primary
		@ConfigurationProperties("app.datasource.first.configuration")
		public HikariDataSource firstDataSource() {
			return firstDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
		}

		@Bean
		@ConfigurationProperties("app.datasource.second")
		public DataSourceProperties secondDataSourceProperties() {
			return new DataSourceProperties();
		}

		@Bean
		@ConfigurationProperties("app.datasource.second.configuration")
		public BasicDataSource secondDataSource() {
			return secondDataSourceProperties().initializeDataSourceBuilder().type(BasicDataSource.class).build();
		}
		// end::configuration[]

	}

}
