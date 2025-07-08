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

package org.springframework.boot.quartz.autoconfigure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.sql.init.DatabaseInitializationMode;

/**
 * Configuration properties for the Quartz Scheduler integration when using a JDBC job
 * store.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@ConfigurationProperties("spring.quartz.jdbc")
public class QuartzJdbcProperties {

	private static final String DEFAULT_SCHEMA_LOCATION = "classpath:org/quartz/impl/"
			+ "jdbcjobstore/tables_@@platform@@.sql";

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
	 * Database schema initialization mode.
	 */
	private DatabaseInitializationMode initializeSchema = DatabaseInitializationMode.EMBEDDED;

	/**
	 * Prefixes for single-line comments in SQL initialization scripts.
	 */
	private List<String> commentPrefix = new ArrayList<>(Arrays.asList("#", "--"));

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

	public DatabaseInitializationMode getInitializeSchema() {
		return this.initializeSchema;
	}

	public void setInitializeSchema(DatabaseInitializationMode initializeSchema) {
		this.initializeSchema = initializeSchema;
	}

	public List<String> getCommentPrefix() {
		return this.commentPrefix;
	}

	public void setCommentPrefix(List<String> commentPrefix) {
		this.commentPrefix = commentPrefix;
	}

}
