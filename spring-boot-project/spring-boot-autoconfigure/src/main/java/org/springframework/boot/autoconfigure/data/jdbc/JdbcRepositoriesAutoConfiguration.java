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

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(JdbcRepositoryConfigExtension.class)
	@Import(JdbcRepositoriesRegistrar.class)
	static class JdbcRepositoriesConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(AbstractJdbcConfiguration.class)
	static class SpringBootJdbcConfiguration extends AbstractJdbcConfiguration {

		private final ApplicationContext applicationContext;

		SpringBootJdbcConfiguration(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Override
		protected Set<Class<?>> getInitialEntitySet() throws ClassNotFoundException {
			return new EntityScanner(this.applicationContext).scan(Table.class);
		}

		@Override
		@Bean
		@ConditionalOnMissingBean
		public RelationalManagedTypes jdbcManagedTypes() throws ClassNotFoundException {
			return super.jdbcManagedTypes();
		}

		@Override
		@Bean
		@ConditionalOnMissingBean
		public JdbcMappingContext jdbcMappingContext(Optional<NamingStrategy> namingStrategy,
				JdbcCustomConversions customConversions, RelationalManagedTypes jdbcManagedTypes) {
			return super.jdbcMappingContext(namingStrategy, customConversions, jdbcManagedTypes);
		}

		@Override
		@Bean
		@ConditionalOnMissingBean
		public JdbcConverter jdbcConverter(JdbcMappingContext mappingContext, NamedParameterJdbcOperations operations,
				@Lazy RelationResolver relationResolver, JdbcCustomConversions conversions, Dialect dialect) {
			return super.jdbcConverter(mappingContext, operations, relationResolver, conversions, dialect);
		}

		@Override
		@Bean
		@ConditionalOnMissingBean
		public JdbcCustomConversions jdbcCustomConversions() {
			return super.jdbcCustomConversions();
		}

		@Override
		@Bean
		@ConditionalOnMissingBean
		public JdbcAggregateTemplate jdbcAggregateTemplate(ApplicationContext applicationContext,
				JdbcMappingContext mappingContext, JdbcConverter converter, DataAccessStrategy dataAccessStrategy) {
			return super.jdbcAggregateTemplate(applicationContext, mappingContext, converter, dataAccessStrategy);
		}

		@Override
		@Bean
		@ConditionalOnMissingBean
		public DataAccessStrategy dataAccessStrategyBean(NamedParameterJdbcOperations operations,
				JdbcConverter jdbcConverter, JdbcMappingContext context, Dialect dialect) {
			return super.dataAccessStrategyBean(operations, jdbcConverter, context, dialect);
		}

		@Override
		@Bean
		@ConditionalOnMissingBean
		public Dialect jdbcDialect(NamedParameterJdbcOperations operations) {
			return super.jdbcDialect(operations);
		}

	}

}
