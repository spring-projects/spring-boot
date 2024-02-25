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
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
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
	private Duration connectRetriesInterval = Duration.ofSeconds(120);

	/**
	 * Maximum number of retries when trying to obtain a lock.
	 */
	private int lockRetryCount = 50;

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
	private String placeholderSeparator = ":";

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
	private String target = "latest";

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
	 * Prefix of placeholders in migration scripts.
	 */
	private String scriptPlaceholderPrefix = "FP__";

	/**
	 * Suffix of placeholders in migration scripts.
	 */
	private String scriptPlaceholderSuffix = "__";

	/**
	 * Whether Flyway should execute SQL within a transaction.
	 */
	private boolean executeInTransaction = true;

	/**
	 * Loggers Flyway should use.
	 */
	private String[] loggers = { "slf4j" };

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
	 * Whether Flyway should output a table with the results of queries when executing
	 * migrations. Requires Flyway Teams.
	 */
	private Boolean outputQueryResults;

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

	private final Oracle oracle = new Oracle();

	private final Postgresql postgresql = new Postgresql();

	private final Sqlserver sqlserver = new Sqlserver();

	/**
     * Returns the current status of the enabled flag.
     *
     * @return true if the enabled flag is set to true, false otherwise
     */
    public boolean isEnabled() {
		return this.enabled;
	}

	/**
     * Sets the enabled status of the FlywayProperties.
     * 
     * @param enabled the enabled status to set
     */
    public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
     * Returns the value indicating whether the execution should fail if any locations are missing.
     *
     * @return {@code true} if the execution should fail on missing locations, {@code false} otherwise
     */
    public boolean isFailOnMissingLocations() {
		return this.failOnMissingLocations;
	}

	/**
     * Sets the flag indicating whether to fail if missing locations are encountered.
     * 
     * @param failOnMissingLocations true to fail if missing locations are encountered, false otherwise
     */
    public void setFailOnMissingLocations(boolean failOnMissingLocations) {
		this.failOnMissingLocations = failOnMissingLocations;
	}

	/**
     * Returns the list of locations.
     *
     * @return the list of locations
     */
    public List<String> getLocations() {
		return this.locations;
	}

	/**
     * Sets the list of locations for Flyway migration.
     * 
     * @param locations the list of locations to set
     */
    public void setLocations(List<String> locations) {
		this.locations = locations;
	}

	/**
     * Returns the encoding used by Flyway for reading and writing SQL scripts.
     *
     * @return The encoding used by Flyway.
     */
    public Charset getEncoding() {
		return this.encoding;
	}

	/**
     * Sets the encoding for Flyway.
     * 
     * @param encoding the encoding to be set
     */
    public void setEncoding(Charset encoding) {
		this.encoding = encoding;
	}

	/**
     * Returns the number of connection retries.
     *
     * @return the number of connection retries
     */
    public int getConnectRetries() {
		return this.connectRetries;
	}

	/**
     * Sets the number of connection retries.
     * 
     * @param connectRetries the number of connection retries to set
     */
    public void setConnectRetries(int connectRetries) {
		this.connectRetries = connectRetries;
	}

	/**
     * Returns the interval between connection retries.
     *
     * @return the interval between connection retries
     */
    public Duration getConnectRetriesInterval() {
		return this.connectRetriesInterval;
	}

	/**
     * Sets the interval between connection retries.
     * 
     * @param connectRetriesInterval the duration between connection retries
     */
    public void setConnectRetriesInterval(Duration connectRetriesInterval) {
		this.connectRetriesInterval = connectRetriesInterval;
	}

	/**
     * Returns the number of times to retry acquiring a lock during database migration.
     *
     * @return the number of lock acquisition retries
     */
    public int getLockRetryCount() {
		return this.lockRetryCount;
	}

	/**
     * Sets the number of times to retry acquiring a lock when running Flyway migrations.
     * 
     * @param lockRetryCount the number of times to retry acquiring a lock
     */
    public void setLockRetryCount(Integer lockRetryCount) {
		this.lockRetryCount = lockRetryCount;
	}

	/**
     * Returns the default schema.
     *
     * @return the default schema
     */
    public String getDefaultSchema() {
		return this.defaultSchema;
	}

	/**
     * Sets the default schema for Flyway.
     * 
     * @param defaultSchema the default schema to be set
     */
    public void setDefaultSchema(String defaultSchema) {
		this.defaultSchema = defaultSchema;
	}

	/**
     * Retrieves the list of schemas.
     *
     * @return the list of schemas
     */
    public List<String> getSchemas() {
		return this.schemas;
	}

	/**
     * Sets the list of schemas to be used by Flyway.
     * 
     * @param schemas the list of schemas to be used
     */
    public void setSchemas(List<String> schemas) {
		this.schemas = schemas;
	}

	/**
     * Returns a boolean value indicating whether to create schemas.
     * 
     * @return true if schemas should be created, false otherwise
     */
    public boolean isCreateSchemas() {
		return this.createSchemas;
	}

	/**
     * Sets the flag indicating whether to create schemas.
     * 
     * @param createSchemas true to create schemas, false otherwise
     */
    public void setCreateSchemas(boolean createSchemas) {
		this.createSchemas = createSchemas;
	}

	/**
     * Returns the name of the table.
     *
     * @return the name of the table
     */
    public String getTable() {
		return this.table;
	}

	/**
     * Sets the table name for Flyway migration history.
     * 
     * @param table the name of the table to be set
     */
    public void setTable(String table) {
		this.table = table;
	}

	/**
     * Returns the tablespace associated with the FlywayProperties object.
     *
     * @return the tablespace associated with the FlywayProperties object
     */
    public String getTablespace() {
		return this.tablespace;
	}

	/**
     * Sets the tablespace for the FlywayProperties object.
     * 
     * @param tablespace the tablespace to be set
     */
    public void setTablespace(String tablespace) {
		this.tablespace = tablespace;
	}

	/**
     * Returns the baseline description.
     *
     * @return the baseline description
     */
    public String getBaselineDescription() {
		return this.baselineDescription;
	}

	/**
     * Sets the baseline description for FlywayProperties.
     * 
     * @param baselineDescription the baseline description to set
     */
    public void setBaselineDescription(String baselineDescription) {
		this.baselineDescription = baselineDescription;
	}

	/**
     * Returns the baseline version of the FlywayProperties.
     *
     * @return the baseline version of the FlywayProperties
     */
    public String getBaselineVersion() {
		return this.baselineVersion;
	}

	/**
     * Sets the baseline version for Flyway.
     * 
     * @param baselineVersion the baseline version to set
     */
    public void setBaselineVersion(String baselineVersion) {
		this.baselineVersion = baselineVersion;
	}

	/**
     * Returns the value of the installedBy property.
     *
     * @return the value of the installedBy property
     */
    public String getInstalledBy() {
		return this.installedBy;
	}

	/**
     * Sets the value of the installedBy property.
     * 
     * @param installedBy the value to set for the installedBy property
     */
    public void setInstalledBy(String installedBy) {
		this.installedBy = installedBy;
	}

	/**
     * Returns the placeholders map.
     *
     * @return the placeholders map
     */
    public Map<String, String> getPlaceholders() {
		return this.placeholders;
	}

	/**
     * Sets the placeholders for the FlywayProperties.
     * 
     * @param placeholders a Map containing the placeholders to be set
     */
    public void setPlaceholders(Map<String, String> placeholders) {
		this.placeholders = placeholders;
	}

	/**
     * Retrieves the placeholder prefix used in FlywayProperties.
     *
     * @return The placeholder prefix.
     */
    public String getPlaceholderPrefix() {
		return this.placeholderPrefix;
	}

	/**
     * Sets the placeholder prefix for Flyway properties.
     * 
     * @param placeholderPrefix the placeholder prefix to be set
     */
    public void setPlaceholderPrefix(String placeholderPrefix) {
		this.placeholderPrefix = placeholderPrefix;
	}

	/**
     * Returns the placeholder suffix used in Flyway properties.
     *
     * @return the placeholder suffix
     */
    public String getPlaceholderSuffix() {
		return this.placeholderSuffix;
	}

	/**
     * Sets the suffix to be appended to placeholders in SQL migration scripts.
     * 
     * @param placeholderSuffix the suffix to be appended to placeholders
     */
    public void setPlaceholderSuffix(String placeholderSuffix) {
		this.placeholderSuffix = placeholderSuffix;
	}

	/**
     * Returns the placeholder separator used in FlywayProperties.
     *
     * @return the placeholder separator
     */
    public String getPlaceholderSeparator() {
		return this.placeholderSeparator;
	}

	/**
     * Sets the placeholder separator for Flyway properties.
     * 
     * @param placeholderSeparator the placeholder separator to be set
     */
    public void setPlaceholderSeparator(String placeholderSeparator) {
		this.placeholderSeparator = placeholderSeparator;
	}

	/**
     * Returns a boolean value indicating whether placeholder replacement is enabled.
     *
     * @return true if placeholder replacement is enabled, false otherwise
     */
    public boolean isPlaceholderReplacement() {
		return this.placeholderReplacement;
	}

	/**
     * Sets the flag indicating whether placeholder replacement should be enabled or not.
     * 
     * @param placeholderReplacement true to enable placeholder replacement, false otherwise
     */
    public void setPlaceholderReplacement(boolean placeholderReplacement) {
		this.placeholderReplacement = placeholderReplacement;
	}

	/**
     * Retrieves the SQL migration prefix.
     * 
     * @return the SQL migration prefix
     */
    public String getSqlMigrationPrefix() {
		return this.sqlMigrationPrefix;
	}

	/**
     * Sets the prefix for SQL migration files.
     * 
     * @param sqlMigrationPrefix the prefix for SQL migration files
     */
    public void setSqlMigrationPrefix(String sqlMigrationPrefix) {
		this.sqlMigrationPrefix = sqlMigrationPrefix;
	}

	/**
     * Returns the list of SQL migration suffixes.
     * 
     * @return the list of SQL migration suffixes
     */
    public List<String> getSqlMigrationSuffixes() {
		return this.sqlMigrationSuffixes;
	}

	/**
     * Sets the suffixes for SQL migration files.
     * 
     * @param sqlMigrationSuffixes the list of suffixes to be set
     */
    public void setSqlMigrationSuffixes(List<String> sqlMigrationSuffixes) {
		this.sqlMigrationSuffixes = sqlMigrationSuffixes;
	}

	/**
     * Retrieves the SQL migration separator.
     * 
     * @return the SQL migration separator
     */
    public String getSqlMigrationSeparator() {
		return this.sqlMigrationSeparator;
	}

	/**
     * Sets the SQL migration separator.
     * 
     * @param sqlMigrationSeparator the separator to be used for separating SQL migration scripts
     */
    public void setSqlMigrationSeparator(String sqlMigrationSeparator) {
		this.sqlMigrationSeparator = sqlMigrationSeparator;
	}

	/**
     * Retrieves the repeatable SQL migration prefix.
     * 
     * @return the repeatable SQL migration prefix
     */
    public String getRepeatableSqlMigrationPrefix() {
		return this.repeatableSqlMigrationPrefix;
	}

	/**
     * Sets the prefix for repeatable SQL migrations.
     * 
     * @param repeatableSqlMigrationPrefix the prefix for repeatable SQL migrations
     */
    public void setRepeatableSqlMigrationPrefix(String repeatableSqlMigrationPrefix) {
		this.repeatableSqlMigrationPrefix = repeatableSqlMigrationPrefix;
	}

	/**
     * Returns the target value.
     *
     * @return the target value
     */
    public String getTarget() {
		return this.target;
	}

	/**
     * Sets the target version or tag for the migration.
     * 
     * @param target the target version or tag to set
     */
    public void setTarget(String target) {
		this.target = target;
	}

	/**
     * Returns the user associated with the FlywayProperties object.
     *
     * @return the user associated with the FlywayProperties object
     */
    public String getUser() {
		return this.user;
	}

	/**
     * Sets the user for the FlywayProperties.
     * 
     * @param user the user to set
     */
    public void setUser(String user) {
		this.user = user;
	}

	/**
     * Returns the password for the Flyway database connection.
     *
     * @return the password for the Flyway database connection
     */
    public String getPassword() {
		return this.password;
	}

	/**
     * Sets the password for the Flyway database connection.
     * 
     * @param password the password to set
     */
    public void setPassword(String password) {
		this.password = password;
	}

	/**
     * Returns the driver class name.
     *
     * @return the driver class name
     */
    public String getDriverClassName() {
		return this.driverClassName;
	}

	/**
     * Sets the driver class name for the Flyway database migration.
     * 
     * @param driverClassName the driver class name to be set
     */
    public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	/**
     * Returns the URL of the Flyway database connection.
     *
     * @return the URL of the Flyway database connection
     */
    public String getUrl() {
		return this.url;
	}

	/**
     * Sets the URL for the Flyway database connection.
     * 
     * @param url the URL for the database connection
     */
    public void setUrl(String url) {
		this.url = url;
	}

	/**
     * Returns the list of initial SQL statements.
     *
     * @return the list of initial SQL statements
     */
    public List<String> getInitSqls() {
		return this.initSqls;
	}

	/**
     * Sets the list of initial SQL scripts to be executed.
     * 
     * @param initSqls the list of initial SQL scripts
     */
    public void setInitSqls(List<String> initSqls) {
		this.initSqls = initSqls;
	}

	/**
     * Returns a boolean value indicating whether the baseline should be applied during migration.
     * 
     * @return true if the baseline should be applied during migration, false otherwise
     */
    public boolean isBaselineOnMigrate() {
		return this.baselineOnMigrate;
	}

	/**
     * Sets the flag indicating whether to baseline on migrate.
     * 
     * @param baselineOnMigrate the flag indicating whether to baseline on migrate
     */
    public void setBaselineOnMigrate(boolean baselineOnMigrate) {
		this.baselineOnMigrate = baselineOnMigrate;
	}

	/**
     * Returns a boolean value indicating whether the clean operation is disabled.
     * 
     * @return true if the clean operation is disabled, false otherwise
     */
    public boolean isCleanDisabled() {
		return this.cleanDisabled;
	}

	/**
     * Sets the flag indicating whether the clean operation is disabled.
     * 
     * @param cleanDisabled true if the clean operation is disabled, false otherwise
     */
    public void setCleanDisabled(boolean cleanDisabled) {
		this.cleanDisabled = cleanDisabled;
	}

	/**
     * Returns a boolean value indicating whether the clean operation should be performed on validation error.
     *
     * @return true if the clean operation should be performed on validation error, false otherwise
     */
    public boolean isCleanOnValidationError() {
		return this.cleanOnValidationError;
	}

	/**
     * Sets the flag indicating whether to clean the database on validation error.
     * 
     * @param cleanOnValidationError the flag indicating whether to clean the database on validation error
     */
    public void setCleanOnValidationError(boolean cleanOnValidationError) {
		this.cleanOnValidationError = cleanOnValidationError;
	}

	/**
     * Returns a boolean value indicating whether the FlywayProperties object is a group.
     *
     * @return true if the FlywayProperties object is a group, false otherwise.
     */
    public boolean isGroup() {
		return this.group;
	}

	/**
     * Sets the value of the group property.
     * 
     * @param group the new value for the group property
     */
    public void setGroup(boolean group) {
		this.group = group;
	}

	/**
     * Returns a boolean value indicating whether the Flyway properties are mixed or not.
     * 
     * @return true if the Flyway properties are mixed, false otherwise
     */
    public boolean isMixed() {
		return this.mixed;
	}

	/**
     * Sets the value indicating whether the migration scripts are mixed.
     * 
     * @param mixed the value indicating whether the migration scripts are mixed
     */
    public void setMixed(boolean mixed) {
		this.mixed = mixed;
	}

	/**
     * Returns a boolean value indicating whether the object is out of order.
     *
     * @return true if the object is out of order, false otherwise
     */
    public boolean isOutOfOrder() {
		return this.outOfOrder;
	}

	/**
     * Sets the out of order flag for FlywayProperties.
     * 
     * @param outOfOrder the out of order flag to set
     */
    public void setOutOfOrder(boolean outOfOrder) {
		this.outOfOrder = outOfOrder;
	}

	/**
     * Returns a boolean value indicating whether the default callbacks should be skipped.
     * 
     * @return true if the default callbacks should be skipped, false otherwise
     */
    public boolean isSkipDefaultCallbacks() {
		return this.skipDefaultCallbacks;
	}

	/**
     * Sets the flag to skip default callbacks.
     * 
     * @param skipDefaultCallbacks true to skip default callbacks, false otherwise
     */
    public void setSkipDefaultCallbacks(boolean skipDefaultCallbacks) {
		this.skipDefaultCallbacks = skipDefaultCallbacks;
	}

	/**
     * Returns a boolean value indicating whether to skip default resolvers.
     * 
     * @return true if default resolvers should be skipped, false otherwise
     */
    public boolean isSkipDefaultResolvers() {
		return this.skipDefaultResolvers;
	}

	/**
     * Sets the flag to skip default resolvers.
     * 
     * @param skipDefaultResolvers the flag indicating whether to skip default resolvers
     */
    public void setSkipDefaultResolvers(boolean skipDefaultResolvers) {
		this.skipDefaultResolvers = skipDefaultResolvers;
	}

	/**
     * Returns the flag indicating whether migration naming is validated.
     * 
     * @return {@code true} if migration naming is validated, {@code false} otherwise.
     */
    public boolean isValidateMigrationNaming() {
		return this.validateMigrationNaming;
	}

	/**
     * Sets the flag to enable or disable validation of migration naming.
     * 
     * @param validateMigrationNaming true to enable validation, false to disable validation
     */
    public void setValidateMigrationNaming(boolean validateMigrationNaming) {
		this.validateMigrationNaming = validateMigrationNaming;
	}

	/**
     * Returns the value of the validateOnMigrate property.
     * 
     * @return true if validateOnMigrate is enabled, false otherwise
     */
    public boolean isValidateOnMigrate() {
		return this.validateOnMigrate;
	}

	/**
     * Sets the flag to determine whether to validate migrations against the schema history table during migration.
     * 
     * @param validateOnMigrate true to validate migrations, false otherwise
     */
    public void setValidateOnMigrate(boolean validateOnMigrate) {
		this.validateOnMigrate = validateOnMigrate;
	}

	/**
     * Returns the prefix used for script placeholders.
     *
     * @return the script placeholder prefix
     */
    public String getScriptPlaceholderPrefix() {
		return this.scriptPlaceholderPrefix;
	}

	/**
     * Sets the prefix for script placeholders.
     * 
     * @param scriptPlaceholderPrefix the prefix for script placeholders
     */
    public void setScriptPlaceholderPrefix(String scriptPlaceholderPrefix) {
		this.scriptPlaceholderPrefix = scriptPlaceholderPrefix;
	}

	/**
     * Returns the suffix used to identify placeholders in SQL scripts.
     * 
     * @return the script placeholder suffix
     */
    public String getScriptPlaceholderSuffix() {
		return this.scriptPlaceholderSuffix;
	}

	/**
     * Sets the suffix for script placeholders.
     * 
     * @param scriptPlaceholderSuffix the suffix for script placeholders
     */
    public void setScriptPlaceholderSuffix(String scriptPlaceholderSuffix) {
		this.scriptPlaceholderSuffix = scriptPlaceholderSuffix;
	}

	/**
     * Returns a boolean value indicating whether the execution should be performed within a transaction.
     *
     * @return true if the execution should be performed within a transaction, false otherwise
     */
    public boolean isExecuteInTransaction() {
		return this.executeInTransaction;
	}

	/**
     * Sets whether the execution should be performed within a transaction.
     * 
     * @param executeInTransaction true if the execution should be performed within a transaction, false otherwise
     */
    public void setExecuteInTransaction(boolean executeInTransaction) {
		this.executeInTransaction = executeInTransaction;
	}

	/**
     * Returns an array of loggers.
     *
     * @return an array of loggers
     */
    public String[] getLoggers() {
		return this.loggers;
	}

	/**
     * Sets the loggers for FlywayProperties.
     * 
     * @param loggers an array of loggers to be set
     */
    public void setLoggers(String[] loggers) {
		this.loggers = loggers;
	}

	/**
     * Returns the value of the batch property.
     *
     * @return true if batch mode is enabled, false otherwise
     */
    public Boolean getBatch() {
		return this.batch;
	}

	/**
     * Sets the value of the batch property.
     * 
     * @param batch the new value for the batch property
     */
    public void setBatch(Boolean batch) {
		this.batch = batch;
	}

	/**
     * Returns the dry run output file.
     * 
     * @return the dry run output file
     */
    public File getDryRunOutput() {
		return this.dryRunOutput;
	}

	/**
     * Sets the file to which the dry run output should be written.
     * 
     * @param dryRunOutput the file to which the dry run output should be written
     */
    public void setDryRunOutput(File dryRunOutput) {
		this.dryRunOutput = dryRunOutput;
	}

	/**
     * Returns the error overrides array.
     *
     * @return the error overrides array
     */
    public String[] getErrorOverrides() {
		return this.errorOverrides;
	}

	/**
     * Sets the error overrides for Flyway.
     * 
     * @param errorOverrides an array of error overrides to be set
     */
    public void setErrorOverrides(String[] errorOverrides) {
		this.errorOverrides = errorOverrides;
	}

	/**
     * Returns the license key associated with the FlywayProperties object.
     *
     * @return the license key
     */
    public String getLicenseKey() {
		return this.licenseKey;
	}

	/**
     * Sets the license key for Flyway.
     * 
     * @param licenseKey the license key to be set
     */
    public void setLicenseKey(String licenseKey) {
		this.licenseKey = licenseKey;
	}

	/**
     * Gets the value of the deprecated configuration property "oracleSqlplus".
     *
     * This property has been deprecated since version 3.2.0 and will be removed in a future release.
     * It is recommended to use the replacement property "spring.flyway.oracle.sqlplus" instead.
     *
     * @return the value of the deprecated configuration property "oracleSqlplus"
     * @deprecated This property has been deprecated since version 3.2.0 and will be removed in a future release.
     * It is recommended to use the replacement property "spring.flyway.oracle.sqlplus" instead.
     */
    @DeprecatedConfigurationProperty(replacement = "spring.flyway.oracle.sqlplus", since = "3.2.0")
	@Deprecated(since = "3.2.0", forRemoval = true)
	public Boolean getOracleSqlplus() {
		return getOracle().getSqlplus();
	}

	/**
     * Sets the value of the oracleSqlplus property.
     * 
     * @param oracleSqlplus the new value for the oracleSqlplus property
     * 
     * @deprecated This method is deprecated since version 3.2.0 and will be removed in a future release.
     * 
     * @see FlywayProperties#getOracle()
     * @see OracleProperties#setSqlplus(Boolean)
     */
    @Deprecated(since = "3.2.0", forRemoval = true)
	public void setOracleSqlplus(Boolean oracleSqlplus) {
		getOracle().setSqlplus(oracleSqlplus);
	}

	/**
     * Retrieves the value of the deprecated configuration property "oracle.sqlplus-warn".
     * 
     * @return The value of the deprecated configuration property "oracle.sqlplus-warn".
     * 
     * @deprecated This method is deprecated since version 3.2.0 and will be removed in a future release.
     *             Use the replacement property "spring.flyway.oracle.sqlplus-warn" instead.
     * 
     * @see FlywayProperties#getOracle()
     * @see OracleProperties#getSqlplusWarn()
     * @see DeprecatedConfigurationProperty
     * @see Deprecated
     * @since 3.2.0
     */
    @DeprecatedConfigurationProperty(replacement = "spring.flyway.oracle.sqlplus-warn", since = "3.2.0")
	@Deprecated(since = "3.2.0", forRemoval = true)
	public Boolean getOracleSqlplusWarn() {
		return getOracle().getSqlplusWarn();
	}

	/**
     * Sets the Oracle SQLPlus warn flag.
     * 
     * @param oracleSqlplusWarn the Oracle SQLPlus warn flag to set
     * 
     * @deprecated This method is deprecated since version 3.2.0 and will be removed in a future release.
     */
    @Deprecated(since = "3.2.0", forRemoval = true)
	public void setOracleSqlplusWarn(Boolean oracleSqlplusWarn) {
		getOracle().setSqlplusWarn(oracleSqlplusWarn);
	}

	/**
     * Retrieves the Oracle wallet location.
     * 
     * @return the Oracle wallet location
     * 
     * @deprecated This method has been deprecated since version 3.2.0 and will be removed in a future release. 
     *             Use {@link #getOracle().getWalletLocation()} instead.
     *             For more information, refer to the replacement property {@code spring.flyway.oracle.wallet-location}.
     * 
     * @see FlywayProperties#getOracle()
     * @see FlywayProperties#getOracle().getWalletLocation()
     * @see Deprecated
     * @see DeprecatedConfigurationProperty
     * 
     * @since 3.2.0
     */
    @DeprecatedConfigurationProperty(replacement = "spring.flyway.oracle.wallet-location", since = "3.2.0")
	@Deprecated(since = "3.2.0", forRemoval = true)
	public String getOracleWalletLocation() {
		return getOracle().getWalletLocation();
	}

	/**
     * Sets the Oracle wallet location.
     * 
     * @param oracleWalletLocation the location of the Oracle wallet
     * 
     * @deprecated This method is deprecated since version 3.2.0 and will be removed in a future release.
     * 
     * @see FlywayProperties#getOracle()
     * @see OracleConfiguration#setWalletLocation(String)
     */
    @Deprecated(since = "3.2.0", forRemoval = true)
	public void setOracleWalletLocation(String oracleWalletLocation) {
		getOracle().setWalletLocation(oracleWalletLocation);
	}

	/**
     * Returns the value of the stream property.
     *
     * @return the value of the stream property
     */
    public Boolean getStream() {
		return this.stream;
	}

	/**
     * Sets the value of the stream property.
     * 
     * @param stream the new value for the stream property
     */
    public void setStream(Boolean stream) {
		this.stream = stream;
	}

	/**
     * Retrieves the prefix used for undo SQL migration files.
     * 
     * @return The undo SQL migration prefix.
     */
    public String getUndoSqlMigrationPrefix() {
		return this.undoSqlMigrationPrefix;
	}

	/**
     * Sets the prefix for undo SQL migration files.
     * 
     * @param undoSqlMigrationPrefix the prefix for undo SQL migration files
     */
    public void setUndoSqlMigrationPrefix(String undoSqlMigrationPrefix) {
		this.undoSqlMigrationPrefix = undoSqlMigrationPrefix;
	}

	/**
     * Returns the cherryPick array.
     *
     * @return the cherryPick array
     */
    public String[] getCherryPick() {
		return this.cherryPick;
	}

	/**
     * Sets the cherryPick property.
     * 
     * @param cherryPick an array of strings representing the cherry-picked migrations to apply
     */
    public void setCherryPick(String[] cherryPick) {
		this.cherryPick = cherryPick;
	}

	/**
     * Returns the JDBC properties for the Flyway configuration.
     * 
     * @return a Map containing the JDBC properties
     */
    public Map<String, String> getJdbcProperties() {
		return this.jdbcProperties;
	}

	/**
     * Sets the JDBC properties for the FlywayProperties class.
     * 
     * @param jdbcProperties a Map containing the JDBC properties to be set
     */
    public void setJdbcProperties(Map<String, String> jdbcProperties) {
		this.jdbcProperties = jdbcProperties;
	}

	/**
     * Returns the path of the Kerberos configuration file.
     *
     * @return the path of the Kerberos configuration file
     */
    public String getKerberosConfigFile() {
		return this.kerberosConfigFile;
	}

	/**
     * Sets the path to the Kerberos configuration file.
     * 
     * @param kerberosConfigFile the path to the Kerberos configuration file
     */
    public void setKerberosConfigFile(String kerberosConfigFile) {
		this.kerberosConfigFile = kerberosConfigFile;
	}

	/**
     * Retrieves the Oracle Kerberos cache file.
     * 
     * @return The Oracle Kerberos cache file.
     * 
     * @deprecated This configuration property has been deprecated since version 3.2.0. Please use 'spring.flyway.oracle.kerberos-cache-file' instead.
     * 
     * @since 3.2.0
     * @deprecated This method has been deprecated since version 3.2.0 and is scheduled for removal.
     */
    @DeprecatedConfigurationProperty(replacement = "spring.flyway.oracle.kerberos-cache-file", since = "3.2.0")
	@Deprecated(since = "3.2.0", forRemoval = true)
	public String getOracleKerberosCacheFile() {
		return getOracle().getKerberosCacheFile();
	}

	/**
     * Sets the Oracle Kerberos cache file.
     * 
     * @param oracleKerberosCacheFile the path to the Oracle Kerberos cache file
     * 
     * @deprecated This method has been deprecated since version 3.2.0 and will be removed in a future release.
     * 
     * @see FlywayProperties#getOracle()
     * @see OracleConfiguration#setKerberosCacheFile(String)
     */
    @Deprecated(since = "3.2.0", forRemoval = true)
	public void setOracleKerberosCacheFile(String oracleKerberosCacheFile) {
		getOracle().setKerberosCacheFile(oracleKerberosCacheFile);
	}

	/**
     * Returns the value of the outputQueryResults property.
     * 
     * @return the value of the outputQueryResults property
     */
    public Boolean getOutputQueryResults() {
		return this.outputQueryResults;
	}

	/**
     * Sets the flag to determine whether to output query results.
     * 
     * @param outputQueryResults the flag indicating whether to output query results
     */
    public void setOutputQueryResults(Boolean outputQueryResults) {
		this.outputQueryResults = outputQueryResults;
	}

	/**
     * Retrieves the SQL Server Kerberos login file.
     * 
     * @return The SQL Server Kerberos login file.
     * 
     * @deprecated This method has been deprecated since version 3.2.0 and will be removed in a future release. 
     *             Please use the replacement property "spring.flyway.sqlserver.kerberos-login-file" instead.
     */
    @DeprecatedConfigurationProperty(replacement = "spring.flyway.sqlserver.kerberos-login-file")
	@Deprecated(since = "3.2.0", forRemoval = true)
	public String getSqlServerKerberosLoginFile() {
		return getSqlserver().getKerberosLoginFile();
	}

	/**
     * Sets the SQL Server Kerberos login file.
     * 
     * @param sqlServerKerberosLoginFile the path to the SQL Server Kerberos login file
     * 
     * @deprecated This method has been deprecated since version 3.2.0 and will be removed in a future release.
     * 
     * @see FlywayProperties#getSqlserver()
     * @see SqlServerProperties#setKerberosLoginFile(String)
     */
    @Deprecated(since = "3.2.0", forRemoval = true)
	public void setSqlServerKerberosLoginFile(String sqlServerKerberosLoginFile) {
		getSqlserver().setKerberosLoginFile(sqlServerKerberosLoginFile);
	}

	/**
     * Returns the value of the skipExecutingMigrations property.
     * 
     * @return the value of the skipExecutingMigrations property
     */
    public Boolean getSkipExecutingMigrations() {
		return this.skipExecutingMigrations;
	}

	/**
     * Sets the flag to skip executing migrations.
     * 
     * @param skipExecutingMigrations the flag indicating whether to skip executing migrations
     */
    public void setSkipExecutingMigrations(Boolean skipExecutingMigrations) {
		this.skipExecutingMigrations = skipExecutingMigrations;
	}

	/**
     * Returns the list of ignore migration patterns.
     * 
     * @return the list of ignore migration patterns
     */
    public List<String> getIgnoreMigrationPatterns() {
		return this.ignoreMigrationPatterns;
	}

	/**
     * Sets the list of migration patterns to ignore during the migration process.
     * 
     * @param ignoreMigrationPatterns the list of migration patterns to ignore
     */
    public void setIgnoreMigrationPatterns(List<String> ignoreMigrationPatterns) {
		this.ignoreMigrationPatterns = ignoreMigrationPatterns;
	}

	/**
     * Returns the value of the detectEncoding property.
     *
     * @return true if encoding detection is enabled, false otherwise
     */
    public Boolean getDetectEncoding() {
		return this.detectEncoding;
	}

	/**
     * Sets the flag to enable or disable automatic detection of encoding.
     * 
     * @param detectEncoding
     *            the flag indicating whether to enable or disable automatic detection of encoding
     */
    public void setDetectEncoding(final Boolean detectEncoding) {
		this.detectEncoding = detectEncoding;
	}

	/**
     * Returns the Oracle instance associated with this FlywayProperties object.
     *
     * @return the Oracle instance
     */
    public Oracle getOracle() {
		return this.oracle;
	}

	/**
     * Returns the Postgresql object associated with this FlywayProperties instance.
     *
     * @return the Postgresql object
     */
    public Postgresql getPostgresql() {
		return this.postgresql;
	}

	/**
     * Returns the Sqlserver object associated with this FlywayProperties instance.
     *
     * @return the Sqlserver object
     */
    public Sqlserver getSqlserver() {
		return this.sqlserver;
	}

	/**
	 * {@code OracleConfigurationExtension} properties.
	 */
	public static class Oracle {

		/**
		 * Whether to enable support for Oracle SQL*Plus commands. Requires Flyway Teams.
		 */
		private Boolean sqlplus;

		/**
		 * Whether to issue a warning rather than an error when a not-yet-supported Oracle
		 * SQL*Plus statement is encountered. Requires Flyway Teams.
		 */
		private Boolean sqlplusWarn;

		/**
		 * Path of the Oracle Kerberos cache file. Requires Flyway Teams.
		 */
		private String kerberosCacheFile;

		/**
		 * Location of the Oracle Wallet, used to sign in to the database automatically.
		 * Requires Flyway Teams.
		 */
		private String walletLocation;

		/**
         * Returns the value of the sqlplus property.
         * 
         * @return the value of the sqlplus property
         */
        public Boolean getSqlplus() {
			return this.sqlplus;
		}

		/**
         * Sets the value of the sqlplus property.
         * 
         * @param sqlplus the new value for the sqlplus property
         */
        public void setSqlplus(Boolean sqlplus) {
			this.sqlplus = sqlplus;
		}

		/**
         * Returns the value of the sqlplusWarn property.
         * 
         * @return the value of the sqlplusWarn property
         */
        public Boolean getSqlplusWarn() {
			return this.sqlplusWarn;
		}

		/**
         * Sets the value of the sqlplusWarn property.
         * 
         * @param sqlplusWarn the new value for the sqlplusWarn property
         */
        public void setSqlplusWarn(Boolean sqlplusWarn) {
			this.sqlplusWarn = sqlplusWarn;
		}

		/**
         * Returns the path of the Kerberos cache file.
         * 
         * @return the path of the Kerberos cache file
         */
        public String getKerberosCacheFile() {
			return this.kerberosCacheFile;
		}

		/**
         * Sets the path to the Kerberos cache file.
         * 
         * @param kerberosCacheFile the path to the Kerberos cache file
         */
        public void setKerberosCacheFile(String kerberosCacheFile) {
			this.kerberosCacheFile = kerberosCacheFile;
		}

		/**
         * Returns the location of the wallet.
         *
         * @return the location of the wallet
         */
        public String getWalletLocation() {
			return this.walletLocation;
		}

		/**
         * Sets the location of the wallet.
         * 
         * @param walletLocation the location of the wallet
         */
        public void setWalletLocation(String walletLocation) {
			this.walletLocation = walletLocation;
		}

	}

	/**
	 * {@code PostgreSQLConfigurationExtension} properties.
	 */
	public static class Postgresql {

		/**
		 * Whether transactional advisory locks should be used. If set to false,
		 * session-level locks are used instead.
		 */
		private Boolean transactionalLock;

		/**
         * Returns the value of the transactional lock.
         *
         * @return the value of the transactional lock
         */
        public Boolean getTransactionalLock() {
			return this.transactionalLock;
		}

		/**
         * Sets the transactional lock flag.
         * 
         * @param transactionalLock the transactional lock flag to be set
         */
        public void setTransactionalLock(Boolean transactionalLock) {
			this.transactionalLock = transactionalLock;
		}

	}

	/**
	 * {@code SQLServerConfigurationExtension} properties.
	 */
	public static class Sqlserver {

		/**
		 * Path to the SQL Server Kerberos login file. Requires Flyway Teams.
		 */
		private String kerberosLoginFile;

		/**
         * Returns the path of the Kerberos login file.
         * 
         * @return the path of the Kerberos login file
         */
        public String getKerberosLoginFile() {
			return this.kerberosLoginFile;
		}

		/**
         * Sets the path to the Kerberos login file.
         * 
         * @param kerberosLoginFile the path to the Kerberos login file
         */
        public void setKerberosLoginFile(String kerberosLoginFile) {
			this.kerberosLoginFile = kerberosLoginFile;
		}

	}

}
