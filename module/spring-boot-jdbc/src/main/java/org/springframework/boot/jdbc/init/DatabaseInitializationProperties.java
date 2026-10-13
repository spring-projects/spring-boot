/*
 * Copyright 2012-present the original author or authors.
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
