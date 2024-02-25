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

package org.springframework.boot.autoconfigure.sql.init;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.util.StringUtils;

/**
 * DataSourceInitializationConfiguration class.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean({ SqlDataSourceScriptDatabaseInitializer.class, SqlR2dbcScriptDatabaseInitializer.class })
@ConditionalOnSingleCandidate(DataSource.class)
@ConditionalOnClass(DatabasePopulator.class)
class DataSourceInitializationConfiguration {

	/**
	 * Creates a {@link SqlDataSourceScriptDatabaseInitializer} bean to initialize the
	 * database using SQL scripts.
	 * @param dataSource the {@link DataSource} to be used for database initialization
	 * @param properties the {@link SqlInitializationProperties} containing the
	 * initialization properties
	 * @return the {@link SqlDataSourceScriptDatabaseInitializer} bean
	 */
	@Bean
	SqlDataSourceScriptDatabaseInitializer dataSourceScriptDatabaseInitializer(DataSource dataSource,
			SqlInitializationProperties properties) {
		return new SqlDataSourceScriptDatabaseInitializer(
				determineDataSource(dataSource, properties.getUsername(), properties.getPassword()), properties);
	}

	/**
	 * Determines the appropriate data source based on the provided username and password.
	 * If both username and password are provided, a new data source is created with the
	 * provided credentials. Otherwise, the original data source is returned.
	 * @param dataSource the original data source
	 * @param username the username for the new data source (optional)
	 * @param password the password for the new data source (optional)
	 * @return the determined data source
	 */
	private static DataSource determineDataSource(DataSource dataSource, String username, String password) {
		if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
			return DataSourceBuilder.derivedFrom(dataSource)
				.username(username)
				.password(password)
				.type(SimpleDriverDataSource.class)
				.build();
		}
		return dataSource;
	}

}
