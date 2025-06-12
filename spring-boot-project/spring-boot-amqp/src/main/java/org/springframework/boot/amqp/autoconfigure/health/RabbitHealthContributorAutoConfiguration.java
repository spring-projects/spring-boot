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

package org.springframework.boot.amqp.autoconfigure.health;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.amqp.health.RabbitHealthIndicator;
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
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RabbitHealthIndicator}.
 *
 * @author Christian Dupuis
 * @since 4.0.0
 */
@AutoConfiguration(after = RabbitAutoConfiguration.class)
@ConditionalOnClass({ RabbitHealthIndicator.class, RabbitTemplate.class, ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(RabbitTemplate.class)
@ConditionalOnEnabledHealthIndicator("rabbit")
public class RabbitHealthContributorAutoConfiguration
		extends CompositeHealthContributorConfiguration<RabbitHealthIndicator, RabbitTemplate> {

	public RabbitHealthContributorAutoConfiguration() {
		super(RabbitHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "rabbitHealthIndicator", "rabbitHealthContributor" })
	public HealthContributor rabbitHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, RabbitTemplate.class);
	}

}
