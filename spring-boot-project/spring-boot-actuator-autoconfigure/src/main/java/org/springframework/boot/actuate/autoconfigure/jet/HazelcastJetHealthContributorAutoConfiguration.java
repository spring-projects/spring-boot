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

package org.springframework.boot.actuate.autoconfigure.jet;

import java.util.Map;

import com.hazelcast.jet.JetInstance;

import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthContributorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.jet.HazelcastJetHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jet.HazelcastJetAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link HazelcastJetHealthIndicator}.
 *
 * @author Ali Gurbuz
 * @since 2.3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(JetInstance.class)
@ConditionalOnBean(JetInstance.class)
@ConditionalOnEnabledHealthIndicator("hazelcast.jet")
@AutoConfigureAfter(HazelcastJetAutoConfiguration.class)
public class HazelcastJetHealthContributorAutoConfiguration
		extends CompositeHealthContributorConfiguration<HazelcastJetHealthIndicator, JetInstance> {

	@Bean
	@ConditionalOnMissingBean(name = {"hazelcastJetHealthIndicator", "hazelcastJetHealthContributor"})
	public HealthContributor hazelcastHealthContributor(Map<String, JetInstance> jetInstances) {
		return createContributor(jetInstances);
	}

}
