/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.quartz;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceInitializationMode;

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
	 * Additional Quartz Scheduler properties.
	 */
	private final Map<String, String> properties = new HashMap<>();

	private final Jdbc jdbc = new Jdbc();

	public JobStoreType getJobStoreType() {
		return this.jobStoreType;
	}

	public void setJobStoreType(JobStoreType jobStoreType) {
		this.jobStoreType = jobStoreType;
	}

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public Jdbc getJdbc() {
		return this.jdbc;
	}

	public static class Jdbc {

		private static final String DEFAULT_SCHEMA_LOCATION = "classpath:org/quartz/impl/"
				+ "jdbcjobstore/tables_@@platform@@.sql";

		/**
		 * Path to the SQL file to use to initialize the database schema.
		 */
		private String schema = DEFAULT_SCHEMA_LOCATION;

		/**
		 * Database schema initialization mode.
		 */
		private DataSourceInitializationMode initializeSchema = DataSourceInitializationMode.EMBEDDED;

		/**
		 * Prefix for single-line comments in SQL initialization scripts.
		 */
		private String commentPrefix = "--";

		public String getSchema() {
			return this.schema;
		}

		public void setSchema(String schema) {
			this.schema = schema;
		}

		public DataSourceInitializationMode getInitializeSchema() {
			return this.initializeSchema;
		}

		public void setInitializeSchema(DataSourceInitializationMode initializeSchema) {
			this.initializeSchema = initializeSchema;
		}

		public String getCommentPrefix() {
			return this.commentPrefix;
		}

		public void setCommentPrefix(String commentPrefix) {
			this.commentPrefix = commentPrefix;
		}

	}

}
