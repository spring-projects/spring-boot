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

import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.autoconfigure.init.ApplicationScriptDatabaseInitializer;
import org.springframework.boot.sql.autoconfigure.init.SqlInitializationProperties;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;

/**
 * {@link DataSourceScriptDatabaseInitializer} for the primary SQL database. May be
 * registered as a bean to override auto-configuration.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0.0
 */
public class ApplicationDataSourceScriptDatabaseInitializer extends DataSourceScriptDatabaseInitializer
		implements ApplicationScriptDatabaseInitializer {

	/**
	 * Create a new {@link ApplicationDataSourceScriptDatabaseInitializer} instance.
	 * @param dataSource the primary SQL data source
	 * @param properties the SQL initialization properties
	 */
	public ApplicationDataSourceScriptDatabaseInitializer(DataSource dataSource,
			SqlInitializationProperties properties) {
		this(dataSource, ApplicationScriptDatabaseInitializer.getSettings(properties));
	}

	/**
	 * Create a new {@link ApplicationDataSourceScriptDatabaseInitializer} instance.
	 * @param dataSource the primary SQL data source
	 * @param settings the database initialization settings
	 */
	public ApplicationDataSourceScriptDatabaseInitializer(DataSource dataSource,
			DatabaseInitializationSettings settings) {
		super(dataSource, settings);
	}

}
