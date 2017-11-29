/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.session;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceInitializationMode;

/**
 * Configuration properties for JDBC backed Spring Session.
 *
 * @author Vedran Pavic
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.session.jdbc")
public class JdbcSessionProperties {

	private static final String DEFAULT_SCHEMA_LOCATION = "classpath:org/springframework/"
			+ "session/jdbc/schema-@@platform@@.sql";

	private static final String DEFAULT_TABLE_NAME = "SPRING_SESSION";

	private static final String DEFAULT_CLEANUP_CRON = "0 * * * * *";

	/**
	 * Path to the SQL file to use to initialize the database schema.
	 */
	private String schema = DEFAULT_SCHEMA_LOCATION;

	/**
	 * Name of the database table used to store sessions.
	 */
	private String tableName = DEFAULT_TABLE_NAME;

	/**
	 * Cron expression for expired session cleanup job.
	 */
	private String cleanupCron = DEFAULT_CLEANUP_CRON;

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

	public String getTableName() {
		return this.tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getCleanupCron() {
		return this.cleanupCron;
	}

	public void setCleanupCron(String cleanupCron) {
		this.cleanupCron = cleanupCron;
	}

	public DataSourceInitializationMode getInitializeSchema() {
		return this.initializeSchema;
	}

	public void setInitializeSchema(DataSourceInitializationMode initializeSchema) {
		this.initializeSchema = initializeSchema;
	}

}
