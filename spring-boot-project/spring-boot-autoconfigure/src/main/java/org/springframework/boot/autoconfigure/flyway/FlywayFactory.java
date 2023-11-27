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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.extensibility.ConfigurationExtension;
import org.flywaydb.core.internal.database.postgresql.PostgreSQLConfigurationExtension;
import org.flywaydb.database.oracle.OracleConfigurationExtension;
import org.flywaydb.database.sqlserver.SQLServerConfigurationExtension;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.PropertiesFlywayConnectionDetails;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties.Oracle;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties.Postgresql;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties.Sqlserver;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.function.SingletonSupplier;

class FlywayFactory {

	private final ResourceLoader resourceLoader;

	private final ObjectProvider<DataSource> dataSource;

	private final ResourceProviderCustomizer resourceProviderCustomizer;

	FlywayFactory(ResourceLoader resourceLoader, ObjectProvider<DataSource> dataSource, ResourceProviderCustomizer resourceProviderCustomizer) {
		this.resourceLoader = resourceLoader;
		this.dataSource = dataSource;
		this.resourceProviderCustomizer = resourceProviderCustomizer;
	}

	Flyway createFlyway(FlywayProperties properties,
			ObjectProvider<FlywayConnectionDetails> connectionDetails,
			ObjectProvider<DataSource> flywayDataSource,
			ObjectProvider<FlywayConfigurationCustomizer> flywayConfigurationCustomizers,
			ObjectProvider<Callback> callbacks,
			ObjectProvider<JavaMigration> javaMigrations) {
		FluentConfiguration configuration = new FluentConfiguration(this.resourceLoader.getClassLoader());
		configureDataSource(configuration, flywayDataSource.getIfAvailable(), this.dataSource.getIfUnique(),
				connectionDetails.getIfAvailable(() -> new PropertiesFlywayConnectionDetails(properties)));
		configureProperties(configuration, properties);
		configureCallbacks(configuration, callbacks.orderedStream().toList());
		configureJavaMigrations(configuration, javaMigrations.orderedStream().toList());
		createDatabaseCustomizers(properties).forEach((customizer) -> customizer.customize(configuration));
		flywayConfigurationCustomizers.orderedStream().forEach((customizer) -> customizer.customize(configuration));
		this.resourceProviderCustomizer.customize(configuration);
		return configuration.load();
	}

	private void configureDataSource(FluentConfiguration configuration, DataSource flywayDataSource,
			DataSource dataSource, FlywayConnectionDetails connectionDetails) {
		DataSource migrationDataSource = getMigrationDataSource(flywayDataSource, dataSource, connectionDetails);
		configuration.dataSource(migrationDataSource);
	}

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

	private void configureExecuteInTransaction(FluentConfiguration configuration, FlywayProperties properties,
			PropertyMapper map) {
		try {
			map.from(properties.isExecuteInTransaction()).to(configuration::executeInTransaction);
		}
		catch (NoSuchMethodError ex) {
			// Flyway < 9.14
		}
	}

	private void configureCallbacks(FluentConfiguration configuration, List<Callback> callbacks) {
		if (!callbacks.isEmpty()) {
			configuration.callbacks(callbacks.toArray(new Callback[0]));
		}
	}

	private void configureJavaMigrations(FluentConfiguration flyway, List<JavaMigration> migrations) {
		if (!migrations.isEmpty()) {
			flyway.javaMigrations(migrations.toArray(new JavaMigration[0]));
		}
	}

	private static class LocationResolver {

		private static final String VENDOR_PLACEHOLDER = "{vendor}";

		private final DataSource dataSource;

		LocationResolver(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		List<String> resolveLocations(List<String> locations) {
			if (usesVendorLocation(locations)) {
				DatabaseDriver databaseDriver = getDatabaseDriver();
				return replaceVendorLocations(locations, databaseDriver);
			}
			return locations;
		}

		private List<String> replaceVendorLocations(List<String> locations, DatabaseDriver databaseDriver) {
			if (databaseDriver == DatabaseDriver.UNKNOWN) {
				return locations;
			}
			String vendor = databaseDriver.getId();
			return locations.stream().map((location) -> location.replace(VENDOR_PLACEHOLDER, vendor)).toList();
		}

		private DatabaseDriver getDatabaseDriver() {
			try {
				String url = JdbcUtils.extractDatabaseMetaData(this.dataSource, DatabaseMetaData::getURL);
				return DatabaseDriver.fromJdbcUrl(url);
			}
			catch (MetaDataAccessException ex) {
				throw new IllegalStateException(ex);
			}

		}

		private boolean usesVendorLocation(Collection<String> locations) {
			for (String location : locations) {
				if (location.contains(VENDOR_PLACEHOLDER)) {
					return true;
				}
			}
			return false;
		}

	}

	private List<FlywayConfigurationCustomizer> createDatabaseCustomizers(FlywayProperties properties) {
		List<FlywayConfigurationCustomizer> customizers = new ArrayList<>();
		if (isClassLoadable("org.flywaydb.database.sqlserver.SQLServerConfigurationExtension")) {
			customizers.add(new SqlServerFlywayConfigurationCustomizer(properties));
		}
		if (isClassLoadable("org.flywaydb.database.oracle.OracleConfigurationExtension")) {
			customizers.add(new OracleFlywayConfigurationCustomizer(properties));
		}
		if (isClassLoadable("org.flywaydb.core.internal.database.postgresql.PostgreSQLConfigurationExtension")) {
			customizers.add(new PostgresqlFlywayConfigurationCustomizer(properties));
		}
		return customizers;
	}

	private boolean isClassLoadable(String className) {
		try {
			Class.forName(className, false, getClass().getClassLoader());
			return true;
		}
		catch (Throwable ex) {
			return false;
		}
	}

	static final class OracleFlywayConfigurationCustomizer implements FlywayConfigurationCustomizer {

		private final FlywayProperties properties;

		OracleFlywayConfigurationCustomizer(FlywayProperties properties) {
			this.properties = properties;
		}

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

	static final class PostgresqlFlywayConfigurationCustomizer implements FlywayConfigurationCustomizer {

		private final FlywayProperties properties;

		PostgresqlFlywayConfigurationCustomizer(FlywayProperties properties) {
			this.properties = properties;
		}

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

	static final class SqlServerFlywayConfigurationCustomizer implements FlywayConfigurationCustomizer {

		private final FlywayProperties properties;

		SqlServerFlywayConfigurationCustomizer(FlywayProperties properties) {
			this.properties = properties;
		}

		@Override
		public void customize(FluentConfiguration configuration) {
			Extension<SQLServerConfigurationExtension> extension = new Extension<>(configuration,
					SQLServerConfigurationExtension.class, "SQL Server");
			Sqlserver properties = this.properties.getSqlserver();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(properties::getKerberosLoginFile).to(extension.via(this::setKerberosLoginFile));
		}

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

		Extension(FluentConfiguration configuration, Class<E> type, String name) {
			this.extension = SingletonSupplier.of(() -> {
				E extension = configuration.getPluginRegister().getPlugin(type);
				Assert.notNull(extension, () -> "Flyway %s extension missing".formatted(name));
				return extension;
			});
		}

		<T> Consumer<T> via(BiConsumer<E, T> action) {
			return (value) -> action.accept(this.extension.get(), value);
		}

	}

}
