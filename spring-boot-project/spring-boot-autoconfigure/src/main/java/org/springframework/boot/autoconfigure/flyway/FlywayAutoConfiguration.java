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

import java.sql.DatabaseMetaData;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.extensibility.ConfigurationExtension;
import org.flywaydb.core.internal.database.postgresql.PostgreSQLConfigurationExtension;
import org.flywaydb.database.oracle.OracleConfigurationExtension;
import org.flywaydb.database.sqlserver.SQLServerConfigurationExtension;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.FlywayAutoConfigurationRuntimeHints;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.FlywayDataSourceCondition;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties.Oracle;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties.Postgresql;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties.Sqlserver;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Flyway database migrations.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @author Jacques-Etienne Beaudet
 * @author Eddú Meléndez
 * @author Dominic Gunn
 * @author Dan Zheng
 * @author András Deák
 * @author Semyon Danilov
 * @author Chris Bono
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 1.1.0
 */
@AutoConfiguration(after = { DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class })
@ConditionalOnClass(Flyway.class)
@Conditional(FlywayDataSourceCondition.class)
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", matchIfMissing = true)
@Import(DatabaseInitializationDependencyConfigurer.class)
@ImportRuntimeHints(FlywayAutoConfigurationRuntimeHints.class)
public class FlywayAutoConfiguration {

	/**
     * Creates a converter for converting a String or Number to a MigrationVersion.
     * This converter is used for binding configuration properties to MigrationVersion objects.
     *
     * @return The StringOrNumberToMigrationVersionConverter instance.
     */
    @Bean
	@ConfigurationPropertiesBinding
	public StringOrNumberToMigrationVersionConverter stringOrNumberMigrationVersionConverter() {
		return new StringOrNumberToMigrationVersionConverter();
	}

	/**
     * Creates a FlywaySchemaManagementProvider bean that provides the default DDL mode for Flyway.
     * 
     * @param flyways the Flyway instances to be managed
     * @return the FlywaySchemaManagementProvider bean
     */
    @Bean
	public FlywaySchemaManagementProvider flywayDefaultDdlModeProvider(ObjectProvider<Flyway> flyways) {
		return new FlywaySchemaManagementProvider(flyways);
	}

	/**
     * FlywayConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JdbcUtils.class)
	@ConditionalOnMissingBean(Flyway.class)
	@EnableConfigurationProperties(FlywayProperties.class)
	public static class FlywayConfiguration {

		private final FlywayProperties properties;

		/**
         * Constructs a new FlywayConfiguration object with the specified FlywayProperties.
         * 
         * @param properties the FlywayProperties to be used for configuring Flyway
         */
        FlywayConfiguration(FlywayProperties properties) {
			this.properties = properties;
		}

		/**
         * Returns a new instance of ResourceProviderCustomizer.
         * 
         * @return a new instance of ResourceProviderCustomizer
         */
        @Bean
		ResourceProviderCustomizer resourceProviderCustomizer() {
			return new ResourceProviderCustomizer();
		}

		/**
         * Generates the Flyway connection details based on the properties provided.
         * If a bean of type FlywayConnectionDetails is already present, this method will not be executed.
         * 
         * @return The Flyway connection details generated from the properties.
         */
        @Bean
		@ConditionalOnMissingBean(FlywayConnectionDetails.class)
		PropertiesFlywayConnectionDetails flywayConnectionDetails() {
			return new PropertiesFlywayConnectionDetails(this.properties);
		}

		/**
         * Creates a customizer for configuring Flyway with SQL Server specific settings.
         * This customizer is only applied if the class "org.flywaydb.database.sqlserver.SQLServerConfigurationExtension" is present in the classpath.
         * 
         * @return the SqlServerFlywayConfigurationCustomizer instance
         */
        @Bean
		@ConditionalOnClass(name = "org.flywaydb.database.sqlserver.SQLServerConfigurationExtension")
		SqlServerFlywayConfigurationCustomizer sqlServerFlywayConfigurationCustomizer() {
			return new SqlServerFlywayConfigurationCustomizer(this.properties);
		}

		/**
         * Creates a customizer for Oracle Flyway configuration.
         * This method is annotated with @Bean to indicate that it is a Spring bean.
         * It is also annotated with @ConditionalOnClass to ensure that the customizer is only created if the class "org.flywaydb.database.oracle.OracleConfigurationExtension" is present in the classpath.
         * 
         * @return OracleFlywayConfigurationCustomizer - the customizer for Oracle Flyway configuration
         */
        @Bean
		@ConditionalOnClass(name = "org.flywaydb.database.oracle.OracleConfigurationExtension")
		OracleFlywayConfigurationCustomizer oracleFlywayConfigurationCustomizer() {
			return new OracleFlywayConfigurationCustomizer(this.properties);
		}

		/**
         * Creates a customizer for PostgresqlFlywayConfiguration.
         * This customizer is only created if the class "org.flywaydb.core.internal.database.postgresql.PostgreSQLConfigurationExtension" is present.
         * 
         * @return the PostgresqlFlywayConfigurationCustomizer object
         */
        @Bean
		@ConditionalOnClass(name = "org.flywaydb.core.internal.database.postgresql.PostgreSQLConfigurationExtension")
		PostgresqlFlywayConfigurationCustomizer postgresqlFlywayConfigurationCustomizer() {
			return new PostgresqlFlywayConfigurationCustomizer(this.properties);
		}

		/**
         * Creates and configures a Flyway instance based on the provided connection details, resource loader,
         * data sources, customizers, and callbacks.
         *
         * @param connectionDetails The connection details for the database.
         * @param resourceLoader The resource loader used to load migration scripts.
         * @param dataSource The primary data source.
         * @param flywayDataSource The data source specifically used for Flyway.
         * @param fluentConfigurationCustomizers The customizers for the Flyway configuration.
         * @param javaMigrations The Java migrations to be executed.
         * @param callbacks The callbacks to be executed during migration.
         * @param resourceProviderCustomizer The customizer for the resource provider.
         * @return The configured Flyway instance.
         */
        @Bean
		Flyway flyway(FlywayConnectionDetails connectionDetails, ResourceLoader resourceLoader,
				ObjectProvider<DataSource> dataSource, @FlywayDataSource ObjectProvider<DataSource> flywayDataSource,
				ObjectProvider<FlywayConfigurationCustomizer> fluentConfigurationCustomizers,
				ObjectProvider<JavaMigration> javaMigrations, ObjectProvider<Callback> callbacks,
				ResourceProviderCustomizer resourceProviderCustomizer) {
			FluentConfiguration configuration = new FluentConfiguration(resourceLoader.getClassLoader());
			configureDataSource(configuration, flywayDataSource.getIfAvailable(), dataSource.getIfUnique(),
					connectionDetails);
			configureProperties(configuration, this.properties);
			configureCallbacks(configuration, callbacks.orderedStream().toList());
			configureJavaMigrations(configuration, javaMigrations.orderedStream().toList());
			fluentConfigurationCustomizers.orderedStream().forEach((customizer) -> customizer.customize(configuration));
			resourceProviderCustomizer.customize(configuration);
			return configuration.load();
		}

		/**
         * Configures the data source for Flyway migration.
         * 
         * @param configuration      the FluentConfiguration object to configure
         * @param flywayDataSource   the data source for Flyway migration
         * @param dataSource         the main data source
         * @param connectionDetails  the connection details for Flyway migration
         */
        private void configureDataSource(FluentConfiguration configuration, DataSource flywayDataSource,
				DataSource dataSource, FlywayConnectionDetails connectionDetails) {
			DataSource migrationDataSource = getMigrationDataSource(flywayDataSource, dataSource, connectionDetails);
			configuration.dataSource(migrationDataSource);
		}

		/**
         * Returns the migration DataSource to be used by Flyway.
         * If a separate flywayDataSource is provided, it is returned.
         * Otherwise, if the jdbcUrl is provided in the connectionDetails, a new DataSource is created using the SimpleDriverDataSource class.
         * If the username is provided in the connectionDetails and a dataSource is provided, a new DataSource is created using the SimpleDriverDataSource class derived from the provided dataSource.
         * If none of the above conditions are met, an IllegalStateException is thrown if the dataSource is null.
         *
         * @param flywayDataSource   the separate flywayDataSource to be used
         * @param dataSource         the dataSource to be used if flywayDataSource is not provided
         * @param connectionDetails  the connection details containing the jdbcUrl and username
         * @return the migration DataSource to be used by Flyway
         * @throws IllegalStateException if the dataSource is null and none of the above conditions are met
         */
        private DataSource getMigrationDataSource(DataSource flywayDataSource, DataSource dataSource,
				FlywayConnectionDetails connectionDetails) {
			if (flywayDataSource != null) {
				return flywayDataSource;
			}
			String url = connectionDetails.getJdbcUrl();
			if (url != null) {
				DataSourceBuilder<?> builder = DataSourceBuilder.create().type(SimpleDriverDataSource.class);
				builder.url(url);
				applyConnectionDetails(connectionDetails, builder);
				return builder.build();
			}
			String user = connectionDetails.getUsername();
			if (user != null && dataSource != null) {
				DataSourceBuilder<?> builder = DataSourceBuilder.derivedFrom(dataSource)
					.type(SimpleDriverDataSource.class);
				applyConnectionDetails(connectionDetails, builder);
				return builder.build();
			}
			Assert.state(dataSource != null, "Flyway migration DataSource missing");
			return dataSource;
		}

		/**
         * Applies the connection details to the DataSourceBuilder.
         * 
         * @param connectionDetails the FlywayConnectionDetails object containing the connection details
         * @param builder the DataSourceBuilder object to apply the connection details to
         */
        private void applyConnectionDetails(FlywayConnectionDetails connectionDetails, DataSourceBuilder<?> builder) {
			builder.username(connectionDetails.getUsername());
			builder.password(connectionDetails.getPassword());
			String driverClassName = connectionDetails.getDriverClassName();
			if (StringUtils.hasText(driverClassName)) {
				builder.driverClassName(driverClassName);
			}
		}

		/**
		 * Configure the given {@code configuration} using the given {@code properties}.
		 * <p>
		 * To maximize forwards- and backwards-compatibility method references are not
		 * used.
		 * @param configuration the configuration
		 * @param properties the properties
		 */
		private void configureProperties(FluentConfiguration configuration, FlywayProperties properties) {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			String[] locations = new LocationResolver(configuration.getDataSource())
				.resolveLocations(properties.getLocations())
				.toArray(new String[0]);
			configuration.locations(locations);
			map.from(properties.isFailOnMissingLocations())
				.to((failOnMissingLocations) -> configuration.failOnMissingLocations(failOnMissingLocations));
			map.from(properties.getEncoding()).to((encoding) -> configuration.encoding(encoding));
			map.from(properties.getConnectRetries())
				.to((connectRetries) -> configuration.connectRetries(connectRetries));
			map.from(properties.getConnectRetriesInterval())
				.as(Duration::getSeconds)
				.as(Long::intValue)
				.to((connectRetriesInterval) -> configuration.connectRetriesInterval(connectRetriesInterval));
			map.from(properties.getLockRetryCount())
				.to((lockRetryCount) -> configuration.lockRetryCount(lockRetryCount));
			map.from(properties.getDefaultSchema()).to((schema) -> configuration.defaultSchema(schema));
			map.from(properties.getSchemas())
				.as(StringUtils::toStringArray)
				.to((schemas) -> configuration.schemas(schemas));
			map.from(properties.isCreateSchemas()).to((createSchemas) -> configuration.createSchemas(createSchemas));
			map.from(properties.getTable()).to((table) -> configuration.table(table));
			map.from(properties.getTablespace()).to((tablespace) -> configuration.tablespace(tablespace));
			map.from(properties.getBaselineDescription())
				.to((baselineDescription) -> configuration.baselineDescription(baselineDescription));
			map.from(properties.getBaselineVersion())
				.to((baselineVersion) -> configuration.baselineVersion(baselineVersion));
			map.from(properties.getInstalledBy()).to((installedBy) -> configuration.installedBy(installedBy));
			map.from(properties.getPlaceholders()).to((placeholders) -> configuration.placeholders(placeholders));
			map.from(properties.getPlaceholderPrefix())
				.to((placeholderPrefix) -> configuration.placeholderPrefix(placeholderPrefix));
			map.from(properties.getPlaceholderSuffix())
				.to((placeholderSuffix) -> configuration.placeholderSuffix(placeholderSuffix));
			map.from(properties.getPlaceholderSeparator())
				.to((placeHolderSeparator) -> configuration.placeholderSeparator(placeHolderSeparator));
			map.from(properties.isPlaceholderReplacement())
				.to((placeholderReplacement) -> configuration.placeholderReplacement(placeholderReplacement));
			map.from(properties.getSqlMigrationPrefix())
				.to((sqlMigrationPrefix) -> configuration.sqlMigrationPrefix(sqlMigrationPrefix));
			map.from(properties.getSqlMigrationSuffixes())
				.as(StringUtils::toStringArray)
				.to((sqlMigrationSuffixes) -> configuration.sqlMigrationSuffixes(sqlMigrationSuffixes));
			map.from(properties.getSqlMigrationSeparator())
				.to((sqlMigrationSeparator) -> configuration.sqlMigrationSeparator(sqlMigrationSeparator));
			map.from(properties.getRepeatableSqlMigrationPrefix())
				.to((repeatableSqlMigrationPrefix) -> configuration
					.repeatableSqlMigrationPrefix(repeatableSqlMigrationPrefix));
			map.from(properties.getTarget()).to((target) -> configuration.target(target));
			map.from(properties.isBaselineOnMigrate())
				.to((baselineOnMigrate) -> configuration.baselineOnMigrate(baselineOnMigrate));
			map.from(properties.isCleanDisabled()).to((cleanDisabled) -> configuration.cleanDisabled(cleanDisabled));
			map.from(properties.isCleanOnValidationError())
				.to((cleanOnValidationError) -> configuration.cleanOnValidationError(cleanOnValidationError));
			map.from(properties.isGroup()).to((group) -> configuration.group(group));
			map.from(properties.isMixed()).to((mixed) -> configuration.mixed(mixed));
			map.from(properties.isOutOfOrder()).to((outOfOrder) -> configuration.outOfOrder(outOfOrder));
			map.from(properties.isSkipDefaultCallbacks())
				.to((skipDefaultCallbacks) -> configuration.skipDefaultCallbacks(skipDefaultCallbacks));
			map.from(properties.isSkipDefaultResolvers())
				.to((skipDefaultResolvers) -> configuration.skipDefaultResolvers(skipDefaultResolvers));
			map.from(properties.isValidateMigrationNaming())
				.to((validateMigrationNaming) -> configuration.validateMigrationNaming(validateMigrationNaming));
			map.from(properties.isValidateOnMigrate())
				.to((validateOnMigrate) -> configuration.validateOnMigrate(validateOnMigrate));
			map.from(properties.getInitSqls())
				.whenNot(CollectionUtils::isEmpty)
				.as((initSqls) -> StringUtils.collectionToDelimitedString(initSqls, "\n"))
				.to((initSql) -> configuration.initSql(initSql));
			map.from(properties.getScriptPlaceholderPrefix())
				.to((prefix) -> configuration.scriptPlaceholderPrefix(prefix));
			map.from(properties.getScriptPlaceholderSuffix())
				.to((suffix) -> configuration.scriptPlaceholderSuffix(suffix));
			configureExecuteInTransaction(configuration, properties, map);
			map.from(properties::getLoggers).to((loggers) -> configuration.loggers(loggers));
			// Flyway Teams properties
			map.from(properties.getBatch()).to((batch) -> configuration.batch(batch));
			map.from(properties.getDryRunOutput()).to((dryRunOutput) -> configuration.dryRunOutput(dryRunOutput));
			map.from(properties.getErrorOverrides())
				.to((errorOverrides) -> configuration.errorOverrides(errorOverrides));
			map.from(properties.getLicenseKey()).to((licenseKey) -> configuration.licenseKey(licenseKey));
			map.from(properties.getStream()).to((stream) -> configuration.stream(stream));
			map.from(properties.getUndoSqlMigrationPrefix())
				.to((undoSqlMigrationPrefix) -> configuration.undoSqlMigrationPrefix(undoSqlMigrationPrefix));
			map.from(properties.getCherryPick()).to((cherryPick) -> configuration.cherryPick(cherryPick));
			map.from(properties.getJdbcProperties())
				.whenNot(Map::isEmpty)
				.to((jdbcProperties) -> configuration.jdbcProperties(jdbcProperties));
			map.from(properties.getKerberosConfigFile())
				.to((configFile) -> configuration.kerberosConfigFile(configFile));
			map.from(properties.getOutputQueryResults())
				.to((outputQueryResults) -> configuration.outputQueryResults(outputQueryResults));
			map.from(properties.getSkipExecutingMigrations())
				.to((skipExecutingMigrations) -> configuration.skipExecutingMigrations(skipExecutingMigrations));
			map.from(properties.getIgnoreMigrationPatterns())
				.whenNot(List::isEmpty)
				.to((ignoreMigrationPatterns) -> configuration
					.ignoreMigrationPatterns(ignoreMigrationPatterns.toArray(new String[0])));
			map.from(properties.getDetectEncoding())
				.to((detectEncoding) -> configuration.detectEncoding(detectEncoding));
		}

		/**
         * Configures whether to execute migrations in a transaction.
         * 
         * @param configuration the FluentConfiguration object to configure
         * @param properties the FlywayProperties object containing the properties
         * @param map the PropertyMapper object used for mapping properties
         */
        private void configureExecuteInTransaction(FluentConfiguration configuration, FlywayProperties properties,
				PropertyMapper map) {
			try {
				map.from(properties.isExecuteInTransaction()).to(configuration::executeInTransaction);
			}
			catch (NoSuchMethodError ex) {
				// Flyway < 9.14
			}
		}

		/**
         * Configures the callbacks for the Flyway configuration.
         * 
         * @param configuration the FluentConfiguration object to configure
         * @param callbacks the list of Callback objects to set as callbacks
         */
        private void configureCallbacks(FluentConfiguration configuration, List<Callback> callbacks) {
			if (!callbacks.isEmpty()) {
				configuration.callbacks(callbacks.toArray(new Callback[0]));
			}
		}

		/**
         * Configures the Java migrations for Flyway.
         * 
         * @param flyway     the FluentConfiguration object for Flyway
         * @param migrations the list of JavaMigration objects to be configured
         */
        private void configureJavaMigrations(FluentConfiguration flyway, List<JavaMigration> migrations) {
			if (!migrations.isEmpty()) {
				flyway.javaMigrations(migrations.toArray(new JavaMigration[0]));
			}
		}

		/**
         * Creates a FlywayMigrationInitializer bean if no other bean of the same type is present.
         * 
         * @param flyway The Flyway instance to be used for migration.
         * @param migrationStrategy The optional FlywayMigrationStrategy to be used for migration.
         * @return The FlywayMigrationInitializer bean.
         */
        @Bean
		@ConditionalOnMissingBean
		public FlywayMigrationInitializer flywayInitializer(Flyway flyway,
				ObjectProvider<FlywayMigrationStrategy> migrationStrategy) {
			return new FlywayMigrationInitializer(flyway, migrationStrategy.getIfAvailable());
		}

	}

	/**
     * LocationResolver class.
     */
    private static class LocationResolver {

		private static final String VENDOR_PLACEHOLDER = "{vendor}";

		private final DataSource dataSource;

		/**
         * Constructs a new LocationResolver object with the specified data source.
         * 
         * @param dataSource the data source to be used for resolving locations
         */
        LocationResolver(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		/**
         * Resolves the locations by replacing vendor locations with database locations.
         * 
         * @param locations the list of locations to be resolved
         * @return the resolved list of locations
         */
        List<String> resolveLocations(List<String> locations) {
			if (usesVendorLocation(locations)) {
				DatabaseDriver databaseDriver = getDatabaseDriver();
				return replaceVendorLocations(locations, databaseDriver);
			}
			return locations;
		}

		/**
         * Replaces the vendor placeholder in the given list of locations with the vendor ID obtained from the database driver.
         * 
         * @param locations the list of locations to be processed
         * @param databaseDriver the database driver used to obtain the vendor ID
         * @return the list of locations with the vendor placeholder replaced by the vendor ID
         */
        private List<String> replaceVendorLocations(List<String> locations, DatabaseDriver databaseDriver) {
			if (databaseDriver == DatabaseDriver.UNKNOWN) {
				return locations;
			}
			String vendor = databaseDriver.getId();
			return locations.stream().map((location) -> location.replace(VENDOR_PLACEHOLDER, vendor)).toList();
		}

		/**
         * Retrieves the database driver used by the current data source.
         * 
         * @return the database driver
         * @throws IllegalStateException if an error occurs while retrieving the database driver
         */
        private DatabaseDriver getDatabaseDriver() {
			try {
				String url = JdbcUtils.extractDatabaseMetaData(this.dataSource, DatabaseMetaData::getURL);
				return DatabaseDriver.fromJdbcUrl(url);
			}
			catch (MetaDataAccessException ex) {
				throw new IllegalStateException(ex);
			}

		}

		/**
         * Checks if the given collection of locations uses the vendor location.
         * 
         * @param locations the collection of locations to check
         * @return true if the collection contains the vendor location, false otherwise
         */
        private boolean usesVendorLocation(Collection<String> locations) {
			for (String location : locations) {
				if (location.contains(VENDOR_PLACEHOLDER)) {
					return true;
				}
			}
			return false;
		}

	}

	/**
	 * Convert a String or Number to a {@link MigrationVersion}.
	 */
	static class StringOrNumberToMigrationVersionConverter implements GenericConverter {

		private static final Set<ConvertiblePair> CONVERTIBLE_TYPES;

		static {
			Set<ConvertiblePair> types = new HashSet<>(2);
			types.add(new ConvertiblePair(String.class, MigrationVersion.class));
			types.add(new ConvertiblePair(Number.class, MigrationVersion.class));
			CONVERTIBLE_TYPES = Collections.unmodifiableSet(types);
		}

		/**
         * Returns the set of convertible types supported by this converter.
         *
         * @return the set of convertible types
         */
        @Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return CONVERTIBLE_TYPES;
		}

		/**
         * Converts a source object to a MigrationVersion object.
         * 
         * @param source the source object to be converted
         * @param sourceType the TypeDescriptor of the source object
         * @param targetType the TypeDescriptor of the target object
         * @return the converted MigrationVersion object
         */
        @Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			String value = ObjectUtils.nullSafeToString(source);
			return MigrationVersion.fromVersion(value);
		}

	}

	/**
     * FlywayDataSourceCondition class.
     */
    static final class FlywayDataSourceCondition extends AnyNestedCondition {

		/**
         * Creates a new instance of FlywayDataSourceCondition.
         * This constructor sets the configuration phase to REGISTER_BEAN.
         */
        FlywayDataSourceCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		/**
         * DataSourceBeanCondition class.
         */
        @ConditionalOnBean(DataSource.class)
		private static final class DataSourceBeanCondition {

		}

		/**
         * JdbcConnectionDetailsCondition class.
         */
        @ConditionalOnBean(JdbcConnectionDetails.class)
		private static final class JdbcConnectionDetailsCondition {

		}

		/**
         * FlywayUrlCondition class.
         */
        @ConditionalOnProperty(prefix = "spring.flyway", name = "url")
		private static final class FlywayUrlCondition {

		}

	}

	/**
     * FlywayAutoConfigurationRuntimeHints class.
     */
    static class FlywayAutoConfigurationRuntimeHints implements RuntimeHintsRegistrar {

		/**
         * Registers the runtime hints for FlywayAutoConfiguration.
         *
         * @param hints the runtime hints to register
         * @param classLoader the class loader to use for resource loading
         */
        @Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.resources().registerPattern("db/migration/*");
		}

	}

	/**
	 * Adapts {@link FlywayProperties} to {@link FlywayConnectionDetails}.
	 */
	static final class PropertiesFlywayConnectionDetails implements FlywayConnectionDetails {

		private final FlywayProperties properties;

		/**
         * Constructs a new instance of PropertiesFlywayConnectionDetails with the specified FlywayProperties.
         *
         * @param properties the FlywayProperties to be used for configuring the connection details
         */
        PropertiesFlywayConnectionDetails(FlywayProperties properties) {
			this.properties = properties;
		}

		/**
         * Returns the username associated with this PropertiesFlywayConnectionDetails object.
         *
         * @return the username
         */
        @Override
		public String getUsername() {
			return this.properties.getUser();
		}

		/**
         * Returns the password for the Flyway connection details.
         *
         * @return the password for the Flyway connection details
         */
        @Override
		public String getPassword() {
			return this.properties.getPassword();
		}

		/**
         * Returns the JDBC URL for the connection details.
         * 
         * @return the JDBC URL
         */
        @Override
		public String getJdbcUrl() {
			return this.properties.getUrl();
		}

		/**
         * Returns the driver class name for the Flyway connection details.
         * 
         * @return the driver class name
         */
        @Override
		public String getDriverClassName() {
			return this.properties.getDriverClassName();
		}

	}

	/**
     * OracleFlywayConfigurationCustomizer class.
     */
    @Order(Ordered.HIGHEST_PRECEDENCE)
	static final class OracleFlywayConfigurationCustomizer implements FlywayConfigurationCustomizer {

		private final FlywayProperties properties;

		/**
         * Constructs a new OracleFlywayConfigurationCustomizer with the specified FlywayProperties.
         *
         * @param properties the FlywayProperties to be used for customizing the Oracle Flyway configuration
         */
        OracleFlywayConfigurationCustomizer(FlywayProperties properties) {
			this.properties = properties;
		}

		/**
         * Customizes the Oracle configuration for Flyway.
         * 
         * @param configuration the FluentConfiguration object to customize
         */
        @Override
		public void customize(FluentConfiguration configuration) {
			Extension<OracleConfigurationExtension> extension = new Extension<>(configuration,
					OracleConfigurationExtension.class, "Oracle");
			Oracle properties = this.properties.getOracle();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(properties::getSqlplus).to(extension.via((ext, sqlplus) -> ext.setSqlplus(sqlplus)));
			map.from(properties::getSqlplusWarn)
				.to(extension.via((ext, sqlplusWarn) -> ext.setSqlplusWarn(sqlplusWarn)));
			map.from(properties::getWalletLocation)
				.to(extension.via((ext, walletLocation) -> ext.setWalletLocation(walletLocation)));
			map.from(properties::getKerberosCacheFile)
				.to(extension.via((ext, kerberosCacheFile) -> ext.setKerberosCacheFile(kerberosCacheFile)));
		}

	}

	/**
     * PostgresqlFlywayConfigurationCustomizer class.
     */
    @Order(Ordered.HIGHEST_PRECEDENCE)
	static final class PostgresqlFlywayConfigurationCustomizer implements FlywayConfigurationCustomizer {

		private final FlywayProperties properties;

		/**
         * Constructs a new instance of PostgresqlFlywayConfigurationCustomizer with the specified FlywayProperties.
         *
         * @param properties the FlywayProperties to be used for configuration customization
         */
        PostgresqlFlywayConfigurationCustomizer(FlywayProperties properties) {
			this.properties = properties;
		}

		/**
         * Customizes the configuration for PostgreSQL Flyway.
         * 
         * @param configuration the FluentConfiguration object to be customized
         */
        @Override
		public void customize(FluentConfiguration configuration) {
			Extension<PostgreSQLConfigurationExtension> extension = new Extension<>(configuration,
					PostgreSQLConfigurationExtension.class, "PostgreSQL");
			Postgresql properties = this.properties.getPostgresql();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(properties::getTransactionalLock)
				.to(extension.via((ext, transactionalLock) -> ext.setTransactionalLock(transactionalLock)));
		}

	}

	/**
     * SqlServerFlywayConfigurationCustomizer class.
     */
    @Order(Ordered.HIGHEST_PRECEDENCE)
	static final class SqlServerFlywayConfigurationCustomizer implements FlywayConfigurationCustomizer {

		private final FlywayProperties properties;

		/**
         * Constructs a new SqlServerFlywayConfigurationCustomizer with the specified FlywayProperties.
         *
         * @param properties the FlywayProperties to be used for customizing the configuration
         */
        SqlServerFlywayConfigurationCustomizer(FlywayProperties properties) {
			this.properties = properties;
		}

		/**
         * Customizes the SQL Server configuration for Flyway.
         * 
         * @param configuration the FluentConfiguration object to be customized
         */
        @Override
		public void customize(FluentConfiguration configuration) {
			Extension<SQLServerConfigurationExtension> extension = new Extension<>(configuration,
					SQLServerConfigurationExtension.class, "SQL Server");
			Sqlserver properties = this.properties.getSqlserver();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(properties::getKerberosLoginFile).to(extension.via(this::setKerberosLoginFile));
		}

		/**
         * Sets the Kerberos login file for the SQL Server configuration.
         * 
         * @param configuration the SQL Server configuration extension
         * @param file the path to the Kerberos login file
         */
        private void setKerberosLoginFile(SQLServerConfigurationExtension configuration, String file) {
			configuration.getKerberos().getLogin().setFile(file);
		}

	}

	/**
	 * Helper class used to map properties to a {@link ConfigurationExtension}.
	 *
	 * @param <E> the extension type
	 */
	static class Extension<E extends ConfigurationExtension> {

		private SingletonSupplier<E> extension;

		/**
         * Creates a new Extension instance with the given configuration, type, and name.
         * 
         * @param configuration the FluentConfiguration object used for retrieving the plugin register
         * @param type the Class object representing the type of the extension
         * @param name the name of the extension
         * @param <E> the type parameter for the extension
         */
        Extension(FluentConfiguration configuration, Class<E> type, String name) {
			this.extension = SingletonSupplier.of(() -> {
				E extension = configuration.getPluginRegister().getPlugin(type);
				Assert.notNull(extension, () -> "Flyway %s extension missing".formatted(name));
				return extension;
			});
		}

		/**
         * Returns a Consumer that adapts a BiConsumer action to a Consumer.
         * 
         * @param action the BiConsumer action to be adapted
         * @return a Consumer that accepts a value and applies the adapted action
         */
        <T> Consumer<T> via(BiConsumer<E, T> action) {
			return (value) -> action.accept(this.extension.get(), value);
		}

	}

}
