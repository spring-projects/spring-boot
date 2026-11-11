/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure.health;

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.health.autoconfigure.contributor.CompositeReactiveHealthContributorConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.ReactiveHealthContributor;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.boot.r2dbc.health.ConnectionFactoryHealthIndicator;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link ConnectionFactoryHealthIndicator}.
 *
 * @author Mark Paluch
 * @since 4.0.0
 */
@AutoConfiguration(after = R2dbcAutoConfiguration.class)
@ConditionalOnClass({ ConnectionFactory.class, ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(ConnectionFactory.class)
@ConditionalOnEnabledHealthIndicator("r2dbc")
public final class ConnectionFactoryHealthContributorAutoConfiguration
		extends CompositeReactiveHealthContributorConfiguration<ConnectionFactoryHealthIndicator, ConnectionFactory> {

	ConnectionFactoryHealthContributorAutoConfiguration() {
		super(ConnectionFactoryHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "r2dbcHealthIndicator", "r2dbcHealthContributor" })
	ReactiveHealthContributor r2dbcHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, ConnectionFactory.class);
	}

}
