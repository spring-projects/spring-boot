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

	@Bean
	@ConfigurationPropertiesBinding
	public StringOrNumberToMigrationVersionConverter stringOrNumberMigrationVersionConverter() {
		return new StringOrNumberToMigrationVersionConverter();
	}

	@Bean
	public FlywaySchemaManagementProvider flywayDefaultDdlModeProvider(ObjectProvider<Flyway> flyways) {
		return new FlywaySchemaManagementProvider(flyways);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JdbcUtils.class)
	@ConditionalOnMissingBean(Flyway.class)
	@EnableConfigurationProperties(FlywayProperties.class)
	public static class FlywayConfiguration {

		private final FlywayProperties properties;

		FlywayConfiguration(FlywayProperties properties) {
			this.properties = properties;
		}

		@Bean
		ResourceProviderCustomizer resourceProviderCustomizer() {
			return new ResourceProviderCustomizer();
		}

		@Bean
		@ConditionalOnMissingBean(FlywayConnectionDetails.class)
		PropertiesFlywayConnectionDetails flywayConnectionDetails() {
			return new PropertiesFlywayConnectionDetails(this.properties);
		}

		@Bean
		@ConditionalOnClass(name = "org.flywaydb.database.sqlserver.SQLServerConfigurationExtension")
		SqlServerFlywayConfigurationCustomizer sqlServerFlywayConfigurationCustomizer() {
			return new SqlServerFlywayConfigurationCustomizer(this.properties);
		}

		@Bean
		@ConditionalOnClass(name = "org.flywaydb.database.oracle.OracleConfigurationExtension")
		OracleFlywayConfigurationCustomizer oracleFlywayConfigurationCustomizer() {
			return new OracleFlywayConfigurationCustomizer(this.properties);
		}

		@Bean
		@ConditionalOnClass(name = "org.flywaydb.core.internal.database.postgresql.PostgreSQLConfigurationExtension")
		PostgresqlFlywayConfigurationCustomizer postgresqlFlywayConfigurationCustomizer() {
			return new PostgresqlFlywayConfigurationCustomizer(this.properties);
		}

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

		private void configureProperties(FluentConfiguration configuration, FlywayProperties properties) {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			String[] locations = new LocationResolver(configuration.getDataSource())
				.resolveLocations(properties.getLocations())
				.toArray(new String[0]);
			map.from(properties.isFailOnMissingLocations()).to(configuration::failOnMissingLocations);
			map.from(locations).to(configuration::locations);
			map.from(properties.getEncoding()).to(configuration::encoding);
			map.from(properties.getConnectRetries()).to(configuration::connectRetries);
			map.from(properties.getConnectRetriesInterval())
				.as(Duration::getSeconds)
				.as(Long::intValue)
				.to(configuration::connectRetriesInterval);
			map.from(properties.getLockRetryCount()).to(configuration::lockRetryCount);
			map.from(properties.getDefaultSchema()).to(configuration::defaultSchema);
			map.from(properties.getSchemas()).as(StringUtils::toStringArray).to(configuration::schemas);
			map.from(properties.isCreateSchemas()).to(configuration::createSchemas);
			map.from(properties.getTable()).to(configuration::table);
			map.from(properties.getTablespace()).to(configuration::tablespace);
			map.from(properties.getBaselineDescription()).to(configuration::baselineDescription);
			map.from(properties.getBaselineVersion()).to(configuration::baselineVersion);
			map.from(properties.getInstalledBy()).to(configuration::installedBy);
			map.from(properties.getPlaceholders()).to(configuration::placeholders);
			map.from(properties.getPlaceholderPrefix()).to(configuration::placeholderPrefix);
			map.from(properties.getPlaceholderSuffix()).to(configuration::placeholderSuffix);
			map.from(properties.getPlaceholderSeparator()).to(configuration::placeholderSeparator);
			map.from(properties.isPlaceholderReplacement()).to(configuration::placeholderReplacement);
			map.from(properties.getSqlMigrationPrefix()).to(configuration::sqlMigrationPrefix);
			map.from(properties.getSqlMigrationSuffixes())
				.as(StringUtils::toStringArray)
				.to(configuration::sqlMigrationSuffixes);
			map.from(properties.getSqlMigrationSeparator()).to(configuration::sqlMigrationSeparator);
			map.from(properties.getRepeatableSqlMigrationPrefix()).to(configuration::repeatableSqlMigrationPrefix);
			map.from(properties.getTarget()).to(configuration::target);
			map.from(properties.isBaselineOnMigrate()).to(configuration::baselineOnMigrate);
			map.from(properties.isCleanDisabled()).to(configuration::cleanDisabled);
			map.from(properties.isCleanOnValidationError()).to(configuration::cleanOnValidationError);
			map.from(properties.isGroup()).to(configuration::group);
			map.from(properties.isMixed()).to(configuration::mixed);
			map.from(properties.isOutOfOrder()).to(configuration::outOfOrder);
			map.from(properties.isSkipDefaultCallbacks()).to(configuration::skipDefaultCallbacks);
			map.from(properties.isSkipDefaultResolvers()).to(configuration::skipDefaultResolvers);
			map.from(properties.isValidateMigrationNaming()).to(configuration::validateMigrationNaming);
			map.from(properties.isValidateOnMigrate()).to(configuration::validateOnMigrate);
			map.from(properties.getInitSqls())
				.whenNot(CollectionUtils::isEmpty)
				.as((initSqls) -> StringUtils.collectionToDelimitedString(initSqls, "\n"))
				.to(configuration::initSql);
			map.from(properties.getScriptPlaceholderPrefix())
				.to((prefix) -> configuration.scriptPlaceholderPrefix(prefix));
			map.from(properties.getScriptPlaceholderSuffix())
				.to((suffix) -> configuration.scriptPlaceholderSuffix(suffix));
			configureExecuteInTransaction(configuration, properties, map);
			map.from(properties::getLoggers).to(configuration::loggers);
			// Flyway Teams properties
			map.from(properties.getBatch()).to(configuration::batch);
			map.from(properties.getDryRunOutput()).to(configuration::dryRunOutput);
			map.from(properties.getErrorOverrides()).to(configuration::errorOverrides);
			map.from(properties.getLicenseKey()).to(configuration::licenseKey);
			map.from(properties.getStream()).to(configuration::stream);
			map.from(properties.getUndoSqlMigrationPrefix()).to(configuration::undoSqlMigrationPrefix);
			map.from(properties.getCherryPick()).to(configuration::cherryPick);
			map.from(properties.getJdbcProperties()).whenNot(Map::isEmpty).to(configuration::jdbcProperties);
			map.from(properties.getKerberosConfigFile()).to(configuration::kerberosConfigFile);
			map.from(properties.getOutputQueryResults()).to(configuration::outputQueryResults);
			map.from(properties.getSkipExecutingMigrations()).to(configuration::skipExecutingMigrations);
			map.from(properties.getIgnoreMigrationPatterns())
				.whenNot(List::isEmpty)
				.as((patterns) -> patterns.toArray(new String[0]))
				.to(configuration::ignoreMigrationPatterns);
			map.from(properties.getDetectEncoding()).to(configuration::detectEncoding);
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

		@Bean
		@ConditionalOnMissingBean
		public FlywayMigrationInitializer flywayInitializer(Flyway flyway,
				ObjectProvider<FlywayMigrationStrategy> migrationStrategy) {
			return new FlywayMigrationInitializer(flyway, migrationStrategy.getIfAvailable());
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

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return CONVERTIBLE_TYPES;
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			String value = ObjectUtils.nullSafeToString(source);
			return MigrationVersion.fromVersion(value);
		}

	}

	static final class FlywayDataSourceCondition extends AnyNestedCondition {

		FlywayDataSourceCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnBean(DataSource.class)
		private static final class DataSourceBeanCondition {

		}

		@ConditionalOnBean(JdbcConnectionDetails.class)
		private static final class JdbcConnectionDetailsCondition {

		}

		@ConditionalOnProperty(prefix = "spring.flyway", name = "url")
		private static final class FlywayUrlCondition {

		}

	}

	static class FlywayAutoConfigurationRuntimeHints implements RuntimeHintsRegistrar {

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

		PropertiesFlywayConnectionDetails(FlywayProperties properties) {
			this.properties = properties;
		}

		@Override
		public String getUsername() {
			return this.properties.getUser();
		}

		@Override
		public String getPassword() {
			return this.properties.getPassword();
		}

		@Override
		public String getJdbcUrl() {
			return this.properties.getUrl();
		}

		@Override
		public String getDriverClassName() {
			return this.properties.getDriverClassName();
		}

	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
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
			map.from(properties::getSqlplus).to(extension.via(OracleConfigurationExtension::setSqlplus));
			map.from(properties::getSqlplusWarn).to(extension.via(OracleConfigurationExtension::setSqlplusWarn));
			map.from(properties::getWalletLocation).to(extension.via(OracleConfigurationExtension::setWalletLocation));
			map.from(properties::getKerberosCacheFile)
				.to(extension.via(OracleConfigurationExtension::setKerberosCacheFile));
		}

	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
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
				.to(extension.via(PostgreSQLConfigurationExtension::setTransactionalLock));
		}

	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
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
