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

package org.springframework.boot.autoconfigure.liquibase;

import javax.sql.DataSource;

import liquibase.change.DatabaseChange;
import liquibase.integration.spring.SpringLiquibase;

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

	@Bean
	public LiquibaseSchemaManagementProvider liquibaseDefaultDdlModeProvider(
			ObjectProvider<SpringLiquibase> liquibases) {
		return new LiquibaseSchemaManagementProvider(liquibases);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ConnectionCallback.class)
	@ConditionalOnMissingBean(SpringLiquibase.class)
	@EnableConfigurationProperties(LiquibaseProperties.class)
	public static class LiquibaseConfiguration {

		@Bean
		@ConditionalOnMissingBean(LiquibaseConnectionDetails.class)
		PropertiesLiquibaseConnectionDetails liquibaseConnectionDetails(LiquibaseProperties properties,
				ObjectProvider<JdbcConnectionDetails> jdbcConnectionDetails) {
			return new PropertiesLiquibaseConnectionDetails(properties);
		}

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
			return liquibase;
		}

		private SpringLiquibase createSpringLiquibase(DataSource liquibaseDataSource, DataSource dataSource,
				LiquibaseConnectionDetails connectionDetails) {
			DataSource migrationDataSource = getMigrationDataSource(liquibaseDataSource, dataSource, connectionDetails);
			SpringLiquibase liquibase = (migrationDataSource == liquibaseDataSource
					|| migrationDataSource == dataSource) ? new SpringLiquibase()
							: new DataSourceClosingSpringLiquibase();
			liquibase.setDataSource(migrationDataSource);
			return liquibase;
		}

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

		@ConditionalOnProperty(prefix = "spring.liquibase", name = "url")
		private static final class LiquibaseUrlCondition {

		}

	}

	static class LiquibaseAutoConfigurationRuntimeHints implements RuntimeHintsRegistrar {

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

		PropertiesLiquibaseConnectionDetails(LiquibaseProperties properties) {
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
			String driverClassName = this.properties.getDriverClassName();
			return (driverClassName != null) ? driverClassName : LiquibaseConnectionDetails.super.getDriverClassName();
		}

	}

}
