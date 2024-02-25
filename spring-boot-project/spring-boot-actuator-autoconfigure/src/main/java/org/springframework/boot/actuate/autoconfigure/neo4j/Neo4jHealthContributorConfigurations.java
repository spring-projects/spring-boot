/*
 * Copyright 2012-2022 the original author or authors.
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

	/**
	 * Neo4jConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	static class Neo4jConfiguration extends CompositeHealthContributorConfiguration<Neo4jHealthIndicator, Driver> {

		/**
		 * Constructs a new Neo4jConfiguration object.
		 *
		 * This constructor initializes a new Neo4jConfiguration object and sets the
		 * health indicator to Neo4jHealthIndicator.
		 * @param neo4jHealthIndicator the health indicator for Neo4j
		 */
		Neo4jConfiguration() {
			super(Neo4jHealthIndicator::new);
		}

		/**
		 * Creates a Neo4j health contributor if no existing bean with the names
		 * "neo4jHealthIndicator" or "neo4jHealthContributor" is found.
		 * @param drivers a map of Neo4j drivers
		 * @return the created Neo4j health contributor
		 */
		@Bean
		@ConditionalOnMissingBean(name = { "neo4jHealthIndicator", "neo4jHealthContributor" })
		HealthContributor neo4jHealthContributor(Map<String, Driver> drivers) {
			return createContributor(drivers);
		}

	}

	/**
	 * Neo4jReactiveConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Flux.class)
	static class Neo4jReactiveConfiguration
			extends CompositeReactiveHealthContributorConfiguration<Neo4jReactiveHealthIndicator, Driver> {

		/**
		 * Constructs a new Neo4jReactiveConfiguration object.
		 *
		 * This constructor initializes the Neo4jReactiveConfiguration object by calling
		 * the superclass constructor with a Neo4jReactiveHealthIndicator object as a
		 * parameter.
		 *
		 * @see Neo4jReactiveHealthIndicator
		 */
		Neo4jReactiveConfiguration() {
			super(Neo4jReactiveHealthIndicator::new);
		}

		/**
		 * Creates a ReactiveHealthContributor for Neo4j if no existing bean with the
		 * names "neo4jHealthIndicator" or "neo4jHealthContributor" is found.
		 * @param drivers a map of Neo4j drivers
		 * @return the created ReactiveHealthContributor for Neo4j
		 */
		@Bean
		@ConditionalOnMissingBean(name = { "neo4jHealthIndicator", "neo4jHealthContributor" })
		ReactiveHealthContributor neo4jHealthContributor(Map<String, Driver> drivers) {
			return createContributor(drivers);
		}

	}

}
