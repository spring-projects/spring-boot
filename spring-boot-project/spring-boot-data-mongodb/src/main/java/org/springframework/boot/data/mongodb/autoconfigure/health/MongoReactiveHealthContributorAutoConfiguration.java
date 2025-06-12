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

package org.springframework.boot.data.mongodb.autoconfigure.health;

import reactor.core.publisher.Flux;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.mongodb.autoconfigure.MongoReactiveDataAutoConfiguration;
import org.springframework.boot.data.mongodb.health.MongoReactiveHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.CompositeReactiveHealthContributorConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.ReactiveHealthContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link MongoReactiveHealthIndicator}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = MongoReactiveDataAutoConfiguration.class)
@ConditionalOnClass({ ReactiveMongoTemplate.class, Flux.class, MongoReactiveHealthIndicator.class,
		ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(ReactiveMongoTemplate.class)
@ConditionalOnEnabledHealthIndicator("mongo")
public class MongoReactiveHealthContributorAutoConfiguration
		extends CompositeReactiveHealthContributorConfiguration<MongoReactiveHealthIndicator, ReactiveMongoTemplate> {

	public MongoReactiveHealthContributorAutoConfiguration() {
		super(MongoReactiveHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "mongoHealthIndicator", "mongoHealthContributor" })
	public ReactiveHealthContributor mongoHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, ReactiveMongoTemplate.class);
	}

}
