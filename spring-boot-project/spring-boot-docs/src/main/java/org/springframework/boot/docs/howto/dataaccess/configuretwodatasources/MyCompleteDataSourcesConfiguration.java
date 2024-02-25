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

package org.springframework.boot.docs.howto.dataaccess.configuretwodatasources;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbcp2.BasicDataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * MyCompleteDataSourcesConfiguration class.
 */
@Configuration(proxyBeanMethods = false)
public class MyCompleteDataSourcesConfiguration {

	/**
	 * Returns the DataSourceProperties object for the first data source. This method is
	 * annotated with @Bean and @Primary to indicate that it is a bean definition and the
	 * primary data source. The configuration properties for the first data source are
	 * specified using the @ConfigurationProperties annotation with the prefix
	 * "app.datasource.first".
	 * @return The DataSourceProperties object for the first data source.
	 */
	@Bean
	@Primary
	@ConfigurationProperties("app.datasource.first")
	public DataSourceProperties firstDataSourceProperties() {
		return new DataSourceProperties();
	}

	/**
	 * Creates a primary HikariDataSource bean using the configuration properties
	 * specified in "app.datasource.first.configuration".
	 * @param firstDataSourceProperties the DataSourceProperties object containing the
	 * configuration properties for the first data source
	 * @return a HikariDataSource object representing the first data source
	 */
	@Bean
	@Primary
	@ConfigurationProperties("app.datasource.first.configuration")
	public HikariDataSource firstDataSource(DataSourceProperties firstDataSourceProperties) {
		return firstDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
	}

	/**
	 * Retrieves the properties for the second data source.
	 * @return The DataSourceProperties object containing the properties for the second
	 * data source.
	 */
	@Bean
	@ConfigurationProperties("app.datasource.second")
	public DataSourceProperties secondDataSourceProperties() {
		return new DataSourceProperties();
	}

	/**
	 * Creates a second data source using the provided secondDataSourceProperties.
	 * @param secondDataSourceProperties the properties for the second data source
	 * @return the second data source
	 */
	@Bean
	@ConfigurationProperties("app.datasource.second.configuration")
	public BasicDataSource secondDataSource(
			@Qualifier("secondDataSourceProperties") DataSourceProperties secondDataSourceProperties) {
		return secondDataSourceProperties.initializeDataSourceBuilder().type(BasicDataSource.class).build();
	}

}
