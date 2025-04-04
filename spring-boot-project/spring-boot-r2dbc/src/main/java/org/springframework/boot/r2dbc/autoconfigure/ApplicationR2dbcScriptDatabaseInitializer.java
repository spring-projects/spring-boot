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

package org.springframework.boot.r2dbc.autoconfigure;

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.boot.r2dbc.init.R2dbcScriptDatabaseInitializer;
import org.springframework.boot.sql.autoconfigure.init.ApplicationScriptDatabaseInitializer;
import org.springframework.boot.sql.autoconfigure.init.SqlInitializationProperties;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;

/**
 * {@link R2dbcScriptDatabaseInitializer} for the primary SQL database. May be registered
 * as a bean to override auto-configuration.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0.0
 */
public class ApplicationR2dbcScriptDatabaseInitializer extends R2dbcScriptDatabaseInitializer
		implements ApplicationScriptDatabaseInitializer {

	/**
	 * Create a new {@code ApplicationR2dbcScriptDatabaseInitializer} instance.
	 * @param connectionFactory the primary SQL connection factory
	 * @param properties the SQL initialization properties
	 */
	public ApplicationR2dbcScriptDatabaseInitializer(ConnectionFactory connectionFactory,
			SqlInitializationProperties properties) {
		super(connectionFactory, ApplicationScriptDatabaseInitializer.getSettings(properties));
	}

	/**
	 * Create a new {@code ApplicationR2dbcScriptDatabaseInitializer} instance.
	 * @param connectionFactory the primary SQL connection factory
	 * @param settings the database initialization settings
	 */
	public ApplicationR2dbcScriptDatabaseInitializer(ConnectionFactory connectionFactory,
			DatabaseInitializationSettings settings) {
		super(connectionFactory, settings);
	}

}
