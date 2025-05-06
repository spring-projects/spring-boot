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

package org.springframework.boot.jdbc.init;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationPropertiesSource;
import org.springframework.boot.sql.init.DatabaseInitializationMode;

/**
 * Base configuration properties class for performing SQL database initialization.
 *
 * @author Yanming Zhou
 * @since 4.0.0
 */
@ConfigurationPropertiesSource
public abstract class DatabaseInitializationProperties {

	/**
	 * Path to the SQL file to use to initialize the database schema.
	 */
	private String schema = getDefaultSchemaLocation();

	/**
	 * Platform to use in initialization scripts if the @@platform@@ placeholder is used.
	 * Auto-detected by default.
	 */
	private @Nullable String platform;

	/**
	 * Database schema initialization mode.
	 */
	private DatabaseInitializationMode initializeSchema = DatabaseInitializationMode.EMBEDDED;

	/**
	 * Whether initialization should continue when an error occurs when applying a schema
	 * script.
	 */
	private boolean continueOnError = true;

	public String getSchema() {
		return this.schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public @Nullable String getPlatform() {
		return this.platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public DatabaseInitializationMode getInitializeSchema() {
		return this.initializeSchema;
	}

	public void setInitializeSchema(DatabaseInitializationMode initializeSchema) {
		this.initializeSchema = initializeSchema;
	}

	public boolean isContinueOnError() {
		return this.continueOnError;
	}

	public void setContinueOnError(boolean continueOnError) {
		this.continueOnError = continueOnError;
	}

	public abstract String getDefaultSchemaLocation();

}
