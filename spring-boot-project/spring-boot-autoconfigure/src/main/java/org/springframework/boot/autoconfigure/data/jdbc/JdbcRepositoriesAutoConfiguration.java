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

package org.springframework.boot.autoconfigure.data.jdbc;

import java.util.Optional;
import java.util.Set;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.convert.RelationResolver;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.jdbc.repository.config.JdbcRepositoryConfigExtension;
import org.springframework.data.relational.RelationalManagedTypes;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's JDBC Repositories.
 * <p>
 * Once in effect, the auto-configuration is the equivalent of enabling JDBC repositories
 * using the {@link EnableJdbcRepositories @EnableJdbcRepositories} annotation and
 * providing an {@link AbstractJdbcConfiguration} subclass.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Mark Paluch
 * @since 2.1.0
 * @see EnableJdbcRepositories
 */
@AutoConfiguration(after = { JdbcTemplateAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class })
@ConditionalOnBean({ NamedParameterJdbcOperations.class, PlatformTransactionManager.class })
@ConditionalOnClass({ NamedParameterJdbcOperations.class, AbstractJdbcConfiguration.class })
@ConditionalOnProperty(prefix = "spring.data.jdbc.repositories", name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class JdbcRepositoriesAutoConfiguration {

	/**
	 * JdbcRepositoriesConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(JdbcRepositoryConfigExtension.class)
	@Import(JdbcRepositoriesRegistrar.class)
	static class JdbcRepositoriesConfiguration {

	}

	/**
	 * SpringBootJdbcConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(AbstractJdbcConfiguration.class)
	static class SpringBootJdbcConfiguration extends AbstractJdbcConfiguration {

		private final ApplicationContext applicationContext;

		/**
		 * Constructs a new instance of SpringBootJdbcConfiguration with the specified
		 * ApplicationContext.
		 * @param applicationContext the ApplicationContext to be used by the
		 * configuration
		 */
		SpringBootJdbcConfiguration(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		/**
		 * Retrieves the initial set of entity classes to be scanned for database table
		 * mappings.
		 * @return A set of Class objects representing the entity classes.
		 * @throws ClassNotFoundException if any of the entity classes cannot be found.
		 */
		@Override
		protected Set<Class<?>> getInitialEntitySet() throws ClassNotFoundException {
			return new EntityScanner(this.applicationContext).scan(Table.class);
		}

		/**
		 * Retrieves the managed types for JDBC connections.
		 * @return The managed types for JDBC connections.
		 * @throws ClassNotFoundException If the class for the managed types cannot be
		 * found.
		 */
		@Override
		@Bean
		@ConditionalOnMissingBean
		public RelationalManagedTypes jdbcManagedTypes() throws ClassNotFoundException {
			return super.jdbcManagedTypes();
		}

		/**
		 * Overrides the jdbcMappingContext method in the parent class.
		 * @param namingStrategy The optional naming strategy to be used.
		 * @param customConversions The custom conversions to be used.
		 * @param jdbcManagedTypes The managed types for JDBC.
		 * @return The JDBC mapping context.
		 */
		@Override
		@Bean
		@ConditionalOnMissingBean
		public JdbcMappingContext jdbcMappingContext(Optional<NamingStrategy> namingStrategy,
				JdbcCustomConversions customConversions, RelationalManagedTypes jdbcManagedTypes) {
			return super.jdbcMappingContext(namingStrategy, customConversions, jdbcManagedTypes);
		}

		/**
		 * Returns the JdbcConverter bean for the application.
		 *
		 * This method is annotated with @Override to indicate that it overrides the
		 * implementation in the superclass.
		 *
		 * This method is annotated with @Bean to indicate that it is a bean definition
		 * method and should be processed by the Spring container.
		 *
		 * This method is annotated with @ConditionalOnMissingBean to indicate that it
		 * should only be executed if there is no existing bean of type JdbcConverter in
		 * the application context.
		 *
		 * This method takes the following parameters: - mappingContext: The
		 * JdbcMappingContext bean used for mapping between Java objects and database
		 * tables. - operations: The NamedParameterJdbcOperations bean used for executing
		 * SQL queries. - relationResolver: The RelationResolver bean used for resolving
		 * relationships between entities. - conversions: The JdbcCustomConversions bean
		 * used for converting between Java types and database types. - dialect: The
		 * Dialect bean used for determining the SQL dialect of the underlying database.
		 *
		 * This method returns the JdbcConverter bean for the application, which is
		 * created by invoking the jdbcConverter method in the superclass.
		 */
		@Override
		@Bean
		@ConditionalOnMissingBean
		public JdbcConverter jdbcConverter(JdbcMappingContext mappingContext, NamedParameterJdbcOperations operations,
				@Lazy RelationResolver relationResolver, JdbcCustomConversions conversions, Dialect dialect) {
			return super.jdbcConverter(mappingContext, operations, relationResolver, conversions, dialect);
		}

		/**
		 * Returns the JdbcCustomConversions bean if it is not already defined.
		 * @return the JdbcCustomConversions bean
		 */
		@Override
		@Bean
		@ConditionalOnMissingBean
		public JdbcCustomConversions jdbcCustomConversions() {
			return super.jdbcCustomConversions();
		}

		/**
		 * Creates a new instance of {@link JdbcAggregateTemplate} if no other bean of the
		 * same type is present in the application context.
		 * @param applicationContext the application context
		 * @param mappingContext the JDBC mapping context
		 * @param converter the JDBC converter
		 * @param dataAccessStrategy the data access strategy
		 * @return the {@link JdbcAggregateTemplate} instance
		 */
		@Override
		@Bean
		@ConditionalOnMissingBean
		public JdbcAggregateTemplate jdbcAggregateTemplate(ApplicationContext applicationContext,
				JdbcMappingContext mappingContext, JdbcConverter converter, DataAccessStrategy dataAccessStrategy) {
			return super.jdbcAggregateTemplate(applicationContext, mappingContext, converter, dataAccessStrategy);
		}

		/**
		 * Returns the data access strategy bean.
		 *
		 * This method is annotated with @Override to indicate that it overrides a method
		 * from the superclass.
		 *
		 * This method is annotated with @Bean to indicate that it is a bean definition
		 * method.
		 *
		 * This method is annotated with @ConditionalOnMissingBean to indicate that it
		 * should only be executed if there is no existing bean of the same type.
		 * @param operations - The NamedParameterJdbcOperations used for executing SQL
		 * queries.
		 * @param jdbcConverter - The JdbcConverter used for converting between Java
		 * objects and database rows.
		 * @param context - The JdbcMappingContext used for mapping Java objects to
		 * database tables.
		 * @param dialect - The Dialect used for generating SQL statements specific to the
		 * database.
		 * @return The data access strategy bean.
		 */
		@Override
		@Bean
		@ConditionalOnMissingBean
		public DataAccessStrategy dataAccessStrategyBean(NamedParameterJdbcOperations operations,
				JdbcConverter jdbcConverter, JdbcMappingContext context, Dialect dialect) {
			return super.dataAccessStrategyBean(operations, jdbcConverter, context, dialect);
		}

		/**
		 * Returns the JDBC dialect for the given NamedParameterJdbcOperations. If no
		 * dialect is specified, the default dialect will be used.
		 * @param operations the NamedParameterJdbcOperations to use
		 * @return the JDBC dialect for the given operations
		 */
		@Override
		@Bean
		@ConditionalOnMissingBean
		public Dialect jdbcDialect(NamedParameterJdbcOperations operations) {
			return super.jdbcDialect(operations);
		}

	}

}
