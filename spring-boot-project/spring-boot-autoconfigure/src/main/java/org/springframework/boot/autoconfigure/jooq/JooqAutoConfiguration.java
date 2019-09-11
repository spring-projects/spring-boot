/*
 * Copyright 2012-2019 the original author or authors.
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
import org.jooq.ExecutorProvider;
import org.jooq.RecordListenerProvider;
import org.jooq.RecordMapperProvider;
import org.jooq.RecordUnmapperProvider;
import org.jooq.TransactionListenerProvider;
import org.jooq.TransactionProvider;
import org.jooq.VisitListenerProvider;
import org.jooq.conf.Settings;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.impl.DefaultExecuteListenerProvider;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
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
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(DSLContext.class)
@ConditionalOnBean(DataSource.class)
@AutoConfigureAfter({ DataSourceAutoConfiguration.class, TransactionAutoConfiguration.class })
public class JooqAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ConnectionProvider.class)
	public DataSourceConnectionProvider dataSourceConnectionProvider(DataSource dataSource) {
		return new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(dataSource));
	}

	@Bean
	@ConditionalOnBean(PlatformTransactionManager.class)
	public SpringTransactionProvider transactionProvider(PlatformTransactionManager txManager) {
		return new SpringTransactionProvider(txManager);
	}

	@Bean
	@Order(0)
	public DefaultExecuteListenerProvider jooqExceptionTranslatorExecuteListenerProvider() {
		return new DefaultExecuteListenerProvider(new JooqExceptionTranslator());
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(DSLContext.class)
	@EnableConfigurationProperties(JooqProperties.class)
	public static class DslContextConfiguration {

		@Bean
		public DefaultDSLContext dslContext(org.jooq.Configuration configuration) {
			return new DefaultDSLContext(configuration);
		}

		@Bean
		@ConditionalOnMissingBean(org.jooq.Configuration.class)
		public DefaultConfiguration jooqConfiguration(JooqProperties properties, ConnectionProvider connectionProvider,
				DataSource dataSource, ObjectProvider<TransactionProvider> transactionProvider,
				ObjectProvider<RecordMapperProvider> recordMapperProvider,
				ObjectProvider<RecordUnmapperProvider> recordUnmapperProvider, ObjectProvider<Settings> settings,
				ObjectProvider<RecordListenerProvider> recordListenerProviders,
				ObjectProvider<ExecuteListenerProvider> executeListenerProviders,
				ObjectProvider<VisitListenerProvider> visitListenerProviders,
				ObjectProvider<TransactionListenerProvider> transactionListenerProviders,
				ObjectProvider<ExecutorProvider> executorProvider) {
			DefaultConfiguration configuration = new DefaultConfiguration();
			configuration.set(properties.determineSqlDialect(dataSource));
			configuration.set(connectionProvider);
			transactionProvider.ifAvailable(configuration::set);
			recordMapperProvider.ifAvailable(configuration::set);
			recordUnmapperProvider.ifAvailable(configuration::set);
			settings.ifAvailable(configuration::set);
			executorProvider.ifAvailable(configuration::set);
			configuration.set(recordListenerProviders.orderedStream().toArray(RecordListenerProvider[]::new));
			configuration.set(executeListenerProviders.orderedStream().toArray(ExecuteListenerProvider[]::new));
			configuration.set(visitListenerProviders.orderedStream().toArray(VisitListenerProvider[]::new));
			configuration.setTransactionListenerProvider(
					transactionListenerProviders.orderedStream().toArray(TransactionListenerProvider[]::new));
			return configuration;
		}

	}

}
