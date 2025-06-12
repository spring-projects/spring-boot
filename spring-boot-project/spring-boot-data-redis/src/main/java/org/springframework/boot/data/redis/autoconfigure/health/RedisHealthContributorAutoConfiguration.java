/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.redis.autoconfigure.health;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration;
import org.springframework.boot.data.redis.health.RedisHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.CompositeHealthContributorConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RedisHealthIndicator}.
 *
 * @author Christian Dupuis
 * @author Richard Santana
 * @author Stephane Nicoll
 * @author Mark Paluch
 * @since 4.0.0
 */
@AutoConfiguration(after = { RedisAutoConfiguration.class, RedisReactiveHealthContributorAutoConfiguration.class })
@ConditionalOnClass({ RedisConnectionFactory.class, HealthIndicator.class, ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(RedisConnectionFactory.class)
@ConditionalOnEnabledHealthIndicator("redis")
public class RedisHealthContributorAutoConfiguration
		extends CompositeHealthContributorConfiguration<RedisHealthIndicator, RedisConnectionFactory> {

	RedisHealthContributorAutoConfiguration() {
		super(RedisHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "redisHealthIndicator", "redisHealthContributor" })
	public HealthContributor redisHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, RedisConnectionFactory.class);
	}

}
