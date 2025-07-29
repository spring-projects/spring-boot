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

package org.springframework.boot.batch.autoconfigure;

import org.jspecify.annotations.Nullable;

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
 * @author Yanming Zhou
 * @since 4.0.0
 */
@ConfigurationProperties("spring.batch")
public class BatchProperties {

	private final Job job = new Job();

	private final Jdbc jdbc = new Jdbc();

	public Job getJob() {
		return this.job;
	}

	public Jdbc getJdbc() {
		return this.jdbc;
	}

	public static class Job {

		/**
		 * Job name to execute on startup. Must be specified if multiple Jobs are found in
		 * the context.
		 */
		private String name = "";

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	public static class Jdbc {

		private static final String DEFAULT_SCHEMA_LOCATION = "classpath:org/springframework/"
				+ "batch/core/schema-@@platform@@.sql";

		/**
		 * Whether to validate the transaction state.
		 */
		private boolean validateTransactionState = true;

		/**
		 * Transaction isolation level to use when creating job meta-data for new jobs.
		 */
		private @Nullable Isolation isolationLevelForCreate;

		/**
		 * Path to the SQL file to use to initialize the database schema.
		 */
		private String schema = DEFAULT_SCHEMA_LOCATION;

		/**
		 * Platform to use in initialization scripts if the @@platform@@ placeholder is
		 * used. Auto-detected by default.
		 */
		private @Nullable String platform;

		/**
		 * Table prefix for all the batch meta-data tables.
		 */
		private @Nullable String tablePrefix;

		/**
		 * Database schema initialization mode.
		 */
		private DatabaseInitializationMode initializeSchema = DatabaseInitializationMode.EMBEDDED;

		public boolean isValidateTransactionState() {
			return this.validateTransactionState;
		}

		public void setValidateTransactionState(boolean validateTransactionState) {
			this.validateTransactionState = validateTransactionState;
		}

		public @Nullable Isolation getIsolationLevelForCreate() {
			return this.isolationLevelForCreate;
		}

		public void setIsolationLevelForCreate(@Nullable Isolation isolationLevelForCreate) {
			this.isolationLevelForCreate = isolationLevelForCreate;
		}

		public String getSchema() {
			return this.schema;
		}

		public void setSchema(String schema) {
			this.schema = schema;
		}

		public @Nullable String getPlatform() {
			return this.platform;
		}

		public void setPlatform(@Nullable String platform) {
			this.platform = platform;
		}

		public @Nullable String getTablePrefix() {
			return this.tablePrefix;
		}

		public void setTablePrefix(@Nullable String tablePrefix) {
			this.tablePrefix = tablePrefix;
		}

		public DatabaseInitializationMode getInitializeSchema() {
			return this.initializeSchema;
		}

		public void setInitializeSchema(DatabaseInitializationMode initializeSchema) {
			this.initializeSchema = initializeSchema;
		}

	}

}
