/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.hazelcast;

import java.util.Map;

import com.hazelcast.core.HazelcastInstance;

import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthContributorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.hazelcast.HazelcastHealthIndicator;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link HazelcastHealthIndicator}.
 *
 * @author Dmytro Nosan
 * @since 2.2.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(HazelcastInstance.class)
@ConditionalOnBean(HazelcastInstance.class)
@ConditionalOnEnabledHealthIndicator("hazelcast")
@AutoConfigureAfter(HazelcastAutoConfiguration.class)
public class HazelcastHealthContributorAutoConfiguration
		extends CompositeHealthContributorConfiguration<HazelcastHealthIndicator, HazelcastInstance> {

	@Bean
	@ConditionalOnMissingBean(name = { "hazelcastHealthIndicator", "hazelcastHealthContributor" })
	public HealthContributor hazelcastHealthContributor(Map<String, HazelcastInstance> hazelcastInstances) {
		return createContributor(hazelcastInstances);
	}

}
