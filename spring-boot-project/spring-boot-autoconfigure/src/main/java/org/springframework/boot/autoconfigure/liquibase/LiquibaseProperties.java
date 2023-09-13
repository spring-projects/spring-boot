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

package org.springframework.boot.autoconfigure.liquibase;

import java.io.File;
import java.util.Map;

import liquibase.integration.spring.SpringLiquibase;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * Configuration properties to configure {@link SpringLiquibase}.
 *
 * @author Marcel Overdijk
 * @author Eddú Meléndez
 * @author Ferenc Gratzer
 * @author Evgeniy Cheban
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.liquibase", ignoreUnknownFields = false)
public class LiquibaseProperties {

	/**
	 * Change log configuration path.
	 */
	private String changeLog = "classpath:/db/changelog/db.changelog-master.yaml";

	/**
	 * Whether to clear all checksums in the current changelog, so they will be
	 * recalculated upon the next update.
	 */
	private boolean clearChecksums;

	/**
	 * Comma-separated list of runtime contexts to use.
	 */
	private String contexts;

	/**
	 * Default database schema.
	 */
	private String defaultSchema;

	/**
	 * Schema to use for Liquibase objects.
	 */
	private String liquibaseSchema;

	/**
	 * Tablespace to use for Liquibase objects.
	 */
	private String liquibaseTablespace;

	/**
	 * Name of table to use for tracking change history.
	 */
	private String databaseChangeLogTable = "DATABASECHANGELOG";

	/**
	 * Name of table to use for tracking concurrent Liquibase usage.
	 */
	private String databaseChangeLogLockTable = "DATABASECHANGELOGLOCK";

	/**
	 * Whether to first drop the database schema.
	 */
	private boolean dropFirst;

	/**
	 * Whether to enable Liquibase support.
	 */
	private boolean enabled = true;

	/**
	 * Login user of the database to migrate.
	 */
	private String user;

	/**
	 * Login password of the database to migrate.
	 */
	private String password;

	/**
	 * Fully qualified name of the JDBC driver. Auto-detected based on the URL by default.
	 */
	private String driverClassName;

	/**
	 * JDBC URL of the database to migrate. If not set, the primary configured data source
	 * is used.
	 */
	private String url;

	/**
	 * Comma-separated list of runtime labels to use.
	 */
	private String labelFilter;

	/**
	 * Change log parameters.
	 */
	private Map<String, String> parameters;

	/**
	 * File to which rollback SQL is written when an update is performed.
	 */
	private File rollbackFile;

	/**
	 * Whether rollback should be tested before update is performed.
	 */
	private boolean testRollbackOnUpdate;

	/**
	 * Tag name to use when applying database changes. Can also be used with
	 * "rollbackFile" to generate a rollback script for all existing changes associated
	 * with that tag.
	 */
	private String tag;

	public String getChangeLog() {
		return this.changeLog;
	}

	public void setChangeLog(String changeLog) {
		Assert.notNull(changeLog, "ChangeLog must not be null");
		this.changeLog = changeLog;
	}

	public String getContexts() {
		return this.contexts;
	}

	public void setContexts(String contexts) {
		this.contexts = contexts;
	}

	public String getDefaultSchema() {
		return this.defaultSchema;
	}

	public void setDefaultSchema(String defaultSchema) {
		this.defaultSchema = defaultSchema;
	}

	public String getLiquibaseSchema() {
		return this.liquibaseSchema;
	}

	public void setLiquibaseSchema(String liquibaseSchema) {
		this.liquibaseSchema = liquibaseSchema;
	}

	public String getLiquibaseTablespace() {
		return this.liquibaseTablespace;
	}

	public void setLiquibaseTablespace(String liquibaseTablespace) {
		this.liquibaseTablespace = liquibaseTablespace;
	}

	public String getDatabaseChangeLogTable() {
		return this.databaseChangeLogTable;
	}

	public void setDatabaseChangeLogTable(String databaseChangeLogTable) {
		this.databaseChangeLogTable = databaseChangeLogTable;
	}

	public String getDatabaseChangeLogLockTable() {
		return this.databaseChangeLogLockTable;
	}

	public void setDatabaseChangeLogLockTable(String databaseChangeLogLockTable) {
		this.databaseChangeLogLockTable = databaseChangeLogLockTable;
	}

	public boolean isDropFirst() {
		return this.dropFirst;
	}

	public void setDropFirst(boolean dropFirst) {
		this.dropFirst = dropFirst;
	}

	public boolean isClearChecksums() {
		return this.clearChecksums;
	}

	public void setClearChecksums(boolean clearChecksums) {
		this.clearChecksums = clearChecksums;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDriverClassName() {
		return this.driverClassName;
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getLabelFilter() {
		return this.labelFilter;
	}

	public void setLabelFilter(String labelFilter) {
		this.labelFilter = labelFilter;
	}

	public Map<String, String> getParameters() {
		return this.parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public File getRollbackFile() {
		return this.rollbackFile;
	}

	public void setRollbackFile(File rollbackFile) {
		this.rollbackFile = rollbackFile;
	}

	public boolean isTestRollbackOnUpdate() {
		return this.testRollbackOnUpdate;
	}

	public void setTestRollbackOnUpdate(boolean testRollbackOnUpdate) {
		this.testRollbackOnUpdate = testRollbackOnUpdate;
	}

	public String getTag() {
		return this.tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

}
