/*
 * Copyright 2012-2015 the original author or authors.
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
import org.jooq.SQLDialect;
import org.jooq.TransactionProvider;
import org.jooq.VisitListenerProvider;
import org.jooq.conf.Settings;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for JOOQ.
 *
 * @author Andreas Ahlenstorf
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass(DSLContext.class)
@ConditionalOnBean(DataSource.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class JooqAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(DataSourceConnectionProvider.class)
	public DataSourceConnectionProvider dataSourceConnectionProvider(DataSource dataSource) {
		return new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(
				dataSource));
	}

	@Bean
	@ConditionalOnBean(PlatformTransactionManager.class)
	public TransactionProvider transactionProvider(PlatformTransactionManager txManager) {
		return new SpringTransactionProvider(txManager);
	}

	@Bean
	public ExecuteListenerProvider jooqExceptionTranslatorExecuteListenerProvider() {
		return new DefaultExecuteListenerProvider(new JooqExceptionTranslator());
	}

	@Configuration
	@ConditionalOnMissingBean(DSLContext.class)
	@EnableConfigurationProperties(JooqProperties.class)
	public static class DslContextConfiguration {

		@Autowired
		private JooqProperties properties = new JooqProperties();

		@Autowired
		private ConnectionProvider connectionProvider;

		@Autowired(required = false)
		private TransactionProvider transactionProvider;

		@Autowired(required = false)
		private RecordMapperProvider recordMapperProvider;

		@Autowired(required = false)
		private Settings settings;

		@Autowired(required = false)
		private RecordListenerProvider[] recordListenerProviders;

		@Autowired
		private ExecuteListenerProvider[] executeListenerProviders;

		@Autowired(required = false)
		private VisitListenerProvider[] visitListenerProviders;

		@Bean
		public DefaultDSLContext dslContext(org.jooq.Configuration configuration) {
			return new DefaultDSLContext(configuration);
		}

		@Bean
		@ConditionalOnMissingBean(org.jooq.Configuration.class)
		public DefaultConfiguration jooqConfiguration() {
			DefaultConfiguration configuration = new DefaultConfiguration();
			if (!StringUtils.isEmpty(this.properties.getSqlDialect())) {
				configuration.set(SQLDialect.valueOf(this.properties.getSqlDialect()));
			}
			configuration.set(this.connectionProvider);
			if (this.transactionProvider != null) {
				configuration.set(this.transactionProvider);
			}
			if (this.recordMapperProvider != null) {
				configuration.set(this.recordMapperProvider);
			}
			if (this.settings != null) {
				configuration.set(this.settings);
			}
			configuration.set(this.recordListenerProviders);
			configuration.set(this.executeListenerProviders);
			configuration.set(this.visitListenerProviders);
			return configuration;
		}

	}

}
