/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.autoconfigure;

import java.io.IOException;

import javax.sql.DataSource;

import io.r2dbc.spi.ConnectionFactory;
import org.jooq.DSLContext;
import org.jooq.ExecuteListenerProvider;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.reactor.extensions.CoreSubscriberProvider;
import org.jooq.tools.jdbc.JDBCUtils;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.r2dbc.connection.ConnectionHolder;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.connection.TransactionAwareConnectionFactoryProxy;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for jOOQ with R2DBC.
 * <p>
 * The following transaction scenarios are not supported:
 * <ul>
 * <li>{@link TransactionDefinition#PROPAGATION_SUPPORTS SUPPORTS} without an existing
 * transaction</li>
 * <li>{@link TransactionDefinition#PROPAGATION_NOT_SUPPORTED NOT_SUPPORTED}</li>
 * <li>Transactions started with a manually bound, unsynchronized
 * {@link ConnectionHolder}</li>
 * </ul>
 *
 * @author George Papadopoulos
 * @since 4.2.0
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class, before = JooqAutoConfiguration.class,
		afterName = "org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration",
		beforeName = "org.springframework.boot.r2dbc.autoconfigure.R2dbcTransactionManagerAutoConfiguration")
@ConditionalOnClass({ DSLContext.class, ConnectionFactory.class, R2dbcTransactionManager.class,
		CoreSubscriberProvider.class })
@ConditionalOnSingleCandidate(ConnectionFactory.class)
@ConditionalOnMissingBean(DataSource.class)
@EnableConfigurationProperties(JooqProperties.class)
public final class R2dbcJooqAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(DSLContext.class)
	DefaultDSLContext dslContext(org.jooq.Configuration configuration) {
		return new DefaultDSLContext(configuration);
	}

	@Bean
	@ConditionalOnMissingBean(org.jooq.Configuration.class)
	DefaultConfiguration jooqConfiguration(JooqProperties properties, ConnectionFactory connectionFactory,
			ObjectProvider<ExecuteListenerProvider> executeListenerProviders,
			ObjectProvider<DefaultConfigurationCustomizer> configurationCustomizers,
			ObjectProvider<Settings> settingsProvider) {
		DefaultConfiguration configuration = new DefaultConfiguration();
		configuration.set((properties.getSqlDialect() != null) ? properties.getSqlDialect()
				: JDBCUtils.dialect(connectionFactory));
		configuration.set(new TransactionAwareConnectionFactoryProxy(connectionFactory));
		// Allows jooq to propagate properly the reactor context.
		configuration.set(new CoreSubscriberProvider());
		settingsProvider.ifAvailable(configuration::set);
		configuration.set(executeListenerProviders.orderedStream().toArray(ExecuteListenerProvider[]::new));
		configurationCustomizers.orderedStream().forEach((customizer) -> customizer.customize(configuration));
		return configuration;
	}

	@Bean
	@ConditionalOnMissingBean(ReactiveTransactionManager.class)
	R2dbcTransactionManager connectionFactoryTransactionManager(ConnectionFactory connectionFactory) {
		return new JooqR2dbcTransactionManager(connectionFactory);
	}

	@Bean
	@ConditionalOnProperty("spring.jooq.config")
	@ConditionalOnMissingBean
	Settings settings(JooqProperties properties) throws IOException {
		return JooqUtils.loadSettings(properties);
	}

}
