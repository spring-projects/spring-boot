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

package org.springframework.boot.autoconfigure.batch;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.transaction.annotation.Isolation;

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
     * Returns the job associated with this BatchProperties object.
     *
     * @return the job associated with this BatchProperties object
     */
    public Job getJob() {
		return this.job;
	}

	/**
     * Returns the Jdbc object associated with this BatchProperties instance.
     *
     * @return the Jdbc object
     */
    public Jdbc getJdbc() {
		return this.jdbc;
	}

	/**
     * Job class.
     */
    public static class Job {

		/**
		 * Job name to execute on startup. Must be specified if multiple Jobs are found in
		 * the context.
		 */
		private String name = "";

		/**
         * Returns the name of the Job.
         *
         * @return the name of the Job
         */
        public String getName() {
			return this.name;
		}

		/**
         * Sets the name of the job.
         * 
         * @param name the name of the job
         */
        public void setName(String name) {
			this.name = name;
		}

	}

	/**
     * Jdbc class.
     */
    public static class Jdbc {

		private static final String DEFAULT_SCHEMA_LOCATION = "classpath:org/springframework/"
				+ "batch/core/schema-@@platform@@.sql";

		/**
		 * Transaction isolation level to use when creating job meta-data for new jobs.
		 */
		private Isolation isolationLevelForCreate;

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
		private DatabaseInitializationMode initializeSchema = DatabaseInitializationMode.EMBEDDED;

		/**
         * Returns the isolation level for create operations.
         * 
         * @return the isolation level for create operations
         */
        public Isolation getIsolationLevelForCreate() {
			return this.isolationLevelForCreate;
		}

		/**
         * Sets the isolation level for creating a new object.
         * 
         * @param isolationLevelForCreate the isolation level to be set
         */
        public void setIsolationLevelForCreate(Isolation isolationLevelForCreate) {
			this.isolationLevelForCreate = isolationLevelForCreate;
		}

		/**
         * Returns the schema of the Jdbc object.
         *
         * @return the schema of the Jdbc object
         */
        public String getSchema() {
			return this.schema;
		}

		/**
         * Sets the schema for the JDBC connection.
         * 
         * @param schema the name of the schema to be set
         */
        public void setSchema(String schema) {
			this.schema = schema;
		}

		/**
         * Returns the platform of the JDBC connection.
         * 
         * @return the platform of the JDBC connection
         */
        public String getPlatform() {
			return this.platform;
		}

		/**
         * Sets the platform for the JDBC connection.
         * 
         * @param platform the platform to be set
         */
        public void setPlatform(String platform) {
			this.platform = platform;
		}

		/**
         * Returns the table prefix used in the JDBC class.
         * 
         * @return the table prefix
         */
        public String getTablePrefix() {
			return this.tablePrefix;
		}

		/**
         * Sets the table prefix for the JDBC class.
         * 
         * @param tablePrefix the table prefix to be set
         */
        public void setTablePrefix(String tablePrefix) {
			this.tablePrefix = tablePrefix;
		}

		/**
         * Returns the initialization mode for the database schema.
         * 
         * @return the initialization mode for the database schema
         */
        public DatabaseInitializationMode getInitializeSchema() {
			return this.initializeSchema;
		}

		/**
         * Sets the mode for initializing the database schema.
         * 
         * @param initializeSchema the mode for initializing the database schema
         */
        public void setInitializeSchema(DatabaseInitializationMode initializeSchema) {
			this.initializeSchema = initializeSchema;
		}

	}

}
