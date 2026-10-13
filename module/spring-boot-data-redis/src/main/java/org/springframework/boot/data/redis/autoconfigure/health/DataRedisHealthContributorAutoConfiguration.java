/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.autoconfigure.health;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.health.DataRedisHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.CompositeHealthContributorConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link DataRedisHealthIndicator}.
 *
 * @author Christian Dupuis
 * @author Richard Santana
 * @author Stephane Nicoll
 * @author Mark Paluch
 * @since 4.0.0
 */
@AutoConfiguration(
		after = { DataRedisAutoConfiguration.class, DataRedisReactiveHealthContributorAutoConfiguration.class })
@ConditionalOnClass({ RedisConnectionFactory.class, HealthIndicator.class, ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(RedisConnectionFactory.class)
@ConditionalOnEnabledHealthIndicator("redis")
public final class DataRedisHealthContributorAutoConfiguration
		extends CompositeHealthContributorConfiguration<DataRedisHealthIndicator, RedisConnectionFactory> {

	DataRedisHealthContributorAutoConfiguration() {
		super(DataRedisHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "redisHealthIndicator", "redisHealthContributor" })
	HealthContributor redisHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, RedisConnectionFactory.class);
	}

}
