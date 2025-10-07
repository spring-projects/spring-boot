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

package org.springframework.boot.liquibase.autoconfigure;

import java.io.File;
import java.util.List;
import java.util.Map;

import liquibase.UpdateSummaryEnum;
import liquibase.UpdateSummaryOutputEnum;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.ui.UIServiceEnum;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * Configuration properties to configure {@link SpringLiquibase}.
 *
 * @author Marcel Overdijk
 * @author Eddú Meléndez
 * @author Ferenc Gratzer
 * @author Evgeniy Cheban
 * @since 4.0.0
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
	 * List of runtime contexts to use.
	 */
	private @Nullable List<String> contexts;

	/**
	 * Default database schema.
	 */
	private @Nullable String defaultSchema;

	/**
	 * Schema to use for Liquibase objects.
	 */
	private @Nullable String liquibaseSchema;

	/**
	 * Tablespace to use for Liquibase objects.
	 */
	private @Nullable String liquibaseTablespace;

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
	private @Nullable String user;

	/**
	 * Login password of the database to migrate.
	 */
	private @Nullable String password;

	/**
	 * Fully qualified name of the JDBC driver. Auto-detected based on the URL by default.
	 */
	private @Nullable String driverClassName;

	/**
	 * JDBC URL of the database to migrate. If not set, the primary configured data source
	 * is used.
	 */
	private @Nullable String url;

	/**
	 * List of runtime labels to use.
	 */
	private @Nullable List<String> labelFilter;

	/**
	 * Change log parameters.
	 */
	private @Nullable Map<String, String> parameters;

	/**
	 * Liquibase global configuration properties. Properties must be set with liquibase's
	 * full propertiesFile dot format. For example:
	 * {@code spring.liquibase.properties.liquibase.duplicateFileMode}. Note that
	 * Liquibase’s normal precedence still applies (env variables and jvm system
	 * properties can override values set here).
	 */
	private @Nullable Map<String, String> properties;

	/**
	 * File to which rollback SQL is written when an update is performed.
	 */
	private @Nullable File rollbackFile;

	/**
	 * Whether rollback should be tested before update is performed.
	 */
	private boolean testRollbackOnUpdate;

	/**
	 * Tag name to use when applying database changes. Can also be used with
	 * "rollbackFile" to generate a rollback script for all existing changes associated
	 * with that tag.
	 */
	private @Nullable String tag;

	/**
	 * Whether to print a summary of the update operation.
	 */
	private @Nullable ShowSummary showSummary;

	/**
	 * Where to print a summary of the update operation.
	 */
	private @Nullable ShowSummaryOutput showSummaryOutput;

	/**
	 * Which UIService to use.
	 */
	private @Nullable UiService uiService;

	/**
	 * Whether to send product usage data and analytics to Liquibase.
	 */
	private @Nullable Boolean analyticsEnabled;

	/**
	 * Liquibase Pro license key.
	 */
	private @Nullable String licenseKey;

	public String getChangeLog() {
		return this.changeLog;
	}

	public void setChangeLog(String changeLog) {
		Assert.notNull(changeLog, "'changeLog' must not be null");
		this.changeLog = changeLog;
	}

	public @Nullable List<String> getContexts() {
		return this.contexts;
	}

	public void setContexts(@Nullable List<String> contexts) {
		this.contexts = contexts;
	}

	public @Nullable String getDefaultSchema() {
		return this.defaultSchema;
	}

	public void setDefaultSchema(@Nullable String defaultSchema) {
		this.defaultSchema = defaultSchema;
	}

	public @Nullable String getLiquibaseSchema() {
		return this.liquibaseSchema;
	}

	public void setLiquibaseSchema(@Nullable String liquibaseSchema) {
		this.liquibaseSchema = liquibaseSchema;
	}

	public @Nullable String getLiquibaseTablespace() {
		return this.liquibaseTablespace;
	}

	public void setLiquibaseTablespace(@Nullable String liquibaseTablespace) {
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

	public @Nullable String getUser() {
		return this.user;
	}

	public void setUser(@Nullable String user) {
		this.user = user;
	}

	public @Nullable String getPassword() {
		return this.password;
	}

	public void setPassword(@Nullable String password) {
		this.password = password;
	}

	public @Nullable String getDriverClassName() {
		return this.driverClassName;
	}

	public void setDriverClassName(@Nullable String driverClassName) {
		this.driverClassName = driverClassName;
	}

	public @Nullable String getUrl() {
		return this.url;
	}

	public void setUrl(@Nullable String url) {
		this.url = url;
	}

	public @Nullable List<String> getLabelFilter() {
		return this.labelFilter;
	}

	public void setLabelFilter(@Nullable List<String> labelFilter) {
		this.labelFilter = labelFilter;
	}

	public @Nullable Map<String, String> getParameters() {
		return this.parameters;
	}

	public void setParameters(@Nullable Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public @Nullable Map<String, String> getProperties() {
		return this.properties;
	}

	public void setProperties(@Nullable Map<String, String> properties) {
		this.properties = properties;
	}

	public @Nullable File getRollbackFile() {
		return this.rollbackFile;
	}

	public void setRollbackFile(@Nullable File rollbackFile) {
		this.rollbackFile = rollbackFile;
	}

	public boolean isTestRollbackOnUpdate() {
		return this.testRollbackOnUpdate;
	}

	public void setTestRollbackOnUpdate(boolean testRollbackOnUpdate) {
		this.testRollbackOnUpdate = testRollbackOnUpdate;
	}

	public @Nullable String getTag() {
		return this.tag;
	}

	public void setTag(@Nullable String tag) {
		this.tag = tag;
	}

	public @Nullable ShowSummary getShowSummary() {
		return this.showSummary;
	}

	public void setShowSummary(@Nullable ShowSummary showSummary) {
		this.showSummary = showSummary;
	}

	public @Nullable ShowSummaryOutput getShowSummaryOutput() {
		return this.showSummaryOutput;
	}

	public void setShowSummaryOutput(@Nullable ShowSummaryOutput showSummaryOutput) {
		this.showSummaryOutput = showSummaryOutput;
	}

	public @Nullable UiService getUiService() {
		return this.uiService;
	}

	public void setUiService(@Nullable UiService uiService) {
		this.uiService = uiService;
	}

	public @Nullable Boolean getAnalyticsEnabled() {
		return this.analyticsEnabled;
	}

	public void setAnalyticsEnabled(@Nullable Boolean analyticsEnabled) {
		this.analyticsEnabled = analyticsEnabled;
	}

	public @Nullable String getLicenseKey() {
		return this.licenseKey;
	}

	public void setLicenseKey(@Nullable String licenseKey) {
		this.licenseKey = licenseKey;
	}

	/**
	 * Enumeration of types of summary to show. Values are the same as those on
	 * {@link UpdateSummaryEnum}. To maximize backwards compatibility, the Liquibase enum
	 * is not used directly.
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
	 */
	public enum UiService {

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
