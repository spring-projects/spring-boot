/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jms.autoconfigure.health;

import jakarta.jms.ConnectionFactory;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.autoconfigure.contributor.CompositeHealthContributorConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.jms.autoconfigure.JmsAutoConfiguration;
import org.springframework.boot.jms.health.JmsHealthIndicator;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link JmsHealthIndicator}.
 *
 * @author Stephane Nicoll
 * @author Venkata Naga Sai Srikanth Gollapudi
 * @since 4.0.0
 */
@AutoConfiguration(after = JmsAutoConfiguration.class)
@ConditionalOnClass({ ConnectionFactory.class, JmsHealthIndicator.class })
@ConditionalOnBean(ConnectionFactory.class)
@ConditionalOnEnabledHealthIndicator("jms")
@EnableConfigurationProperties(JmsHealthIndicatorProperties.class)
public final class JmsHealthContributorAutoConfiguration
		extends CompositeHealthContributorConfiguration<JmsHealthIndicator, ConnectionFactory> {

	JmsHealthContributorAutoConfiguration(JmsHealthIndicatorProperties properties) {
		super((connectionFactory) -> new JmsHealthIndicator(connectionFactory, properties.getStartTimeout()));
	}

	@Bean
	@ConditionalOnMissingBean(name = { "jmsHealthIndicator", "jmsHealthContributor" })
	HealthContributor jmsHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, ConnectionFactory.class);
	}

}
