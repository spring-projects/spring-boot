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

import javax.sql.DataSource;

import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * {@link DataSourceScriptDatabaseInitializer} for the primary SQL database. May be
 * registered as a bean to override auto-configuration.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.6.0
 */
@ImportRuntimeHints(SqlInitializationScriptsRuntimeHints.class)
public class SqlDataSourceScriptDatabaseInitializer extends DataSourceScriptDatabaseInitializer {

	/**
	 * Create a new {@link SqlDataSourceScriptDatabaseInitializer} instance.
	 * @param dataSource the primary SQL data source
	 * @param properties the SQL initialization properties
	 * @see #getSettings
	 */
	public SqlDataSourceScriptDatabaseInitializer(DataSource dataSource, SqlInitializationProperties properties) {
		this(dataSource, getSettings(properties));
	}

	/**
	 * Create a new {@link SqlDataSourceScriptDatabaseInitializer} instance.
	 * @param dataSource the primary SQL data source
	 * @param settings the database initialization settings
	 * @see #getSettings
	 */
	public SqlDataSourceScriptDatabaseInitializer(DataSource dataSource, DatabaseInitializationSettings settings) {
		super(dataSource, settings);
	}

	/**
	 * Adapts {@link SqlInitializationProperties SQL initialization properties} to
	 * {@link DatabaseInitializationSettings}.
	 * @param properties the SQL initialization properties
	 * @return a new {@link DatabaseInitializationSettings} instance
	 * @see #SqlDataSourceScriptDatabaseInitializer(DataSource,
	 * DatabaseInitializationSettings)
	 */
	public static DatabaseInitializationSettings getSettings(SqlInitializationProperties properties) {
		return SettingsCreator.createFrom(properties);
	}

}
