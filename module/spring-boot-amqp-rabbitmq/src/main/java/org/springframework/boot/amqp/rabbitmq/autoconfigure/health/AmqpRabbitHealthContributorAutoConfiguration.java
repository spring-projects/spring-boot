/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.rabbitmq.autoconfigure.health;

import com.rabbitmq.client.amqp.Connection;

import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.amqp.rabbitmq.autoconfigure.AmqpRabbitAutoConfiguration;
import org.springframework.boot.amqp.rabbitmq.health.AmqpRabbitHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.health.autoconfigure.contributor.CompositeHealthContributorConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link AmqpRabbitHealthIndicator}.
 *
 * @author Stephane Nicoll
 * @since 4.2.0
 */
@AutoConfiguration(after = AmqpRabbitAutoConfiguration.class)
@ConditionalOnClass({ AmqpRabbitHealthIndicator.class, RabbitAmqpTemplate.class, Connection.class,
		ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(AmqpConnectionFactory.class)
@ConditionalOnEnabledHealthIndicator("rabbit")
public final class AmqpRabbitHealthContributorAutoConfiguration
		extends CompositeHealthContributorConfiguration<AmqpRabbitHealthIndicator, AmqpConnectionFactory> {

	AmqpRabbitHealthContributorAutoConfiguration() {
		super(AmqpRabbitHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "rabbitHealthIndicator", "rabbitHealthContributor" })
	HealthContributor rabbitHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, AmqpConnectionFactory.class);
	}

}
