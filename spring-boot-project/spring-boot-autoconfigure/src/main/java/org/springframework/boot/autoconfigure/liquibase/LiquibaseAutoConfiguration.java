/*
 * Copyright 2012-2021 the original author or authors.
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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration.LiquibaseDataSourceCondition;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
 * @since 1.1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ SpringLiquibase.class, DatabaseChange.class })
@ConditionalOnProperty(prefix = "spring.liquibase", name = "enabled", matchIfMissing = true)
@Conditional(LiquibaseDataSourceCondition.class)
@AutoConfigureAfter({ DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
@Import(DatabaseInitializationDependencyConfigurer.class)
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

		private final LiquibaseProperties properties;

		public LiquibaseConfiguration(LiquibaseProperties properties) {
			this.properties = properties;
		}

		@Bean
		public SpringLiquibase liquibase(ObjectProvider<DataSource> dataSource,
				@LiquibaseDataSource ObjectProvider<DataSource> liquibaseDataSource) {
			SpringLiquibase liquibase = createSpringLiquibase(liquibaseDataSource.getIfAvailable(),
					dataSource.getIfUnique());
			liquibase.setChangeLog(this.properties.getChangeLog());
			liquibase.setClearCheckSums(this.properties.isClearChecksums());
			liquibase.setContexts(this.properties.getContexts());
			liquibase.setDefaultSchema(this.properties.getDefaultSchema());
			liquibase.setLiquibaseSchema(this.properties.getLiquibaseSchema());
			liquibase.setLiquibaseTablespace(this.properties.getLiquibaseTablespace());
			liquibase.setDatabaseChangeLogTable(this.properties.getDatabaseChangeLogTable());
			liquibase.setDatabaseChangeLogLockTable(this.properties.getDatabaseChangeLogLockTable());
			liquibase.setDropFirst(this.properties.isDropFirst());
			liquibase.setShouldRun(this.properties.isEnabled());
			liquibase.setLabels(this.properties.getLabels());
			liquibase.setChangeLogParameters(this.properties.getParameters());
			liquibase.setRollbackFile(this.properties.getRollbackFile());
			liquibase.setTestRollbackOnUpdate(this.properties.isTestRollbackOnUpdate());
			liquibase.setTag(this.properties.getTag());
			return liquibase;
		}

		private SpringLiquibase createSpringLiquibase(DataSource liquibaseDataSource, DataSource dataSource) {
			LiquibaseProperties properties = this.properties;
			DataSource migrationDataSource = getMigrationDataSource(liquibaseDataSource, dataSource, properties);
			SpringLiquibase liquibase = (migrationDataSource == liquibaseDataSource
					|| migrationDataSource == dataSource) ? new SpringLiquibase()
							: new DataSourceClosingSpringLiquibase();
			liquibase.setDataSource(migrationDataSource);
			return liquibase;
		}

		private DataSource getMigrationDataSource(DataSource liquibaseDataSource, DataSource dataSource,
				LiquibaseProperties properties) {
			if (liquibaseDataSource != null) {
				return liquibaseDataSource;
			}
			if (properties.getUrl() != null) {
				DataSourceBuilder<?> builder = DataSourceBuilder.create().type(SimpleDriverDataSource.class);
				builder.url(properties.getUrl());
				applyCommonBuilderProperties(properties, builder);
				return builder.build();
			}
			if (properties.getUser() != null && dataSource != null) {
				DataSourceBuilder<?> builder = DataSourceBuilder.derivedFrom(dataSource)
						.type(SimpleDriverDataSource.class);
				applyCommonBuilderProperties(properties, builder);
				return builder.build();
			}
			Assert.state(dataSource != null, "Liquibase migration DataSource missing");
			return dataSource;
		}

		private void applyCommonBuilderProperties(LiquibaseProperties properties, DataSourceBuilder<?> builder) {
			builder.username(properties.getUser());
			builder.password(properties.getPassword());
			if (StringUtils.hasText(properties.getDriverClassName())) {
				builder.driverClassName(properties.getDriverClassName());
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

		@ConditionalOnProperty(prefix = "spring.liquibase", name = "url")
		private static final class LiquibaseUrlCondition {

		}

	}

}
