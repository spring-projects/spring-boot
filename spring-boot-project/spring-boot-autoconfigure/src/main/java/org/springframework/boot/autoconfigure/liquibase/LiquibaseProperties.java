/*
 * Copyright 2012-2024 the original author or authors.
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

import liquibase.UpdateSummaryEnum;
import liquibase.UpdateSummaryOutputEnum;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.ui.UIServiceEnum;

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

	/**
	 * Whether to print a summary of the update operation.
	 */
	private ShowSummary showSummary;

	/**
	 * Where to print a summary of the update operation.
	 */
	private ShowSummaryOutput showSummaryOutput;

	/**
	 * Which UIService to use.
	 */
	private UIService uiService;

	/**
	 * Returns the change log associated with the LiquibaseProperties object.
	 * @return the change log
	 */
	public String getChangeLog() {
		return this.changeLog;
	}

	/**
	 * Sets the change log for Liquibase.
	 * @param changeLog the change log to be set
	 * @throws IllegalArgumentException if the change log is null
	 */
	public void setChangeLog(String changeLog) {
		Assert.notNull(changeLog, "ChangeLog must not be null");
		this.changeLog = changeLog;
	}

	/**
	 * Returns the contexts of the LiquibaseProperties.
	 * @return the contexts of the LiquibaseProperties
	 */
	public String getContexts() {
		return this.contexts;
	}

	/**
	 * Sets the contexts for Liquibase.
	 * @param contexts the contexts to set
	 */
	public void setContexts(String contexts) {
		this.contexts = contexts;
	}

	/**
	 * Returns the default schema.
	 * @return the default schema
	 */
	public String getDefaultSchema() {
		return this.defaultSchema;
	}

	/**
	 * Sets the default schema for the Liquibase properties.
	 * @param defaultSchema the default schema to be set
	 */
	public void setDefaultSchema(String defaultSchema) {
		this.defaultSchema = defaultSchema;
	}

	/**
	 * Returns the Liquibase schema.
	 * @return the Liquibase schema
	 */
	public String getLiquibaseSchema() {
		return this.liquibaseSchema;
	}

	/**
	 * Sets the Liquibase schema.
	 * @param liquibaseSchema the Liquibase schema to set
	 */
	public void setLiquibaseSchema(String liquibaseSchema) {
		this.liquibaseSchema = liquibaseSchema;
	}

	/**
	 * Returns the Liquibase tablespace.
	 * @return the Liquibase tablespace
	 */
	public String getLiquibaseTablespace() {
		return this.liquibaseTablespace;
	}

	/**
	 * Sets the Liquibase tablespace.
	 * @param liquibaseTablespace the Liquibase tablespace to set
	 */
	public void setLiquibaseTablespace(String liquibaseTablespace) {
		this.liquibaseTablespace = liquibaseTablespace;
	}

	/**
	 * Returns the name of the database change log table.
	 * @return the name of the database change log table
	 */
	public String getDatabaseChangeLogTable() {
		return this.databaseChangeLogTable;
	}

	/**
	 * Sets the name of the database change log table.
	 * @param databaseChangeLogTable the name of the database change log table
	 */
	public void setDatabaseChangeLogTable(String databaseChangeLogTable) {
		this.databaseChangeLogTable = databaseChangeLogTable;
	}

	/**
	 * Returns the name of the database change log lock table.
	 * @return the name of the database change log lock table
	 */
	public String getDatabaseChangeLogLockTable() {
		return this.databaseChangeLogLockTable;
	}

	/**
	 * Sets the name of the database change log lock table.
	 * @param databaseChangeLogLockTable the name of the database change log lock table
	 */
	public void setDatabaseChangeLogLockTable(String databaseChangeLogLockTable) {
		this.databaseChangeLogLockTable = databaseChangeLogLockTable;
	}

	/**
	 * Returns a boolean value indicating whether the first change set should be dropped.
	 * @return true if the first change set should be dropped, false otherwise
	 */
	public boolean isDropFirst() {
		return this.dropFirst;
	}

	/**
	 * Sets the flag indicating whether to drop the first change set when applying
	 * changes.
	 * @param dropFirst true to drop the first change set, false otherwise
	 */
	public void setDropFirst(boolean dropFirst) {
		this.dropFirst = dropFirst;
	}

	/**
	 * Returns a boolean value indicating whether the checksums should be cleared.
	 * @return {@code true} if the checksums should be cleared, {@code false} otherwise
	 */
	public boolean isClearChecksums() {
		return this.clearChecksums;
	}

	/**
	 * Sets the flag to clear checksums.
	 * @param clearChecksums the flag indicating whether to clear checksums
	 */
	public void setClearChecksums(boolean clearChecksums) {
		this.clearChecksums = clearChecksums;
	}

	/**
	 * Returns the current status of the enabled flag.
	 * @return {@code true} if the enabled flag is set to true, {@code false} otherwise.
	 */
	public boolean isEnabled() {
		return this.enabled;
	}

	/**
	 * Sets the enabled status of the LiquibaseProperties.
	 * @param enabled the enabled status to be set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Returns the user associated with the Liquibase properties.
	 * @return the user associated with the Liquibase properties
	 */
	public String getUser() {
		return this.user;
	}

	/**
	 * Sets the user for the LiquibaseProperties.
	 * @param user the user to set
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * Returns the password.
	 * @return the password
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Sets the password for the LiquibaseProperties object.
	 * @param password the password to be set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Returns the driver class name.
	 * @return the driver class name
	 */
	public String getDriverClassName() {
		return this.driverClassName;
	}

	/**
	 * Sets the driver class name for the Liquibase properties.
	 * @param driverClassName the driver class name to be set
	 */
	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	/**
	 * Returns the URL of the Liquibase database connection.
	 * @return the URL of the Liquibase database connection
	 */
	public String getUrl() {
		return this.url;
	}

	/**
	 * Sets the URL for the Liquibase database connection.
	 * @param url the URL to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Returns the label filter used for Liquibase changesets.
	 * @return the label filter
	 */
	public String getLabelFilter() {
		return this.labelFilter;
	}

	/**
	 * Sets the label filter for Liquibase properties.
	 * @param labelFilter the label filter to be set
	 */
	public void setLabelFilter(String labelFilter) {
		this.labelFilter = labelFilter;
	}

	/**
	 * Returns the parameters of the LiquibaseProperties.
	 * @return a Map containing the parameters of the LiquibaseProperties
	 */
	public Map<String, String> getParameters() {
		return this.parameters;
	}

	/**
	 * Sets the parameters for the LiquibaseProperties.
	 * @param parameters a Map containing the parameters to be set
	 */
	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	/**
	 * Returns the rollback file associated with the LiquibaseProperties object.
	 * @return the rollback file
	 */
	public File getRollbackFile() {
		return this.rollbackFile;
	}

	/**
	 * Sets the rollback file for Liquibase.
	 * @param rollbackFile the rollback file to be set
	 */
	public void setRollbackFile(File rollbackFile) {
		this.rollbackFile = rollbackFile;
	}

	/**
	 * Returns the value indicating whether the test rollback on update is enabled or not.
	 * @return {@code true} if the test rollback on update is enabled, {@code false}
	 * otherwise.
	 */
	public boolean isTestRollbackOnUpdate() {
		return this.testRollbackOnUpdate;
	}

	/**
	 * Sets the flag indicating whether to test rollback on update.
	 * @param testRollbackOnUpdate the flag indicating whether to test rollback on update
	 */
	public void setTestRollbackOnUpdate(boolean testRollbackOnUpdate) {
		this.testRollbackOnUpdate = testRollbackOnUpdate;
	}

	/**
	 * Returns the tag associated with the LiquibaseProperties object.
	 * @return the tag associated with the LiquibaseProperties object
	 */
	public String getTag() {
		return this.tag;
	}

	/**
	 * Sets the tag for the LiquibaseProperties.
	 * @param tag the tag to be set
	 */
	public void setTag(String tag) {
		this.tag = tag;
	}

	/**
	 * Returns the show summary of the LiquibaseProperties.
	 * @return the show summary of the LiquibaseProperties
	 */
	public ShowSummary getShowSummary() {
		return this.showSummary;
	}

	/**
	 * Sets the show summary for the LiquibaseProperties.
	 * @param showSummary the show summary to be set
	 */
	public void setShowSummary(ShowSummary showSummary) {
		this.showSummary = showSummary;
	}

	/**
	 * Returns the ShowSummaryOutput object.
	 * @return the ShowSummaryOutput object
	 */
	public ShowSummaryOutput getShowSummaryOutput() {
		return this.showSummaryOutput;
	}

	/**
	 * Sets the ShowSummaryOutput object.
	 * @param showSummaryOutput the ShowSummaryOutput object to be set
	 */
	public void setShowSummaryOutput(ShowSummaryOutput showSummaryOutput) {
		this.showSummaryOutput = showSummaryOutput;
	}

	/**
	 * Returns the UI service associated with this LiquibaseProperties instance.
	 * @return the UI service
	 */
	public UIService getUiService() {
		return this.uiService;
	}

	/**
	 * Sets the UI service for the LiquibaseProperties class.
	 * @param uiService the UI service to be set
	 */
	public void setUiService(UIService uiService) {
		this.uiService = uiService;
	}

	/**
	 * Enumeration of types of summary to show. Values are the same as those on
	 * {@link UpdateSummaryEnum}. To maximize backwards compatibility, the Liquibase enum
	 * is not used directly.
	 *
	 * @since 3.2.1
	 */
	public enum ShowSummary {

		/**
		 * Do not show a summary.
		 */
		OFF,

		/**
		 * Show a summary.
		 */
		SUMMARY,

		/**
		 * Show a verbose summary.
		 */
		VERBOSE

	}

	/**
	 * Enumeration of destinations to which the summary should be output. Values are the
	 * same as those on {@link UpdateSummaryOutputEnum}. To maximize backwards
	 * compatibility, the Liquibase enum is not used directly.
	 *
	 * @since 3.2.1
	 */
	public enum ShowSummaryOutput {

		/**
		 * Log the summary.
		 */
		LOG,

		/**
		 * Output the summary to the console.
		 */
		CONSOLE,

		/**
		 * Log the summary and output it to the console.
		 */
		ALL

	}

	/**
	 * Enumeration of types of UIService. Values are the same as those on
	 * {@link UIServiceEnum}. To maximize backwards compatibility, the Liquibase enum is
	 * not used directly.
	 *
	 * @since 3.3.0
	 */
	public enum UIService {

		/**
		 * Console-based UIService.
		 */
		CONSOLE,

		/**
		 * Logging-based UIService.
		 */
		LOGGER

	}

}
