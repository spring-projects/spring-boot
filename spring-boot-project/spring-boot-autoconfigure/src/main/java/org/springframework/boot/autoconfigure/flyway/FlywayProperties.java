/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.flyway;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Flyway database migrations.
 *
 * @author Dave Syer
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.flyway")
public class FlywayProperties {

	/**
	 * Whether to enable flyway.
	 */
	private boolean enabled = true;

	/**
	 * Whether to check that migration scripts location exists.
	 */
	private boolean checkLocation = true;

	/**
	 * Locations of migrations scripts. Can contain the special "{vendor}" placeholder to
	 * use vendor-specific locations.
	 */
	private List<String> locations = new ArrayList<>(Collections.singletonList("classpath:db/migration"));

	/**
	 * Encoding of SQL migrations.
	 */
	private Charset encoding = StandardCharsets.UTF_8;

	/**
	 * Maximum number of retries when attempting to connect to the database.
	 */
	private int connectRetries;

	/**
	 * Default schema name managed by Flyway (case-sensitive).
	 */
	private String defaultSchema;

	/**
	 * Scheme names managed by Flyway (case-sensitive).
	 */
	private List<String> schemas = new ArrayList<>();

	/**
	 * Name of the schema history table that will be used by Flyway.
	 */
	private String table = "flyway_schema_history";

	/**
	 * Tablespace in which the schema history table is created. Ignored when using a
	 * database that does not support tablespaces. Defaults to the default tablespace of
	 * the connection used by Flyway.
	 */
	private String tablespace;

	/**
	 * Description to tag an existing schema with when applying a baseline.
	 */
	private String baselineDescription = "<< Flyway Baseline >>";

	/**
	 * Version to tag an existing schema with when executing baseline.
	 */
	private String baselineVersion = "1";

	/**
	 * Username recorded in the schema history table as having applied the migration.
	 */
	private String installedBy;

	/**
	 * Placeholders and their replacements to apply to sql migration scripts.
	 */
	private Map<String, String> placeholders = new HashMap<>();

	/**
	 * Prefix of placeholders in migration scripts.
	 */
	private String placeholderPrefix = "${";

	/**
	 * Suffix of placeholders in migration scripts.
	 */
	private String placeholderSuffix = "}";

	/**
	 * Perform placeholder replacement in migration scripts.
	 */
	private boolean placeholderReplacement = true;

	/**
	 * File name prefix for SQL migrations.
	 */
	private String sqlMigrationPrefix = "V";

	/**
	 * File name suffix for SQL migrations.
	 */
	private List<String> sqlMigrationSuffixes = new ArrayList<>(Collections.singleton(".sql"));

	/**
	 * File name separator for SQL migrations.
	 */
	private String sqlMigrationSeparator = "__";

	/**
	 * File name prefix for repeatable SQL migrations.
	 */
	private String repeatableSqlMigrationPrefix = "R";

	/**
	 * Target version up to which migrations should be considered.
	 */
	private String target;

	/**
	 * JDBC url of the database to migrate. If not set, the primary configured data source
	 * is used.
	 */
	private String url;

	/**
	 * Login user of the database to migrate.
	 */
	private String user;

	/**
	 * Login password of the database to migrate.
	 */
	private String password;

	/**
	 * SQL statements to execute to initialize a connection immediately after obtaining
	 * it.
	 */
	private List<String> initSqls = new ArrayList<>();

	/**
	 * Whether to automatically call baseline when migrating a non-empty schema.
	 */
	private boolean baselineOnMigrate;

	/**
	 * Whether to disable cleaning of the database.
	 */
	private boolean cleanDisabled;

	/**
	 * Whether to automatically call clean when a validation error occurs.
	 */
	private boolean cleanOnValidationError;

	/**
	 * Whether to group all pending migrations together in the same transaction when
	 * applying them.
	 */
	private boolean group;

	/**
	 * Whether to ignore missing migrations when reading the schema history table.
	 */
	private boolean ignoreMissingMigrations;

	/**
	 * Whether to ignore ignored migrations when reading the schema history table.
	 */
	private boolean ignoreIgnoredMigrations;

	/**
	 * Whether to ignore pending migrations when reading the schema history table.
	 */
	private boolean ignorePendingMigrations;

	/**
	 * Whether to ignore future migrations when reading the schema history table.
	 */
	private boolean ignoreFutureMigrations = true;

	/**
	 * Whether to allow mixing transactional and non-transactional statements within the
	 * same migration.
	 */
	private boolean mixed;

	/**
	 * Whether to allow migrations to be run out of order.
	 */
	private boolean outOfOrder;

	/**
	 * Whether to skip default callbacks. If true, only custom callbacks are used.
	 */
	private boolean skipDefaultCallbacks;

	/**
	 * Whether to skip default resolvers. If true, only custom resolvers are used.
	 */
	private boolean skipDefaultResolvers;

	/**
	 * Whether to validate migrations and callbacks whose scripts do not obey the correct
	 * naming convention.
	 */
	private boolean validateMigrationNaming = false;

	/**
	 * Whether to automatically call validate when performing a migration.
	 */
	private boolean validateOnMigrate = true;

	/**
	 * Whether to batch SQL statements when executing them. Requires Flyway Pro or Flyway
	 * Enterprise.
	 */
	private Boolean batch;

	/**
	 * File to which the SQL statements of a migration dry run should be output. Requires
	 * Flyway Pro or Flyway Enterprise.
	 */
	private File dryRunOutput;

	/**
	 * Rules for the built-in error handling to override specific SQL states and error
	 * codes. Requires Flyway Pro or Flyway Enterprise.
	 */
	private String[] errorOverrides;

	/**
	 * Licence key for Flyway Pro or Flyway Enterprise.
	 */
	private String licenseKey;

	/**
	 * Whether to enable support for Oracle SQL*Plus commands. Requires Flyway Pro or
	 * Flyway Enterprise.
	 */
	private Boolean oracleSqlplus;

	/**
	 * Whether to issue a warning rather than an error when a not-yet-supported Oracle
	 * SQL*Plus statement is encountered. Requires Flyway Pro or Flyway Enterprise.
	 */
	private Boolean oracleSqlplusWarn;

	/**
	 * Whether to stream SQL migrations when executing them. Requires Flyway Pro or Flyway
	 * Enterprise.
	 */
	private Boolean stream;

	/**
	 * File name prefix for undo SQL migrations. Requires Flyway Pro or Flyway Enterprise.
	 */
	private String undoSqlMigrationPrefix;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isCheckLocation() {
		return this.checkLocation;
	}

	public void setCheckLocation(boolean checkLocation) {
		this.checkLocation = checkLocation;
	}

	public List<String> getLocations() {
		return this.locations;
	}

	public void setLocations(List<String> locations) {
		this.locations = locations;
	}

	public Charset getEncoding() {
		return this.encoding;
	}

	public void setEncoding(Charset encoding) {
		this.encoding = encoding;
	}

	public int getConnectRetries() {
		return this.connectRetries;
	}

	public void setConnectRetries(int connectRetries) {
		this.connectRetries = connectRetries;
	}

	public String getDefaultSchema() {
		return this.defaultSchema;
	}

	public void setDefaultSchema(String defaultSchema) {
		this.defaultSchema = defaultSchema;
	}

	public List<String> getSchemas() {
		return this.schemas;
	}

	public void setSchemas(List<String> schemas) {
		this.schemas = schemas;
	}

	public String getTable() {
		return this.table;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public String getTablespace() {
		return this.tablespace;
	}

	public void setTablespace(String tablespace) {
		this.tablespace = tablespace;
	}

	public String getBaselineDescription() {
		return this.baselineDescription;
	}

	public void setBaselineDescription(String baselineDescription) {
		this.baselineDescription = baselineDescription;
	}

	public String getBaselineVersion() {
		return this.baselineVersion;
	}

	public void setBaselineVersion(String baselineVersion) {
		this.baselineVersion = baselineVersion;
	}

	public String getInstalledBy() {
		return this.installedBy;
	}

	public void setInstalledBy(String installedBy) {
		this.installedBy = installedBy;
	}

	public Map<String, String> getPlaceholders() {
		return this.placeholders;
	}

	public void setPlaceholders(Map<String, String> placeholders) {
		this.placeholders = placeholders;
	}

	public String getPlaceholderPrefix() {
		return this.placeholderPrefix;
	}

	public void setPlaceholderPrefix(String placeholderPrefix) {
		this.placeholderPrefix = placeholderPrefix;
	}

	public String getPlaceholderSuffix() {
		return this.placeholderSuffix;
	}

	public void setPlaceholderSuffix(String placeholderSuffix) {
		this.placeholderSuffix = placeholderSuffix;
	}

	public boolean isPlaceholderReplacement() {
		return this.placeholderReplacement;
	}

	public void setPlaceholderReplacement(boolean placeholderReplacement) {
		this.placeholderReplacement = placeholderReplacement;
	}

	public String getSqlMigrationPrefix() {
		return this.sqlMigrationPrefix;
	}

	public void setSqlMigrationPrefix(String sqlMigrationPrefix) {
		this.sqlMigrationPrefix = sqlMigrationPrefix;
	}

	public List<String> getSqlMigrationSuffixes() {
		return this.sqlMigrationSuffixes;
	}

	public void setSqlMigrationSuffixes(List<String> sqlMigrationSuffixes) {
		this.sqlMigrationSuffixes = sqlMigrationSuffixes;
	}

	public String getSqlMigrationSeparator() {
		return this.sqlMigrationSeparator;
	}

	public void setSqlMigrationSeparator(String sqlMigrationSeparator) {
		this.sqlMigrationSeparator = sqlMigrationSeparator;
	}

	public String getRepeatableSqlMigrationPrefix() {
		return this.repeatableSqlMigrationPrefix;
	}

	public void setRepeatableSqlMigrationPrefix(String repeatableSqlMigrationPrefix) {
		this.repeatableSqlMigrationPrefix = repeatableSqlMigrationPrefix;
	}

	public String getTarget() {
		return this.target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public boolean isCreateDataSource() {
		return this.url != null || this.user != null;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return (this.password != null) ? this.password : "";
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public List<String> getInitSqls() {
		return this.initSqls;
	}

	public void setInitSqls(List<String> initSqls) {
		this.initSqls = initSqls;
	}

	public boolean isBaselineOnMigrate() {
		return this.baselineOnMigrate;
	}

	public void setBaselineOnMigrate(boolean baselineOnMigrate) {
		this.baselineOnMigrate = baselineOnMigrate;
	}

	public boolean isCleanDisabled() {
		return this.cleanDisabled;
	}

	public void setCleanDisabled(boolean cleanDisabled) {
		this.cleanDisabled = cleanDisabled;
	}

	public boolean isCleanOnValidationError() {
		return this.cleanOnValidationError;
	}

	public void setCleanOnValidationError(boolean cleanOnValidationError) {
		this.cleanOnValidationError = cleanOnValidationError;
	}

	public boolean isGroup() {
		return this.group;
	}

	public void setGroup(boolean group) {
		this.group = group;
	}

	public boolean isIgnoreMissingMigrations() {
		return this.ignoreMissingMigrations;
	}

	public void setIgnoreMissingMigrations(boolean ignoreMissingMigrations) {
		this.ignoreMissingMigrations = ignoreMissingMigrations;
	}

	public boolean isIgnoreIgnoredMigrations() {
		return this.ignoreIgnoredMigrations;
	}

	public void setIgnoreIgnoredMigrations(boolean ignoreIgnoredMigrations) {
		this.ignoreIgnoredMigrations = ignoreIgnoredMigrations;
	}

	public boolean isIgnorePendingMigrations() {
		return this.ignorePendingMigrations;
	}

	public void setIgnorePendingMigrations(boolean ignorePendingMigrations) {
		this.ignorePendingMigrations = ignorePendingMigrations;
	}

	public boolean isIgnoreFutureMigrations() {
		return this.ignoreFutureMigrations;
	}

	public void setIgnoreFutureMigrations(boolean ignoreFutureMigrations) {
		this.ignoreFutureMigrations = ignoreFutureMigrations;
	}

	public boolean isMixed() {
		return this.mixed;
	}

	public void setMixed(boolean mixed) {
		this.mixed = mixed;
	}

	public boolean isOutOfOrder() {
		return this.outOfOrder;
	}

	public void setOutOfOrder(boolean outOfOrder) {
		this.outOfOrder = outOfOrder;
	}

	public boolean isSkipDefaultCallbacks() {
		return this.skipDefaultCallbacks;
	}

	public void setSkipDefaultCallbacks(boolean skipDefaultCallbacks) {
		this.skipDefaultCallbacks = skipDefaultCallbacks;
	}

	public boolean isSkipDefaultResolvers() {
		return this.skipDefaultResolvers;
	}

	public void setSkipDefaultResolvers(boolean skipDefaultResolvers) {
		this.skipDefaultResolvers = skipDefaultResolvers;
	}

	public boolean isValidateMigrationNaming() {
		return this.validateMigrationNaming;
	}

	public void setValidateMigrationNaming(boolean validateMigrationNaming) {
		this.validateMigrationNaming = validateMigrationNaming;
	}

	public boolean isValidateOnMigrate() {
		return this.validateOnMigrate;
	}

	public void setValidateOnMigrate(boolean validateOnMigrate) {
		this.validateOnMigrate = validateOnMigrate;
	}

	public Boolean getBatch() {
		return this.batch;
	}

	public void setBatch(Boolean batch) {
		this.batch = batch;
	}

	public File getDryRunOutput() {
		return this.dryRunOutput;
	}

	public void setDryRunOutput(File dryRunOutput) {
		this.dryRunOutput = dryRunOutput;
	}

	public String[] getErrorOverrides() {
		return this.errorOverrides;
	}

	public void setErrorOverrides(String[] errorOverrides) {
		this.errorOverrides = errorOverrides;
	}

	public String getLicenseKey() {
		return this.licenseKey;
	}

	public void setLicenseKey(String licenseKey) {
		this.licenseKey = licenseKey;
	}

	public Boolean getOracleSqlplus() {
		return this.oracleSqlplus;
	}

	public void setOracleSqlplus(Boolean oracleSqlplus) {
		this.oracleSqlplus = oracleSqlplus;
	}

	public Boolean getOracleSqlplusWarn() {
		return this.oracleSqlplusWarn;
	}

	public void setOracleSqlplusWarn(Boolean oracleSqlplusWarn) {
		this.oracleSqlplusWarn = oracleSqlplusWarn;
	}

	public Boolean getStream() {
		return this.stream;
	}

	public void setStream(Boolean stream) {
		this.stream = stream;
	}

	public String getUndoSqlMigrationPrefix() {
		return this.undoSqlMigrationPrefix;
	}

	public void setUndoSqlMigrationPrefix(String undoSqlMigrationPrefix) {
		this.undoSqlMigrationPrefix = undoSqlMigrationPrefix;
	}

}
