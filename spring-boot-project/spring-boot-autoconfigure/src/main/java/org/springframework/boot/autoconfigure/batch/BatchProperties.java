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

package org.springframework.boot.autoconfigure.batch;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.boot.jdbc.DataSourceInitializationMode;

/**
 * Configuration properties for Spring Batch.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Vedran Pavic
 * @author Mukul Kumar Chaundhyan
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = "spring.batch")
public class BatchProperties {

	private final Job job = new Job();

	private final Jdbc jdbc = new Jdbc();

	/**
	 * Return the datasource schema.
	 * @return the schema
	 * @deprecated since 2.5.0 for removal in 2.7.0 in favor of {@link Jdbc#getSchema()}
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.batch.jdbc.schema")
	public String getSchema() {
		return this.jdbc.getSchema();
	}

	@Deprecated
	public void setSchema(String schema) {
		this.jdbc.setSchema(schema);
	}

	/**
	 * Return the table prefix.
	 * @return the table prefix
	 * @deprecated since 2.5.0 for removal in 2.7.0 in favor of
	 * {@link Jdbc#getTablePrefix()}
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.batch.jdbc.table-prefix")
	public String getTablePrefix() {
		return this.jdbc.getTablePrefix();
	}

	@Deprecated
	public void setTablePrefix(String tablePrefix) {
		this.jdbc.setTablePrefix(tablePrefix);
	}

	/**
	 * Return whether the schema should be initialized.
	 * @return the initialization mode
	 * @deprecated since 2.5.0 for removal in 2.7.0 in favor of
	 * {@link Jdbc#getInitializeSchema()}
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.batch.jdbc.initialize-schema")
	public DataSourceInitializationMode getInitializeSchema() {
		return this.jdbc.getInitializeSchema();
	}

	@Deprecated
	public void setInitializeSchema(DataSourceInitializationMode initializeSchema) {
		this.jdbc.setInitializeSchema(initializeSchema);
	}

	public Job getJob() {
		return this.job;
	}

	public Jdbc getJdbc() {
		return this.jdbc;
	}

	public static class Job {

		/**
		 * Comma-separated list of job names to execute on startup (for instance,
		 * 'job1,job2'). By default, all Jobs found in the context are executed.
		 */
		private String names = "";

		public String getNames() {
			return this.names;
		}

		public void setNames(String names) {
			this.names = names;
		}

	}

	public static class Jdbc {

		private static final String DEFAULT_SCHEMA_LOCATION = "classpath:org/springframework/"
				+ "batch/core/schema-@@platform@@.sql";

		/**
		 * Path to the SQL file to use to initialize the database schema.
		 */
		private String schema = DEFAULT_SCHEMA_LOCATION;

		/**
		 * Platform to use in initialization scripts if the @@platform@@ placeholder is
		 * used. Auto-detected by default.
		 */
		private String platform;

		/**
		 * Table prefix for all the batch meta-data tables.
		 */
		private String tablePrefix;

		/**
		 * Database schema initialization mode.
		 */
		private DataSourceInitializationMode initializeSchema = DataSourceInitializationMode.EMBEDDED;

		public String getSchema() {
			return this.schema;
		}

		public void setSchema(String schema) {
			this.schema = schema;
		}

		public String getPlatform() {
			return this.platform;
		}

		public void setPlatform(String platform) {
			this.platform = platform;
		}

		public String getTablePrefix() {
			return this.tablePrefix;
		}

		public void setTablePrefix(String tablePrefix) {
			this.tablePrefix = tablePrefix;
		}

		public DataSourceInitializationMode getInitializeSchema() {
			return this.initializeSchema;
		}

		public void setInitializeSchema(DataSourceInitializationMode initializeSchema) {
			this.initializeSchema = initializeSchema;
		}

	}

}
