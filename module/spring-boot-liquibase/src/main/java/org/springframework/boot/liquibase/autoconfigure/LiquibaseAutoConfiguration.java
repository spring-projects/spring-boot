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

import javax.sql.DataSource;

import liquibase.Liquibase;
import liquibase.UpdateSummaryEnum;
import liquibase.UpdateSummaryOutputEnum;
import liquibase.change.DatabaseChange;
import liquibase.integration.spring.Customizer;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.ui.UIServiceEnum;
import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration.LiquibaseAutoConfigurationRuntimeHints;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration.LiquibaseDataSourceCondition;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
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
 * @author Ahmed Ashour
 * @since 4.0.0
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass({ SpringLiquibase.class, DatabaseChange.class })
@ConditionalOnBooleanProperty(name = "spring.liquibase.enabled", matchIfMissing = true)
@Conditional(LiquibaseDataSourceCondition.class)
@Import(DatabaseInitializationDependencyConfigurer.class)
@ImportRuntimeHints(LiquibaseAutoConfigurationRuntimeHints.class)
public final class LiquibaseAutoConfiguration {

	@Bean
	LiquibaseSchemaManagementProvider liquibaseDefaultDdlModeProvider(ObjectProvider<SpringLiquibase> liquibases) {
		return new LiquibaseSchemaManagementProvider(liquibases);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ConnectionCallback.class)
	@ConditionalOnMissingBean(SpringLiquibase.class)
	@EnableConfigurationProperties(LiquibaseProperties.class)
	static class LiquibaseConfiguration {

		@Bean
		@ConditionalOnMissingBean(LiquibaseConnectionDetails.class)
		PropertiesLiquibaseConnectionDetails liquibaseConnectionDetails(LiquibaseProperties properties) {
			return new PropertiesLiquibaseConnectionDetails(properties);
		}

		@Bean
		SpringLiquibase liquibase(ObjectProvider<DataSource> dataSource,
				@LiquibaseDataSource ObjectProvider<DataSource> liquibaseDataSource, LiquibaseProperties properties,
				ObjectProvider<SpringLiquibaseCustomizer> customizers, LiquibaseConnectionDetails connectionDetails) {
			SpringLiquibase liquibase = createSpringLiquibase(liquibaseDataSource.getIfAvailable(),
					dataSource.getIfUnique(), connectionDetails);
			liquibase.setChangeLog(properties.getChangeLog());
			liquibase.setClearCheckSums(properties.isClearChecksums());
			if (!CollectionUtils.isEmpty(properties.getContexts())) {
				liquibase.setContexts(StringUtils.collectionToCommaDelimitedString(properties.getContexts()));
			}
			liquibase.setDefaultSchema(properties.getDefaultSchema());
			liquibase.setLiquibaseSchema(properties.getLiquibaseSchema());
			liquibase.setLiquibaseTablespace(properties.getLiquibaseTablespace());
			liquibase.setDatabaseChangeLogTable(properties.getDatabaseChangeLogTable());
			liquibase.setDatabaseChangeLogLockTable(properties.getDatabaseChangeLogLockTable());
			liquibase.setDropFirst(properties.isDropFirst());
			liquibase.setShouldRun(properties.isEnabled());
			if (!CollectionUtils.isEmpty(properties.getLabelFilter())) {
				liquibase.setLabelFilter(StringUtils.collectionToCommaDelimitedString(properties.getLabelFilter()));
			}
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
			if (properties.getAnalyticsEnabled() != null) {
				liquibase.setAnalyticsEnabled(properties.getAnalyticsEnabled());
			}
			if (properties.getLicenseKey() != null) {
				liquibase.setLicenseKey(properties.getLicenseKey());
			}
			customizers.orderedStream().forEach((customizer) -> customizer.customize(liquibase));
			return liquibase;
		}

		private SpringLiquibase createSpringLiquibase(@Nullable DataSource liquibaseDataSource,
				@Nullable DataSource dataSource, LiquibaseConnectionDetails connectionDetails) {
			DataSource migrationDataSource = getMigrationDataSource(liquibaseDataSource, dataSource, connectionDetails);
			SpringLiquibase liquibase = (migrationDataSource == liquibaseDataSource
					|| migrationDataSource == dataSource) ? new SpringLiquibase()
							: new DataSourceClosingSpringLiquibase();
			liquibase.setDataSource(migrationDataSource);
			return liquibase;
		}

		private DataSource getMigrationDataSource(@Nullable DataSource liquibaseDataSource,
				@Nullable DataSource dataSource, LiquibaseConnectionDetails connectionDetails) {
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

	@Bean
	static BeanFactoryPostProcessor liquibaseConfigurationValueProviderRegistrar(Environment environment) {

		return (beanFactory) -> {
			var liquibaseConfiguration = liquibase.Scope.getCurrentScope()
				.getSingleton(liquibase.configuration.LiquibaseConfiguration.class);

			// Remove any previously registered instance of our provider class
			liquibaseConfiguration.getProviders()
				.stream()
				.filter((provider) -> provider.getClass() == EnvironmentConfigurationValueProvider.class)
				.toList()
				.forEach(liquibaseConfiguration::unregisterProvider);

			liquibaseConfiguration.registerProvider(new EnvironmentConfigurationValueProvider(environment));
		};
	}

	@ConditionalOnClass(Customizer.class)
	@Configuration(proxyBeanMethods = false)
	static class CustomizerConfiguration {

		@Bean
		@ConditionalOnBean(Customizer.class)
		SpringLiquibaseCustomizer springLiquibaseCustomizer(Customizer<Liquibase> customizer) {
			return (springLiquibase) -> springLiquibase.setCustomizer(customizer);
		}

	}

	static final class LiquibaseDataSourceCondition extends AnyNestedCondition {

		LiquibaseDataSourceCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnBean(DataSource.class)
		private static final class DataSourceBeanCondition {

		}

		@ConditionalOnBean(JdbcConnectionDetails.class)
		private static final class JdbcConnectionDetailsCondition {

		}

		@ConditionalOnProperty("spring.liquibase.url")
		private static final class LiquibaseUrlCondition {

		}

	}

	static class LiquibaseAutoConfigurationRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			hints.resources().registerPattern("db/changelog/**");
		}

	}

	/**
	 * Adapts {@link LiquibaseProperties} to {@link LiquibaseConnectionDetails}.
	 */
	static final class PropertiesLiquibaseConnectionDetails implements LiquibaseConnectionDetails {

		private final LiquibaseProperties properties;

		PropertiesLiquibaseConnectionDetails(LiquibaseProperties properties) {
			this.properties = properties;
		}

		@Override
		public @Nullable String getUsername() {
			return this.properties.getUser();
		}

		@Override
		public @Nullable String getPassword() {
			return this.properties.getPassword();
		}

		@Override
		public @Nullable String getJdbcUrl() {
			return this.properties.getUrl();
		}

		@Override
		public @Nullable String getDriverClassName() {
			String driverClassName = this.properties.getDriverClassName();
			return (driverClassName != null) ? driverClassName : LiquibaseConnectionDetails.super.getDriverClassName();
		}

	}

	@FunctionalInterface
	interface SpringLiquibaseCustomizer {

		/**
		 * Customize the given {@link SpringLiquibase} instance.
		 * @param springLiquibase the instance to configure
		 */
		void customize(SpringLiquibase springLiquibase);

	}

}
