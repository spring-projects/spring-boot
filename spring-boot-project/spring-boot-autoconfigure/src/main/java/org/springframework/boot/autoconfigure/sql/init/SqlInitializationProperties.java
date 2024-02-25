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

import java.nio.charset.Charset;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.sql.init.DatabaseInitializationMode;

/**
 * {@link ConfigurationProperties Configuration properties} for initializing an SQL
 * database.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
@ConfigurationProperties("spring.sql.init")
public class SqlInitializationProperties {

	/**
	 * Locations of the schema (DDL) scripts to apply to the database.
	 */
	private List<String> schemaLocations;

	/**
	 * Locations of the data (DML) scripts to apply to the database.
	 */
	private List<String> dataLocations;

	/**
	 * Platform to use in the default schema or data script locations,
	 * schema-${platform}.sql and data-${platform}.sql.
	 */
	private String platform = "all";

	/**
	 * Username of the database to use when applying initialization scripts (if
	 * different).
	 */
	private String username;

	/**
	 * Password of the database to use when applying initialization scripts (if
	 * different).
	 */
	private String password;

	/**
	 * Whether initialization should continue when an error occurs.
	 */
	private boolean continueOnError = false;

	/**
	 * Statement separator in the schema and data scripts.
	 */
	private String separator = ";";

	/**
	 * Encoding of the schema and data scripts.
	 */
	private Charset encoding;

	/**
	 * Mode to apply when determining whether initialization should be performed.
	 */
	private DatabaseInitializationMode mode = DatabaseInitializationMode.EMBEDDED;

	/**
	 * Returns the list of schema locations.
	 * @return the list of schema locations
	 */
	public List<String> getSchemaLocations() {
		return this.schemaLocations;
	}

	/**
	 * Sets the list of schema locations.
	 * @param schemaLocations the list of schema locations to be set
	 */
	public void setSchemaLocations(List<String> schemaLocations) {
		this.schemaLocations = schemaLocations;
	}

	/**
	 * Returns the list of data locations.
	 * @return the list of data locations
	 */
	public List<String> getDataLocations() {
		return this.dataLocations;
	}

	/**
	 * Sets the data locations for the SqlInitializationProperties.
	 * @param dataLocations the list of data locations to be set
	 */
	public void setDataLocations(List<String> dataLocations) {
		this.dataLocations = dataLocations;
	}

	/**
	 * Returns the platform of the SqlInitializationProperties.
	 * @return the platform of the SqlInitializationProperties
	 */
	public String getPlatform() {
		return this.platform;
	}

	/**
	 * Sets the platform for SQL initialization.
	 * @param platform the platform to set
	 */
	public void setPlatform(String platform) {
		this.platform = platform;
	}

	/**
	 * Returns the username associated with the SqlInitializationProperties object.
	 * @return the username
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Sets the username for the SQL initialization properties.
	 * @param username the username to be set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Returns the password used for SQL initialization.
	 * @return the password used for SQL initialization
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Sets the password for the SQL initialization properties.
	 * @param password the password to be set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Returns a boolean value indicating whether the program should continue executing
	 * even if an error occurs during SQL initialization.
	 * @return true if the program should continue executing on error, false otherwise
	 */
	public boolean isContinueOnError() {
		return this.continueOnError;
	}

	/**
	 * Sets the flag indicating whether to continue executing SQL statements even if an
	 * error occurs.
	 * @param continueOnError the flag indicating whether to continue on error
	 */
	public void setContinueOnError(boolean continueOnError) {
		this.continueOnError = continueOnError;
	}

	/**
	 * Returns the separator used in the SqlInitializationProperties class.
	 * @return the separator used in the SqlInitializationProperties class
	 */
	public String getSeparator() {
		return this.separator;
	}

	/**
	 * Sets the separator used in SQL initialization properties.
	 * @param separator the separator to be set
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	/**
	 * Returns the encoding used by the SqlInitializationProperties.
	 * @return the encoding used by the SqlInitializationProperties
	 */
	public Charset getEncoding() {
		return this.encoding;
	}

	/**
	 * Sets the encoding for the SQL initialization properties.
	 * @param encoding the encoding to be set
	 */
	public void setEncoding(Charset encoding) {
		this.encoding = encoding;
	}

	/**
	 * Returns the mode of database initialization.
	 * @return the mode of database initialization
	 */
	public DatabaseInitializationMode getMode() {
		return this.mode;
	}

	/**
	 * Sets the mode for database initialization.
	 * @param mode the mode to set for database initialization
	 */
	public void setMode(DatabaseInitializationMode mode) {
		this.mode = mode;
	}

}
