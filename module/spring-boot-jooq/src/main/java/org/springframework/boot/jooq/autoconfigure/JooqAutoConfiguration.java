/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.autoconfigure;

import java.io.IOException;

import javax.sql.DataSource;

import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.ExecuteListenerProvider;
import org.jooq.TransactionProvider;
import org.jooq.conf.Settings;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for jOOQ.
 *
 * @author Andreas Ahlenstorf
 * @author Michael Simons
 * @author Dmytro Nosan
 * @author Moritz Halbritter
 * @author George Papadopoulos
 * @since 4.0.0
 */
@AutoConfiguration(after = { DataSourceAutoConfiguration.class, TransactionAutoConfiguration.class })
@ConditionalOnClass(DSLContext.class)
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties(JooqProperties.class)
public final class JooqAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ConnectionProvider.class)
	DataSourceConnectionProvider dataSourceConnectionProvider(DataSource dataSource) {
		return new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(dataSource));
	}

	@Bean
	@ConditionalOnBean(PlatformTransactionManager.class)
	@ConditionalOnMissingBean(TransactionProvider.class)
	SpringTransactionProvider transactionProvider(PlatformTransactionManager txManager) {
		return new SpringTransactionProvider(txManager);
	}

	@Bean
	@Order(0)
	DefaultExecuteListenerProvider jooqExceptionTranslatorExecuteListenerProvider(
			ExceptionTranslatorExecuteListener exceptionTranslatorExecuteListener) {
		return new DefaultExecuteListenerProvider(exceptionTranslatorExecuteListener);
	}

	@Bean
	@ConditionalOnMissingBean
	ExceptionTranslatorExecuteListener jooqExceptionTranslator() {
		return ExceptionTranslatorExecuteListener.DEFAULT;
	}

	@Bean
	@ConditionalOnMissingBean(DSLContext.class)
	DefaultDSLContext dslContext(org.jooq.Configuration configuration) {
		return new DefaultDSLContext(configuration);
	}

	@Bean
	@ConditionalOnMissingBean(org.jooq.Configuration.class)
	DefaultConfiguration jooqConfiguration(JooqProperties properties, ConnectionProvider connectionProvider,
			DataSource dataSource, ObjectProvider<TransactionProvider> transactionProvider,
			ObjectProvider<ExecuteListenerProvider> executeListenerProviders,
			ObjectProvider<DefaultConfigurationCustomizer> configurationCustomizers,
			ObjectProvider<Settings> settingsProvider) {
		DefaultConfiguration configuration = new DefaultConfiguration();
		configuration.set(properties.determineSqlDialect(dataSource));
		configuration.set(connectionProvider);
		transactionProvider.ifAvailable(configuration::set);
		settingsProvider.ifAvailable(configuration::set);
		configuration.set(executeListenerProviders.orderedStream().toArray(ExecuteListenerProvider[]::new));
		configurationCustomizers.orderedStream().forEach((customizer) -> customizer.customize(configuration));
		return configuration;
	}

	@Bean
	@ConditionalOnProperty("spring.jooq.config")
	@ConditionalOnMissingBean
	Settings settings(JooqProperties properties) throws IOException {
		return JooqUtils.loadSettings(properties);
	}

}
