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

import javax.sql.DataSource;

import liquibase.UpdateSummaryEnum;
import liquibase.UpdateSummaryOutputEnum;
import liquibase.change.DatabaseChange;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.ui.UIServiceEnum;

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
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration.LiquibaseAutoConfigurationRuntimeHints;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration.LiquibaseDataSourceCondition;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Liquibase.
 *
 * @author Marcel Overdijk
 * @author Dave Syer
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Andy Wilkinson
 * @author Dominic Gunn
 * @author Dan Zheng
 * @author András Deák
 * @author Ferenc Gratzer
 * @author Evgeniy Cheban
 * @author Moritz Halbritter
 * @since 1.1.0
 */
@AutoConfiguration(after = { DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
@ConditionalOnClass({ SpringLiquibase.class, DatabaseChange.class })
@ConditionalOnProperty(prefix = "spring.liquibase", name = "enabled", matchIfMissing = true)
@Conditional(LiquibaseDataSourceCondition.class)
@Import(DatabaseInitializationDependencyConfigurer.class)
@ImportRuntimeHints(LiquibaseAutoConfigurationRuntimeHints.class)
public class LiquibaseAutoConfiguration {

	/**
	 * Creates a LiquibaseSchemaManagementProvider bean that provides the default DDL mode
	 * for Liquibase.
	 * @param liquibases the ObjectProvider for SpringLiquibase instances
	 * @return the LiquibaseSchemaManagementProvider bean
	 */
	@Bean
	public LiquibaseSchemaManagementProvider liquibaseDefaultDdlModeProvider(
			ObjectProvider<SpringLiquibase> liquibases) {
		return new LiquibaseSchemaManagementProvider(liquibases);
	}

	/**
	 * LiquibaseConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ConnectionCallback.class)
	@ConditionalOnMissingBean(SpringLiquibase.class)
	@EnableConfigurationProperties(LiquibaseProperties.class)
	public static class LiquibaseConfiguration {

		/**
		 * Creates a new instance of {@link PropertiesLiquibaseConnectionDetails} if no
		 * bean of type {@link LiquibaseConnectionDetails} is present.
		 * @param properties The {@link LiquibaseProperties} object containing the
		 * liquibase configuration properties.
		 * @param jdbcConnectionDetails The {@link JdbcConnectionDetails} object provider.
		 * @return The {@link PropertiesLiquibaseConnectionDetails} object.
		 */
		@Bean
		@ConditionalOnMissingBean(LiquibaseConnectionDetails.class)
		PropertiesLiquibaseConnectionDetails liquibaseConnectionDetails(LiquibaseProperties properties,
				ObjectProvider<JdbcConnectionDetails> jdbcConnectionDetails) {
			return new PropertiesLiquibaseConnectionDetails(properties);
		}

		/**
		 * Creates and configures a SpringLiquibase bean.
		 * @param dataSource the data source object provider
		 * @param liquibaseDataSource the liquibase data source object provider
		 * @param properties the liquibase properties
		 * @param connectionDetails the liquibase connection details
		 * @return the configured SpringLiquibase bean
		 */
		@Bean
		public SpringLiquibase liquibase(ObjectProvider<DataSource> dataSource,
				@LiquibaseDataSource ObjectProvider<DataSource> liquibaseDataSource, LiquibaseProperties properties,
				LiquibaseConnectionDetails connectionDetails) {
			SpringLiquibase liquibase = createSpringLiquibase(liquibaseDataSource.getIfAvailable(),
					dataSource.getIfUnique(), connectionDetails);
			liquibase.setChangeLog(properties.getChangeLog());
			liquibase.setClearCheckSums(properties.isClearChecksums());
			liquibase.setContexts(properties.getContexts());
			liquibase.setDefaultSchema(properties.getDefaultSchema());
			liquibase.setLiquibaseSchema(properties.getLiquibaseSchema());
			liquibase.setLiquibaseTablespace(properties.getLiquibaseTablespace());
			liquibase.setDatabaseChangeLogTable(properties.getDatabaseChangeLogTable());
			liquibase.setDatabaseChangeLogLockTable(properties.getDatabaseChangeLogLockTable());
			liquibase.setDropFirst(properties.isDropFirst());
			liquibase.setShouldRun(properties.isEnabled());
			liquibase.setLabelFilter(properties.getLabelFilter());
			liquibase.setChangeLogParameters(properties.getParameters());
			liquibase.setRollbackFile(properties.getRollbackFile());
			liquibase.setTestRollbackOnUpdate(properties.isTestRollbackOnUpdate());
			liquibase.setTag(properties.getTag());
			if (properties.getShowSummary() != null) {
				liquibase.setShowSummary(UpdateSummaryEnum.valueOf(properties.getShowSummary().name()));
			}
			if (properties.getShowSummaryOutput() != null) {
				liquibase
					.setShowSummaryOutput(UpdateSummaryOutputEnum.valueOf(properties.getShowSummaryOutput().name()));
			}
			if (properties.getUiService() != null) {
				liquibase.setUiService(UIServiceEnum.valueOf(properties.getUiService().name()));
			}
			return liquibase;
		}

		/**
		 * Creates a SpringLiquibase instance for performing database migrations.
		 * @param liquibaseDataSource the DataSource for Liquibase
		 * @param dataSource the main DataSource for the application
		 * @param connectionDetails the LiquibaseConnectionDetails containing the
		 * connection details
		 * @return a SpringLiquibase instance configured with the appropriate DataSource
		 */
		private SpringLiquibase createSpringLiquibase(DataSource liquibaseDataSource, DataSource dataSource,
				LiquibaseConnectionDetails connectionDetails) {
			DataSource migrationDataSource = getMigrationDataSource(liquibaseDataSource, dataSource, connectionDetails);
			SpringLiquibase liquibase = (migrationDataSource == liquibaseDataSource
					|| migrationDataSource == dataSource) ? new SpringLiquibase()
							: new DataSourceClosingSpringLiquibase();
			liquibase.setDataSource(migrationDataSource);
			return liquibase;
		}

		/**
		 * Returns the migration DataSource to be used by Liquibase. If a
		 * liquibaseDataSource is provided, it will be returned. Otherwise, if a JDBC URL
		 * is provided in the connectionDetails, a new DataSource will be created using
		 * the SimpleDriverDataSource class. If a username is provided in the
		 * connectionDetails and a dataSource is provided, a new DataSource will be
		 * created using the SimpleDriverDataSource class derived from the provided
		 * dataSource. If none of the above conditions are met and a dataSource is not
		 * provided, an IllegalStateException will be thrown.
		 * @param liquibaseDataSource The DataSource to be used by Liquibase, if provided.
		 * @param dataSource The DataSource to be used if a liquibaseDataSource is not
		 * provided and a username is provided in the connectionDetails.
		 * @param connectionDetails The LiquibaseConnectionDetails containing the JDBC URL
		 * and username.
		 * @return The migration DataSource to be used by Liquibase.
		 * @throws IllegalStateException If a dataSource is not provided and a
		 * liquibaseDataSource is not provided and a username is not provided in the
		 * connectionDetails.
		 */
		private DataSource getMigrationDataSource(DataSource liquibaseDataSource, DataSource dataSource,
				LiquibaseConnectionDetails connectionDetails) {
			if (liquibaseDataSource != null) {
				return liquibaseDataSource;
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
			Assert.state(dataSource != null, "Liquibase migration DataSource missing");
			return dataSource;
		}

		/**
		 * Applies the connection details to the given DataSourceBuilder.
		 * @param connectionDetails the LiquibaseConnectionDetails object containing the
		 * connection details
		 * @param builder the DataSourceBuilder to apply the connection details to
		 */
		private void applyConnectionDetails(LiquibaseConnectionDetails connectionDetails,
				DataSourceBuilder<?> builder) {
			builder.username(connectionDetails.getUsername());
			builder.password(connectionDetails.getPassword());
			String driverClassName = connectionDetails.getDriverClassName();
			if (StringUtils.hasText(driverClassName)) {
				builder.driverClassName(driverClassName);
			}
		}

	}

	/**
	 * LiquibaseDataSourceCondition class.
	 */
	static final class LiquibaseDataSourceCondition extends AnyNestedCondition {

		/**
		 * Constructs a new LiquibaseDataSourceCondition with the specified configuration
		 * phase.
		 * @param configurationPhase the configuration phase for the condition
		 */
		LiquibaseDataSourceCondition() {
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
		 * LiquibaseUrlCondition class.
		 */
		@ConditionalOnProperty(prefix = "spring.liquibase", name = "url")
		private static final class LiquibaseUrlCondition {

		}

	}

	/**
	 * LiquibaseAutoConfigurationRuntimeHints class.
	 */
	static class LiquibaseAutoConfigurationRuntimeHints implements RuntimeHintsRegistrar {

		/**
		 * Registers the runtime hints for Liquibase auto configuration.
		 * @param hints the runtime hints to register
		 * @param classLoader the class loader to use for resource loading
		 */
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.resources().registerPattern("db/changelog/*");
		}

	}

	/**
	 * Adapts {@link LiquibaseProperties} to {@link LiquibaseConnectionDetails}.
	 */
	static final class PropertiesLiquibaseConnectionDetails implements LiquibaseConnectionDetails {

		private final LiquibaseProperties properties;

		/**
		 * Constructs a new instance of PropertiesLiquibaseConnectionDetails with the
		 * specified LiquibaseProperties.
		 * @param properties the LiquibaseProperties to be set for this instance
		 */
		PropertiesLiquibaseConnectionDetails(LiquibaseProperties properties) {
			this.properties = properties;
		}

		/**
		 * Returns the username for the connection details.
		 * @return the username
		 */
		@Override
		public String getUsername() {
			return this.properties.getUser();
		}

		/**
		 * Returns the password for the Liquibase connection details.
		 * @return the password for the Liquibase connection details
		 */
		@Override
		public String getPassword() {
			return this.properties.getPassword();
		}

		/**
		 * Returns the JDBC URL for the connection.
		 * @return the JDBC URL
		 */
		@Override
		public String getJdbcUrl() {
			return this.properties.getUrl();
		}

		/**
		 * Returns the driver class name for the connection.
		 * @return the driver class name
		 */
		@Override
		public String getDriverClassName() {
			String driverClassName = this.properties.getDriverClassName();
			return (driverClassName != null) ? driverClassName : LiquibaseConnectionDetails.super.getDriverClassName();
		}

	}

}
