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

import java.util.function.Supplier;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import liquibase.change.DatabaseChange;
import liquibase.integration.spring.SpringLiquibase;
import org.jooq.DSLContext;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.JdbcOperationsDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.NamedParameterJdbcOperationsDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.jooq.DslContextDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration.LiquibaseDataSourceCondition;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration.LiquibaseDslContextDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration.LiquibaseEntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration.LiquibaseJdbcOperationsDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration.LiquibaseNamedParameterJdbcOperationsDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

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
 * @since 1.1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ SpringLiquibase.class, DatabaseChange.class })
@ConditionalOnProperty(prefix = "spring.liquibase", name = "enabled", matchIfMissing = true)
@Conditional(LiquibaseDataSourceCondition.class)
@AutoConfigureAfter({ DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
@Import({ LiquibaseEntityManagerFactoryDependsOnPostProcessor.class,
		LiquibaseJdbcOperationsDependsOnPostProcessor.class,
		LiquibaseNamedParameterJdbcOperationsDependsOnPostProcessor.class,
		LiquibaseDslContextDependsOnPostProcessor.class })
public class LiquibaseAutoConfiguration {

	@Bean
	public LiquibaseSchemaManagementProvider liquibaseDefaultDdlModeProvider(
			ObjectProvider<SpringLiquibase> liquibases) {
		return new LiquibaseSchemaManagementProvider(liquibases);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(SpringLiquibase.class)
	@EnableConfigurationProperties({ DataSourceProperties.class, LiquibaseProperties.class })
	public static class LiquibaseConfiguration {

		private final LiquibaseProperties properties;

		public LiquibaseConfiguration(LiquibaseProperties properties) {
			this.properties = properties;
		}

		@Bean
		public SpringLiquibase liquibase(DataSourceProperties dataSourceProperties,
				ObjectProvider<DataSource> dataSource,
				@LiquibaseDataSource ObjectProvider<DataSource> liquibaseDataSource) {
			SpringLiquibase liquibase = createSpringLiquibase(liquibaseDataSource.getIfAvailable(),
					dataSource.getIfUnique(), dataSourceProperties);
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

		private SpringLiquibase createSpringLiquibase(DataSource liquibaseDatasource, DataSource dataSource,
				DataSourceProperties dataSourceProperties) {
			DataSource liquibaseDataSource = getDataSource(liquibaseDatasource, dataSource);
			if (liquibaseDataSource != null) {
				SpringLiquibase liquibase = new SpringLiquibase();
				liquibase.setDataSource(liquibaseDataSource);
				return liquibase;
			}
			SpringLiquibase liquibase = new DataSourceClosingSpringLiquibase();
			liquibase.setDataSource(createNewDataSource(dataSourceProperties));
			return liquibase;
		}

		private DataSource getDataSource(DataSource liquibaseDataSource, DataSource dataSource) {
			if (liquibaseDataSource != null) {
				return liquibaseDataSource;
			}
			if (this.properties.getUrl() == null && this.properties.getUser() == null) {
				return dataSource;
			}
			return null;
		}

		private DataSource createNewDataSource(DataSourceProperties dataSourceProperties) {
			String url = getProperty(this.properties::getUrl, dataSourceProperties::determineUrl);
			String user = getProperty(this.properties::getUser, dataSourceProperties::determineUsername);
			String password = getProperty(this.properties::getPassword, dataSourceProperties::determinePassword);
			return DataSourceBuilder.create().type(determineDataSourceType()).url(url).username(user).password(password)
					.build();
		}

		private Class<? extends DataSource> determineDataSourceType() {
			Class<? extends DataSource> type = DataSourceBuilder.findType(null);
			return (type != null) ? type : SimpleDriverDataSource.class;
		}

		private String getProperty(Supplier<String> property, Supplier<String> defaultValue) {
			String value = property.get();
			return (value != null) ? value : defaultValue.get();
		}

	}

	/**
	 * Post processor to ensure that {@link EntityManagerFactory} beans depend on any
	 * {@link SpringLiquibase} beans.
	 */
	@ConditionalOnClass(LocalContainerEntityManagerFactoryBean.class)
	@ConditionalOnBean(AbstractEntityManagerFactoryBean.class)
	static class LiquibaseEntityManagerFactoryDependsOnPostProcessor
			extends EntityManagerFactoryDependsOnPostProcessor {

		LiquibaseEntityManagerFactoryDependsOnPostProcessor() {
			super(SpringLiquibase.class);
		}

	}

	/**
	 * Additional configuration to ensure that {@link JdbcOperations} beans depend on any
	 * {@link SpringLiquibase} beans.
	 */
	@ConditionalOnClass(JdbcOperations.class)
	@ConditionalOnBean(JdbcOperations.class)
	static class LiquibaseJdbcOperationsDependsOnPostProcessor extends JdbcOperationsDependsOnPostProcessor {

		LiquibaseJdbcOperationsDependsOnPostProcessor() {
			super(SpringLiquibase.class);
		}

	}

	/**
	 * Post processor to ensure that {@link NamedParameterJdbcOperations} beans depend on
	 * any {@link SpringLiquibase} beans.
	 */
	@ConditionalOnClass(NamedParameterJdbcOperations.class)
	@ConditionalOnBean(NamedParameterJdbcOperations.class)
	static class LiquibaseNamedParameterJdbcOperationsDependsOnPostProcessor
			extends NamedParameterJdbcOperationsDependsOnPostProcessor {

		LiquibaseNamedParameterJdbcOperationsDependsOnPostProcessor() {
			super(SpringLiquibase.class);
		}

	}

	/**
	 * Post processor to ensure that {@link DSLContext} beans depend on any
	 * {@link SpringLiquibase} beans.
	 */
	@ConditionalOnClass(DSLContext.class)
	@ConditionalOnBean(DSLContext.class)
	static class LiquibaseDslContextDependsOnPostProcessor extends DslContextDependsOnPostProcessor {

		LiquibaseDslContextDependsOnPostProcessor() {
			super(SpringLiquibase.class);
		}

	}

	static final class LiquibaseDataSourceCondition extends AnyNestedCondition {

		LiquibaseDataSourceCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnBean(DataSource.class)
		private static final class DataSourceBeanCondition {

		}

		@ConditionalOnProperty(prefix = "spring.liquibase", name = "url", matchIfMissing = false)
		private static final class LiquibaseUrlCondition {

		}

	}

}
