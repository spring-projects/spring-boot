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

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.boot.autoconfigure.batch.BatchDataSourceScriptDatabaseInitializer;
import org.springframework.boot.r2dbc.init.R2dbcScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * {@link R2dbcScriptDatabaseInitializer} for the primary SQL database. May be registered
 * as a bean to override auto-configuration.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.6.0
 */
@ImportRuntimeHints(SqlInitializationScriptsRuntimeHints.class)
public class SqlR2dbcScriptDatabaseInitializer extends R2dbcScriptDatabaseInitializer {

	/**
	 * Create a new {@link SqlDataSourceScriptDatabaseInitializer} instance.
	 * @param connectionFactory the primary SQL connection factory
	 * @param properties the SQL initialization properties
	 * @see #getSettings
	 */
	public SqlR2dbcScriptDatabaseInitializer(ConnectionFactory connectionFactory,
			SqlInitializationProperties properties) {
		super(connectionFactory, getSettings(properties));
	}

	/**
	 * Create a new {@link BatchDataSourceScriptDatabaseInitializer} instance.
	 * @param connectionFactory the primary SQL connection factory
	 * @param settings the database initialization settings
	 * @see #getSettings
	 */
	public SqlR2dbcScriptDatabaseInitializer(ConnectionFactory connectionFactory,
			DatabaseInitializationSettings settings) {
		super(connectionFactory, settings);
	}

	/**
	 * Adapts {@link SqlInitializationProperties SQL initialization properties} to
	 * {@link DatabaseInitializationSettings}.
	 * @param properties the SQL initialization properties
	 * @return a new {@link DatabaseInitializationSettings} instance
	 * @see #SqlR2dbcScriptDatabaseInitializer(ConnectionFactory,
	 * DatabaseInitializationSettings)
	 */
	public static DatabaseInitializationSettings getSettings(SqlInitializationProperties properties) {
		return SettingsCreator.createFrom(properties);
	}

}
