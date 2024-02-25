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

package org.springframework.boot.autoconfigure.session;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.session.FlushMode;
import org.springframework.session.SaveMode;

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
	 * Platform to use in initialization scripts if the @@platform@@ placeholder is used.
	 * Auto-detected by default.
	 */
	private String platform;

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
	private DatabaseInitializationMode initializeSchema = DatabaseInitializationMode.EMBEDDED;

	/**
	 * Sessions flush mode. Determines when session changes are written to the session
	 * store.
	 */
	private FlushMode flushMode = FlushMode.ON_SAVE;

	/**
	 * Sessions save mode. Determines how session changes are tracked and saved to the
	 * session store.
	 */
	private SaveMode saveMode = SaveMode.ON_SET_ATTRIBUTE;

	/**
     * Returns the schema associated with this JdbcSessionProperties object.
     *
     * @return the schema associated with this JdbcSessionProperties object
     */
    public String getSchema() {
		return this.schema;
	}

	/**
     * Sets the schema for the JDBC session properties.
     * 
     * @param schema the schema to be set
     */
    public void setSchema(String schema) {
		this.schema = schema;
	}

	/**
     * Returns the platform of the JdbcSessionProperties.
     * 
     * @return the platform of the JdbcSessionProperties
     */
    public String getPlatform() {
		return this.platform;
	}

	/**
     * Sets the platform for the JDBC session properties.
     * 
     * @param platform the platform to set
     */
    public void setPlatform(String platform) {
		this.platform = platform;
	}

	/**
     * Returns the name of the table associated with this JdbcSessionProperties object.
     *
     * @return the name of the table
     */
    public String getTableName() {
		return this.tableName;
	}

	/**
     * Sets the name of the table to be used in the JDBC session.
     * 
     * @param tableName the name of the table
     */
    public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	/**
     * Returns the cleanup cron expression for the JDBC session properties.
     *
     * @return the cleanup cron expression
     */
    public String getCleanupCron() {
		return this.cleanupCron;
	}

	/**
     * Sets the cron expression for the cleanup task.
     * 
     * @param cleanupCron the cron expression for the cleanup task
     */
    public void setCleanupCron(String cleanupCron) {
		this.cleanupCron = cleanupCron;
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

	/**
     * Returns the flush mode of this JdbcSessionProperties object.
     * 
     * @return the flush mode of this JdbcSessionProperties object
     */
    public FlushMode getFlushMode() {
		return this.flushMode;
	}

	/**
     * Sets the flush mode for the JDBC session.
     * 
     * @param flushMode the flush mode to be set
     */
    public void setFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
	}

	/**
     * Returns the save mode for the JDBC session properties.
     * 
     * @return the save mode for the JDBC session properties
     */
    public SaveMode getSaveMode() {
		return this.saveMode;
	}

	/**
     * Sets the save mode for the JDBC session properties.
     * 
     * @param saveMode the save mode to be set
     */
    public void setSaveMode(SaveMode saveMode) {
		this.saveMode = saveMode;
	}

}
