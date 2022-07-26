/*
 * Copyright 2012-2022 the original author or authors.
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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

/**
 * Configuration properties for Flyway database migrations.
 *
 * @author Dave Syer
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Chris Bono
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.flyway")
public class FlywayProperties {

	/**
	 * Whether to enable flyway.
	 */
	private boolean enabled = true;

	/**
	 * Whether to fail if a location of migration scripts doesn't exist.
	 */
	private boolean failOnMissingLocations;

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
	 * Maximum time between retries when attempting to connect to the database. If a
	 * duration suffix is not specified, seconds will be used.
	 */
	@DurationUnit(ChronoUnit.SECONDS)
	private Duration connectRetriesInterval;

	/**
	 * Maximum number of retries when trying to obtain a lock.
	 */
	private Integer lockRetryCount;

	/**
	 * Default schema name managed by Flyway (case-sensitive).
	 */
	private String defaultSchema;

	/**
	 * Scheme names managed by Flyway (case-sensitive).
	 */
	private List<String> schemas = new ArrayList<>();

	/**
	 * Whether Flyway should attempt to create the schemas specified in the schemas
	 * property.
	 */
	private boolean createSchemas = true;

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
	 * Separator of default placeholders.
	 */
	private String placeholderSeparator;

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
	 * JDBC url of the database to migrate. If not set, the primary configured data source
	 * is used.
	 */
	private String url;

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
	private boolean cleanDisabled = true;

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
	 * Whether to batch SQL statements when executing them. Requires Flyway Teams.
	 */
	private Boolean batch;

	/**
	 * File to which the SQL statements of a migration dry run should be output. Requires
	 * Flyway Teams.
	 */
	private File dryRunOutput;

	/**
	 * Rules for the built-in error handling to override specific SQL states and error
	 * codes. Requires Flyway Teams.
	 */
	private String[] errorOverrides;

	/**
	 * Licence key for Flyway Teams.
	 */
	private String licenseKey;

	/**
	 * Whether to enable support for Oracle SQL*Plus commands. Requires Flyway Teams.
	 */
	private Boolean oracleSqlplus;

	/**
	 * Whether to issue a warning rather than an error when a not-yet-supported Oracle
	 * SQL*Plus statement is encountered. Requires Flyway Teams.
	 */
	private Boolean oracleSqlplusWarn;

	/**
	 * Whether to stream SQL migrations when executing them. Requires Flyway Teams.
	 */
	private Boolean stream;

	/**
	 * File name prefix for undo SQL migrations. Requires Flyway Teams.
	 */
	private String undoSqlMigrationPrefix;

	/**
	 * Migrations that Flyway should consider when migrating or undoing. When empty all
	 * available migrations are considered. Requires Flyway Teams.
	 */
	private String[] cherryPick;

	/**
	 * Properties to pass to the JDBC driver. Requires Flyway Teams.
	 */
	private Map<String, String> jdbcProperties = new HashMap<>();

	/**
	 * Path of the Kerberos config file. Requires Flyway Teams.
	 */
	private String kerberosConfigFile;

	/**
	 * Path of the Oracle Kerberos cache file. Requires Flyway Teams.
	 */
	private String oracleKerberosCacheFile;

	/**
	 * Location of the Oracle Wallet, used to sign in to the database automatically.
	 * Requires Flyway Teams.
	 */
	private String oracleWalletLocation;

	/**
	 * Whether Flyway should output a table with the results of queries when executing
	 * migrations. Requires Flyway Teams.
	 */
	private Boolean outputQueryResults;

	/**
	 * Path to the SQL Server Kerberos login file. Requires Flyway Teams.
	 */
	private String sqlServerKerberosLoginFile;

	/**
	 * Whether Flyway should skip executing the contents of the migrations and only update
	 * the schema history table. Requires Flyway teams.
	 */
	private Boolean skipExecutingMigrations;

	/**
	 * Ignore migrations that match this comma-separated list of patterns when validating
	 * migrations. Requires Flyway Teams.
	 */
	private List<String> ignoreMigrationPatterns;

	/**
	 * Whether to attempt to automatically detect SQL migration file encoding. Requires
	 * Flyway Teams.
	 */
	private Boolean detectEncoding;

	/**
	 * Prefix of placeholders in migration scripts.
	 */
	private String scriptPlaceholderPrefix;

	/**
	 * Suffix of placeholders in migration scripts.
	 */
	private String scriptPlaceholderSuffix;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isFailOnMissingLocations() {
		return this.failOnMissingLocations;
	}

	public void setFailOnMissingLocations(boolean failOnMissingLocations) {
		this.failOnMissingLocations = failOnMissingLocations;
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

	public Duration getConnectRetriesInterval() {
		return this.connectRetriesInterval;
	}

	public void setConnectRetriesInterval(Duration connectRetriesInterval) {
		this.connectRetriesInterval = connectRetriesInterval;
	}

	public Integer getLockRetryCount() {
		return this.lockRetryCount;
	}

	public void setLockRetryCount(Integer lockRetryCount) {
		this.lockRetryCount = lockRetryCount;
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

	public boolean isCreateSchemas() {
		return this.createSchemas;
	}

	public void setCreateSchemas(boolean createSchemas) {
		this.createSchemas = createSchemas;
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

	public String getPlaceholderSeparator() {
		return this.placeholderSeparator;
	}

	public void setPlaceholderSeparator(String placeholderSeparator) {
		this.placeholderSeparator = placeholderSeparator;
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

	public String getOracleWalletLocation() {
		return this.oracleWalletLocation;
	}

	public void setOracleWalletLocation(String oracleWalletLocation) {
		this.oracleWalletLocation = oracleWalletLocation;
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

	public String[] getCherryPick() {
		return this.cherryPick;
	}

	public void setCherryPick(String[] cherryPick) {
		this.cherryPick = cherryPick;
	}

	public Map<String, String> getJdbcProperties() {
		return this.jdbcProperties;
	}

	public void setJdbcProperties(Map<String, String> jdbcProperties) {
		this.jdbcProperties = jdbcProperties;
	}

	public String getKerberosConfigFile() {
		return this.kerberosConfigFile;
	}

	public void setKerberosConfigFile(String kerberosConfigFile) {
		this.kerberosConfigFile = kerberosConfigFile;
	}

	public String getOracleKerberosCacheFile() {
		return this.oracleKerberosCacheFile;
	}

	public void setOracleKerberosCacheFile(String oracleKerberosCacheFile) {
		this.oracleKerberosCacheFile = oracleKerberosCacheFile;
	}

	public Boolean getOutputQueryResults() {
		return this.outputQueryResults;
	}

	public void setOutputQueryResults(Boolean outputQueryResults) {
		this.outputQueryResults = outputQueryResults;
	}

	public String getSqlServerKerberosLoginFile() {
		return this.sqlServerKerberosLoginFile;
	}

	public void setSqlServerKerberosLoginFile(String sqlServerKerberosLoginFile) {
		this.sqlServerKerberosLoginFile = sqlServerKerberosLoginFile;
	}

	public Boolean getSkipExecutingMigrations() {
		return this.skipExecutingMigrations;
	}

	public void setSkipExecutingMigrations(Boolean skipExecutingMigrations) {
		this.skipExecutingMigrations = skipExecutingMigrations;
	}

	public List<String> getIgnoreMigrationPatterns() {
		return this.ignoreMigrationPatterns;
	}

	public void setIgnoreMigrationPatterns(List<String> ignoreMigrationPatterns) {
		this.ignoreMigrationPatterns = ignoreMigrationPatterns;
	}

	public Boolean getDetectEncoding() {
		return this.detectEncoding;
	}

	public void setDetectEncoding(final Boolean detectEncoding) {
		this.detectEncoding = detectEncoding;
	}

	public String getScriptPlaceholderPrefix() {
		return this.scriptPlaceholderPrefix;
	}

	public void setScriptPlaceholderPrefix(String scriptPlaceholderPrefix) {
		this.scriptPlaceholderPrefix = scriptPlaceholderPrefix;
	}

	public String getScriptPlaceholderSuffix() {
		return this.scriptPlaceholderSuffix;
	}

	public void setScriptPlaceholderSuffix(String scriptPlaceholderSuffix) {
		this.scriptPlaceholderSuffix = scriptPlaceholderSuffix;
	}

}
