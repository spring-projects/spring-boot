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

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.DefaultHealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointSettings;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.SimpleHttpCodeStatusMapper;
import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for {@link HealthEndpoint} infrastructure beans.
 *
 * @author Phillip Webb
 * @see HealthEndpointAutoConfiguration
 */
@Configuration(proxyBeanMethods = false)
class HealthEndpointConfiguration {

	@Bean
	@ConditionalOnMissingBean
	StatusAggregator healthStatusAggregator(HealthEndpointProperties properties) {
		return new SimpleStatusAggregator(properties.getStatus().getOrder());
	}

	@Bean
	@ConditionalOnMissingBean
	HttpCodeStatusMapper healthHttpCodeStatusMapper(HealthEndpointProperties properties) {
		return new SimpleHttpCodeStatusMapper(properties.getStatus().getHttpMapping());
	}

	@Bean
	@ConditionalOnMissingBean
	HealthEndpointSettings healthEndpointSettings(HealthEndpointProperties properties,
			ObjectProvider<StatusAggregator> statusAggregatorProvider,
			ObjectProvider<HttpCodeStatusMapper> httpCodeStatusMapperProvider) {
		StatusAggregator statusAggregator = statusAggregatorProvider
				.getIfAvailable(() -> new SimpleStatusAggregator(properties.getStatus().getOrder()));
		HttpCodeStatusMapper httpCodeStatusMapper = httpCodeStatusMapperProvider
				.getIfAvailable(() -> new SimpleHttpCodeStatusMapper(properties.getStatus().getHttpMapping()));
		return new AutoConfiguredHealthEndpointSettings(statusAggregator, httpCodeStatusMapper,
				properties.getShowDetails(), properties.getRoles());
	}

	@Bean
	@ConditionalOnMissingBean
	HealthContributorRegistry healthContributorRegistry(Map<String, HealthContributor> healthContributors) {
		return new DefaultHealthContributorRegistry(healthContributors);
	}

	@Bean
	@ConditionalOnMissingBean
	HealthEndpoint healthEndpoint(HealthContributorRegistry registry, HealthEndpointSettings settings) {
		return new HealthEndpoint(registry, settings);
	}

}
