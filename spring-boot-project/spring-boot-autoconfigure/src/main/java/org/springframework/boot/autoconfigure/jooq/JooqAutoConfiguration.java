/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
@Configuration
@ConditionalOnClass(DSLContext.class)
@ConditionalOnBean(DataSource.class)
@AutoConfigureAfter({ DataSourceAutoConfiguration.class,
		TransactionAutoConfiguration.class })
public class JooqAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public DataSourceConnectionProvider dataSourceConnectionProvider(
			DataSource dataSource) {
		return new DataSourceConnectionProvider(
				new TransactionAwareDataSourceProxy(dataSource));
	}

	@Bean
	@ConditionalOnBean(PlatformTransactionManager.class)
	public SpringTransactionProvider transactionProvider(
			PlatformTransactionManager txManager) {
		return new SpringTransactionProvider(txManager);
	}

	@Bean
	public DefaultExecuteListenerProvider jooqExceptionTranslatorExecuteListenerProvider() {
		return new DefaultExecuteListenerProvider(new JooqExceptionTranslator());
	}

	@Configuration
	@ConditionalOnMissingBean(DSLContext.class)
	@EnableConfigurationProperties(JooqProperties.class)
	public static class DslContextConfiguration {

		private final JooqProperties properties;

		private final ConnectionProvider connection;

		private final DataSource dataSource;

		private final TransactionProvider transactionProvider;

		private final RecordMapperProvider recordMapperProvider;

		private final RecordUnmapperProvider recordUnmapperProvider;

		private final Settings settings;

		private final RecordListenerProvider[] recordListenerProviders;

		private final ExecuteListenerProvider[] executeListenerProviders;

		private final VisitListenerProvider[] visitListenerProviders;

		private final TransactionListenerProvider[] transactionListenerProviders;

		public DslContextConfiguration(JooqProperties properties,
				ConnectionProvider connectionProvider, DataSource dataSource,
				ObjectProvider<TransactionProvider> transactionProvider,
				ObjectProvider<RecordMapperProvider> recordMapperProvider,
				ObjectProvider<RecordUnmapperProvider> recordUnmapperProvider,
				ObjectProvider<Settings> settings,
				ObjectProvider<RecordListenerProvider[]> recordListenerProviders,
				ExecuteListenerProvider[] executeListenerProviders,
				ObjectProvider<VisitListenerProvider[]> visitListenerProviders,
				ObjectProvider<TransactionListenerProvider[]> transactionListenerProviders) {
			this.properties = properties;
			this.connection = connectionProvider;
			this.dataSource = dataSource;
			this.transactionProvider = transactionProvider.getIfAvailable();
			this.recordMapperProvider = recordMapperProvider.getIfAvailable();
			this.recordUnmapperProvider = recordUnmapperProvider.getIfAvailable();
			this.settings = settings.getIfAvailable();
			this.recordListenerProviders = recordListenerProviders.getIfAvailable();
			this.executeListenerProviders = executeListenerProviders;
			this.visitListenerProviders = visitListenerProviders.getIfAvailable();
			this.transactionListenerProviders = transactionListenerProviders
					.getIfAvailable();
		}

		@Bean
		public DefaultDSLContext dslContext(org.jooq.Configuration configuration) {
			return new DefaultDSLContext(configuration);
		}

		@Bean
		@ConditionalOnMissingBean(org.jooq.Configuration.class)
		public DefaultConfiguration jooqConfiguration() {
			DefaultConfiguration configuration = new DefaultConfiguration();
			configuration.set(this.properties.determineSqlDialect(this.dataSource));
			configuration.set(this.connection);
			if (this.transactionProvider != null) {
				configuration.set(this.transactionProvider);
			}
			if (this.recordMapperProvider != null) {
				configuration.set(this.recordMapperProvider);
			}
			if (this.recordUnmapperProvider != null) {
				configuration.set(this.recordUnmapperProvider);
			}
			if (this.settings != null) {
				configuration.set(this.settings);
			}
			configuration.set(this.recordListenerProviders);
			configuration.set(this.executeListenerProviders);
			configuration.set(this.visitListenerProviders);
			configuration
					.setTransactionListenerProvider(this.transactionListenerProviders);
			return configuration;
		}

	}

}
