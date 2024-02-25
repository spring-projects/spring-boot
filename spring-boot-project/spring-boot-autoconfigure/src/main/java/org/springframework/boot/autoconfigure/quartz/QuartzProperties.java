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

package org.springframework.boot.autoconfigure.quartz;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.sql.init.DatabaseInitializationMode;

/**
 * Configuration properties for the Quartz Scheduler integration.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties("spring.quartz")
public class QuartzProperties {

	/**
	 * Quartz job store type.
	 */
	private JobStoreType jobStoreType = JobStoreType.MEMORY;

	/**
	 * Name of the scheduler.
	 */
	private String schedulerName;

	/**
	 * Whether to automatically start the scheduler after initialization.
	 */
	private boolean autoStartup = true;

	/**
	 * Delay after which the scheduler is started once initialization completes. Setting
	 * this property makes sense if no jobs should be run before the entire application
	 * has started up.
	 */
	private Duration startupDelay = Duration.ofSeconds(0);

	/**
	 * Whether to wait for running jobs to complete on shutdown.
	 */
	private boolean waitForJobsToCompleteOnShutdown = false;

	/**
	 * Whether configured jobs should overwrite existing job definitions.
	 */
	private boolean overwriteExistingJobs = false;

	/**
	 * Additional Quartz Scheduler properties.
	 */
	private final Map<String, String> properties = new HashMap<>();

	private final Jdbc jdbc = new Jdbc();

	/**
	 * Returns the type of job store used by the Quartz scheduler.
	 * @return the job store type
	 */
	public JobStoreType getJobStoreType() {
		return this.jobStoreType;
	}

	/**
	 * Sets the type of job store for Quartz.
	 * @param jobStoreType the type of job store to be set
	 */
	public void setJobStoreType(JobStoreType jobStoreType) {
		this.jobStoreType = jobStoreType;
	}

	/**
	 * Returns the name of the scheduler.
	 * @return the name of the scheduler
	 */
	public String getSchedulerName() {
		return this.schedulerName;
	}

	/**
	 * Sets the name of the scheduler.
	 * @param schedulerName the name of the scheduler
	 */
	public void setSchedulerName(String schedulerName) {
		this.schedulerName = schedulerName;
	}

	/**
	 * Returns a boolean value indicating whether the auto startup feature is enabled or
	 * not.
	 * @return true if auto startup is enabled, false otherwise
	 */
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * Sets the flag indicating whether the Quartz scheduler should automatically start
	 * when the application starts.
	 * @param autoStartup the flag indicating whether the Quartz scheduler should
	 * automatically start
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * Returns the startup delay for the QuartzProperties.
	 * @return the startup delay for the QuartzProperties
	 */
	public Duration getStartupDelay() {
		return this.startupDelay;
	}

	/**
	 * Sets the startup delay for the Quartz scheduler.
	 * @param startupDelay the duration of the startup delay
	 */
	public void setStartupDelay(Duration startupDelay) {
		this.startupDelay = startupDelay;
	}

	/**
	 * Returns the value indicating whether the system should wait for jobs to complete on
	 * shutdown.
	 * @return {@code true} if the system should wait for jobs to complete on shutdown,
	 * {@code false} otherwise
	 */
	public boolean isWaitForJobsToCompleteOnShutdown() {
		return this.waitForJobsToCompleteOnShutdown;
	}

	/**
	 * Sets whether to wait for jobs to complete on shutdown.
	 * @param waitForJobsToCompleteOnShutdown true to wait for jobs to complete on
	 * shutdown, false otherwise
	 */
	public void setWaitForJobsToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
		this.waitForJobsToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
	}

	/**
	 * Returns a boolean value indicating whether existing jobs should be overwritten.
	 * @return true if existing jobs should be overwritten, false otherwise
	 */
	public boolean isOverwriteExistingJobs() {
		return this.overwriteExistingJobs;
	}

	/**
	 * Sets the flag indicating whether existing jobs should be overwritten.
	 * @param overwriteExistingJobs true if existing jobs should be overwritten, false
	 * otherwise
	 */
	public void setOverwriteExistingJobs(boolean overwriteExistingJobs) {
		this.overwriteExistingJobs = overwriteExistingJobs;
	}

	/**
	 * Returns the properties of the QuartzProperties object.
	 * @return a Map containing the properties as key-value pairs
	 */
	public Map<String, String> getProperties() {
		return this.properties;
	}

	/**
	 * Returns the Jdbc object associated with this QuartzProperties instance.
	 * @return the Jdbc object
	 */
	public Jdbc getJdbc() {
		return this.jdbc;
	}

	/**
	 * Jdbc class.
	 */
	public static class Jdbc {

		private static final String DEFAULT_SCHEMA_LOCATION = "classpath:org/quartz/impl/"
				+ "jdbcjobstore/tables_@@platform@@.sql";

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
		 * Database schema initialization mode.
		 */
		private DatabaseInitializationMode initializeSchema = DatabaseInitializationMode.EMBEDDED;

		/**
		 * Prefixes for single-line comments in SQL initialization scripts.
		 */
		private List<String> commentPrefix = new ArrayList<>(Arrays.asList("#", "--"));

		/**
		 * Returns the schema of the Jdbc object.
		 * @return the schema of the Jdbc object
		 */
		public String getSchema() {
			return this.schema;
		}

		/**
		 * Sets the schema for the JDBC connection.
		 * @param schema the name of the schema to be set
		 */
		public void setSchema(String schema) {
			this.schema = schema;
		}

		/**
		 * Returns the platform of the JDBC connection.
		 * @return the platform of the JDBC connection
		 */
		public String getPlatform() {
			return this.platform;
		}

		/**
		 * Sets the platform for the JDBC connection.
		 * @param platform the platform to be set
		 */
		public void setPlatform(String platform) {
			this.platform = platform;
		}

		/**
		 * Returns the initialization mode for the database schema.
		 * @return the initialization mode for the database schema
		 */
		public DatabaseInitializationMode getInitializeSchema() {
			return this.initializeSchema;
		}

		/**
		 * Sets the mode for initializing the database schema.
		 * @param initializeSchema the mode for initializing the database schema
		 */
		public void setInitializeSchema(DatabaseInitializationMode initializeSchema) {
			this.initializeSchema = initializeSchema;
		}

		/**
		 * Returns the list of comment prefixes.
		 * @return the list of comment prefixes
		 */
		public List<String> getCommentPrefix() {
			return this.commentPrefix;
		}

		/**
		 * Sets the comment prefix for the Jdbc class.
		 * @param commentPrefix the list of comment prefixes to be set
		 */
		public void setCommentPrefix(List<String> commentPrefix) {
			this.commentPrefix = commentPrefix;
		}

	}

}
