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

package org.springframework.boot.sql.init;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Settings for initializing an SQL database.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public class DatabaseInitializationSettings {

	private List<String> schemaLocations;

	private List<String> dataLocations;

	private boolean continueOnError = false;

	private String separator = ";";

	private Charset encoding;

	private DatabaseInitializationMode mode = DatabaseInitializationMode.EMBEDDED;

	/**
	 * Returns the locations of the schema (DDL) scripts to apply to the database.
	 * @return the locations of the schema scripts
	 */
	public List<String> getSchemaLocations() {
		return this.schemaLocations;
	}

	/**
	 * Sets the locations of schema (DDL) scripts to apply to the database. By default,
	 * initialization will fail if a location does not exist. To prevent a failure, a
	 * location can be made optional by prefixing it with {@code optional:}.
	 * @param schemaLocations locations of the schema scripts
	 */
	public void setSchemaLocations(List<String> schemaLocations) {
		this.schemaLocations = schemaLocations;
	}

	/**
	 * Returns the locations of data (DML) scripts to apply to the database.
	 * @return the locations of the data scripts
	 */
	public List<String> getDataLocations() {
		return this.dataLocations;
	}

	/**
	 * Sets the locations of data (DML) scripts to apply to the database. By default,
	 * initialization will fail if a location does not exist. To prevent a failure, a
	 * location can be made optional by prefixing it with {@code optional:}.
	 * @param dataLocations locations of the data scripts
	 */
	public void setDataLocations(List<String> dataLocations) {
		this.dataLocations = dataLocations;
	}

	/**
	 * Returns whether to continue when an error occurs while applying a schema or data
	 * script.
	 * @return whether to continue on error
	 */
	public boolean isContinueOnError() {
		return this.continueOnError;
	}

	/**
	 * Sets whether initialization should continue when an error occurs when applying a
	 * schema or data script.
	 * @param continueOnError whether to continue when an error occurs.
	 */
	public void setContinueOnError(boolean continueOnError) {
		this.continueOnError = continueOnError;
	}

	/**
	 * Returns the statement separator used in the schema and data scripts.
	 * @return the statement separator
	 */
	public String getSeparator() {
		return this.separator;
	}

	/**
	 * Sets the statement separator to use when reading the schema and data scripts.
	 * @param separator statement separator used in the schema and data scripts
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	/**
	 * Returns the encoding to use when reading the schema and data scripts.
	 * @return the script encoding
	 */
	public Charset getEncoding() {
		return this.encoding;
	}

	/**
	 * Sets the encoding to use when reading the schema and data scripts.
	 * @param encoding encoding to use when reading the schema and data scripts
	 */
	public void setEncoding(Charset encoding) {
		this.encoding = encoding;
	}

	/**
	 * Gets the mode to use when determining whether database initialization should be
	 * performed.
	 * @return the initialization mode
	 * @since 2.5.1
	 */
	public DatabaseInitializationMode getMode() {
		return this.mode;
	}

	/**
	 * Sets the mode the use when determining whether database initialization should be
	 * performed.
	 * @param mode the initialization mode
	 * @since 2.5.1
	 */
	public void setMode(DatabaseInitializationMode mode) {
		this.mode = mode;
	}

}
