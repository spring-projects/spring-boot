/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.neo4j;

import java.util.Map;

import org.neo4j.driver.Driver;
import reactor.core.publisher.Flux;

import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthContributorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.CompositeReactiveHealthContributorConfiguration;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.actuate.neo4j.Neo4jHealthIndicator;
import org.springframework.boot.actuate.neo4j.Neo4jReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Health contributor options for Neo4j.
 *
 * @author Michael J. Simons
 * @author Stephane Nicoll
 */
class Neo4jHealthContributorConfigurations {

	@Configuration(proxyBeanMethods = false)
	static class Neo4jConfiguration extends CompositeHealthContributorConfiguration<Neo4jHealthIndicator, Driver> {

		@Bean
		@ConditionalOnMissingBean(name = { "neo4jHealthIndicator", "neo4jHealthContributor" })
		HealthContributor neo4jHealthContributor(Map<String, Driver> drivers) {
			return createContributor(drivers);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Flux.class)
	static class Neo4jReactiveConfiguration
			extends CompositeReactiveHealthContributorConfiguration<Neo4jReactiveHealthIndicator, Driver> {

		@Bean
		@ConditionalOnMissingBean(name = { "neo4jHealthIndicator", "neo4jHealthContributor" })
		ReactiveHealthContributor neo4jHealthContributor(Map<String, Driver> drivers) {
			return createContributor(drivers);
		}

	}

}
