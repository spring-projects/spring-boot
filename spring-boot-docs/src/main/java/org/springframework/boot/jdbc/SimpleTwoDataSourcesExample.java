/*
 * Copyright 2012-2017 the original author or authors.
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

import org.apache.commons.dbcp2.BasicDataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Example configuration for configuring a configurable secondary {@link DataSource} while
 * keeping the auto-configuration defaults for the primary one.
 *
 * @author Stephane Nicoll
 */
public class SimpleTwoDataSourcesExample {

	/**
	 * A simple configuration that exposes two data sources.
	 */
	@Configuration
	static class SimpleDataSourcesConfiguration {

		// tag::configuration[]
		@Bean
		@Primary
		@ConfigurationProperties("app.datasource.foo")
		public DataSourceProperties fooDataSourceProperties() {
			return new DataSourceProperties();
		}

		@Bean
		@Primary
		@ConfigurationProperties("app.datasource.foo")
		public DataSource fooDataSource() {
			return fooDataSourceProperties().initializeDataSourceBuilder().build();
		}

		@Bean
		@ConfigurationProperties("app.datasource.bar")
		public BasicDataSource barDataSource() {
			return (BasicDataSource) DataSourceBuilder.create()
					.type(BasicDataSource.class).build();
		}
		// end::configuration[]

	}

}
