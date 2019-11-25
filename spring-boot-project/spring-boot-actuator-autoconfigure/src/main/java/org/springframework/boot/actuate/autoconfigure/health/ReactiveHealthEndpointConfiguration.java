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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.LinkedHashMap;
import java.util.Map;

import reactor.core.publisher.Flux;

import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthContributorRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for reactive {@link HealthEndpoint} infrastructure beans.
 *
 * @author Phillip Webb
 * @see HealthEndpointAutoConfiguration
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Flux.class)
@ConditionalOnBean(HealthEndpoint.class)
class ReactiveHealthEndpointConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ReactiveHealthContributorRegistry reactiveHealthContributorRegistry(
			Map<String, HealthContributor> healthContributors,
			Map<String, ReactiveHealthContributor> reactiveHealthContributors, HealthEndpointGroups groups) {
		Map<String, ReactiveHealthContributor> allContributors = new LinkedHashMap<>(reactiveHealthContributors);
		healthContributors.forEach((name, contributor) -> allContributors.computeIfAbsent(name,
				(key) -> ReactiveHealthContributor.adapt(contributor)));
		return new AutoConfiguredReactiveHealthContributorRegistry(allContributors, groups.getNames());
	}

}
