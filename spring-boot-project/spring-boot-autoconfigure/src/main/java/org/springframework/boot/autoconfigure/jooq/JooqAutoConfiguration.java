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

package org.springframework.boot.autoconfigure.jooq;

import javax.sql.DataSource;

import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.ExecuteListenerProvider;
import org.jooq.TransactionProvider;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.impl.DefaultExecuteListenerProvider;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for JOOQ.
 *
 * @author Andreas Ahlenstorf
 * @author Michael Simons
 * @author Dmytro Nosan
 * @since 1.3.0
 */
@AutoConfiguration(after = { DataSourceAutoConfiguration.class, TransactionAutoConfiguration.class })
@ConditionalOnClass(DSLContext.class)
@ConditionalOnBean(DataSource.class)
public class JooqAutoConfiguration {

	/**
     * Creates a {@link DataSourceConnectionProvider} bean if no other bean of type {@link ConnectionProvider} is present.
     * 
     * @param dataSource the {@link DataSource} to be used by the {@link DataSourceConnectionProvider}
     * @return the created {@link DataSourceConnectionProvider} bean
     */
    @Bean
	@ConditionalOnMissingBean(ConnectionProvider.class)
	public DataSourceConnectionProvider dataSourceConnectionProvider(DataSource dataSource) {
		return new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(dataSource));
	}

	/**
     * Creates a SpringTransactionProvider bean if a PlatformTransactionManager bean is present and a TransactionProvider bean is missing.
     * 
     * @param txManager the PlatformTransactionManager bean to be used by the SpringTransactionProvider
     * @return the created SpringTransactionProvider bean
     */
    @Bean
	@ConditionalOnBean(PlatformTransactionManager.class)
	@ConditionalOnMissingBean(TransactionProvider.class)
	public SpringTransactionProvider transactionProvider(PlatformTransactionManager txManager) {
		return new SpringTransactionProvider(txManager);
	}

	/**
     * Creates a {@link DefaultExecuteListenerProvider} bean with the given {@link ExceptionTranslatorExecuteListener}.
     * This bean is used to translate exceptions thrown by jOOQ into Spring DataAccessExceptions.
     *
     * @param exceptionTranslatorExecuteListener The {@link ExceptionTranslatorExecuteListener} to be used for exception translation.
     * @return The {@link DefaultExecuteListenerProvider} bean.
     */
    @Bean
	@Order(0)
	public DefaultExecuteListenerProvider jooqExceptionTranslatorExecuteListenerProvider(
			ExceptionTranslatorExecuteListener exceptionTranslatorExecuteListener) {
		return new DefaultExecuteListenerProvider(exceptionTranslatorExecuteListener);
	}

	/**
     * Creates a new instance of {@link ExceptionTranslatorExecuteListener} if no bean of type {@link ExceptionTranslatorExecuteListener} is already present in the application context.
     * 
     * This method is annotated with {@link Bean} and {@link ConditionalOnMissingBean} annotations to ensure that the bean is only created if no other bean of the same type is already present.
     * 
     * The created instance is the default implementation of {@link ExceptionTranslatorExecuteListener}.
     * 
     * @return the created instance of {@link ExceptionTranslatorExecuteListener}
     */
    @Bean
	@ConditionalOnMissingBean(ExceptionTranslatorExecuteListener.class)
	public ExceptionTranslatorExecuteListener jooqExceptionTranslator() {
		return ExceptionTranslatorExecuteListener.DEFAULT;
	}

	/**
     * DslContextConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(DSLContext.class)
	@EnableConfigurationProperties(JooqProperties.class)
	public static class DslContextConfiguration {

		/**
         * Creates a new instance of DefaultDSLContext using the provided configuration.
         *
         * @param configuration the configuration to be used for creating the DSLContext
         * @return a new instance of DefaultDSLContext
         */
        @Bean
		public DefaultDSLContext dslContext(org.jooq.Configuration configuration) {
			return new DefaultDSLContext(configuration);
		}

		/**
         * Creates a JOOQ configuration bean if no other bean of type {@link org.jooq.Configuration} is present.
         * 
         * @param properties The JOOQ properties.
         * @param connectionProvider The connection provider.
         * @param dataSource The data source.
         * @param transactionProvider The transaction provider.
         * @param executeListenerProviders The execute listener providers.
         * @param configurationCustomizers The configuration customizers.
         * @return The JOOQ configuration.
         */
        @Bean
		@ConditionalOnMissingBean(org.jooq.Configuration.class)
		public DefaultConfiguration jooqConfiguration(JooqProperties properties, ConnectionProvider connectionProvider,
				DataSource dataSource, ObjectProvider<TransactionProvider> transactionProvider,
				ObjectProvider<ExecuteListenerProvider> executeListenerProviders,
				ObjectProvider<DefaultConfigurationCustomizer> configurationCustomizers) {
			DefaultConfiguration configuration = new DefaultConfiguration();
			configuration.set(properties.determineSqlDialect(dataSource));
			configuration.set(connectionProvider);
			transactionProvider.ifAvailable(configuration::set);
			configuration.set(executeListenerProviders.orderedStream().toArray(ExecuteListenerProvider[]::new));
			configurationCustomizers.orderedStream().forEach((customizer) -> customizer.customize(configuration));
			return configuration;
		}

	}

}
