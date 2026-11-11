/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure.health;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.health.autoconfigure.contributor.CompositeHealthContributorConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.rabbitmq.health.RabbitHealthIndicator;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RabbitHealthIndicator}.
 *
 * @author Christian Dupuis
 * @since 4.2.0
 */
@AutoConfiguration(after = RabbitAutoConfiguration.class)
@ConditionalOnClass({ RabbitHealthIndicator.class, RabbitTemplate.class, ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(RabbitTemplate.class)
@ConditionalOnEnabledHealthIndicator("rabbit")
public final class RabbitHealthContributorAutoConfiguration
		extends CompositeHealthContributorConfiguration<RabbitHealthIndicator, RabbitTemplate> {

	RabbitHealthContributorAutoConfiguration() {
		super(RabbitHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "rabbitHealthIndicator", "rabbitHealthContributor" })
	HealthContributor rabbitHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, RabbitTemplate.class);
	}

}
