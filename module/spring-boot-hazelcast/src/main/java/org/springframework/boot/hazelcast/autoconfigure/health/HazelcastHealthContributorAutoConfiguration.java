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

package org.springframework.boot.hazelcast.autoconfigure.health;

import com.hazelcast.core.HazelcastInstance;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.hazelcast.autoconfigure.HazelcastAutoConfiguration;
import org.springframework.boot.hazelcast.health.HazelcastHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.CompositeHealthContributorConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link HazelcastHealthIndicator}.
 *
 * @author Dmytro Nosan
 * @since 4.0.0
 */
@AutoConfiguration(after = HazelcastAutoConfiguration.class)
@ConditionalOnClass({ HazelcastInstance.class, ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(HazelcastInstance.class)
@ConditionalOnEnabledHealthIndicator("hazelcast")
public class HazelcastHealthContributorAutoConfiguration
		extends CompositeHealthContributorConfiguration<HazelcastHealthIndicator, HazelcastInstance> {

	public HazelcastHealthContributorAutoConfiguration() {
		super(HazelcastHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "hazelcastHealthIndicator", "hazelcastHealthContributor" })
	public HealthContributor hazelcastHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, HazelcastInstance.class);
	}

}
