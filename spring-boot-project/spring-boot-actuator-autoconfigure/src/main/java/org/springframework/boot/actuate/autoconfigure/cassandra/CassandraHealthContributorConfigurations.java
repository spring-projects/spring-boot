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

package org.springframework.boot.actuate.autoconfigure.cassandra;

import java.util.Map;

import com.datastax.oss.driver.api.core.CqlSession;

import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthContributorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.CompositeReactiveHealthContributorConfiguration;
import org.springframework.boot.actuate.cassandra.CassandraDriverHealthIndicator;
import org.springframework.boot.actuate.cassandra.CassandraDriverReactiveHealthIndicator;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Health contributor options for Cassandra.
 *
 * @author Stephane Nicoll
 */
class CassandraHealthContributorConfigurations {

	/**
     * CassandraDriverConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(CqlSession.class)
	static class CassandraDriverConfiguration
			extends CompositeHealthContributorConfiguration<CassandraDriverHealthIndicator, CqlSession> {

		/**
         * Constructs a new CassandraDriverConfiguration object.
         * 
         * This constructor initializes the CassandraDriverHealthIndicator by calling the super constructor.
         * The CassandraDriverHealthIndicator is responsible for monitoring the health of the Cassandra driver.
         */
        CassandraDriverConfiguration() {
			super(CassandraDriverHealthIndicator::new);
		}

		/**
         * Creates a Cassandra health contributor if no bean with the names "cassandraHealthIndicator" and "cassandraHealthContributor" exists.
         * 
         * @param sessions a map of CqlSession instances
         * @return the created Cassandra health contributor
         */
        @Bean
		@ConditionalOnMissingBean(name = { "cassandraHealthIndicator", "cassandraHealthContributor" })
		HealthContributor cassandraHealthContributor(Map<String, CqlSession> sessions) {
			return createContributor(sessions);
		}

	}

	/**
     * CassandraReactiveDriverConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(CqlSession.class)
	static class CassandraReactiveDriverConfiguration extends
			CompositeReactiveHealthContributorConfiguration<CassandraDriverReactiveHealthIndicator, CqlSession> {

		/**
         * Constructs a new CassandraReactiveDriverConfiguration object.
         * 
         * This constructor initializes the CassandraReactiveDriverConfiguration object by calling the superclass constructor with a new instance of CassandraDriverReactiveHealthIndicator.
         * 
         * @see CassandraDriverReactiveHealthIndicator
         */
        CassandraReactiveDriverConfiguration() {
			super(CassandraDriverReactiveHealthIndicator::new);
		}

		/**
         * Creates a ReactiveHealthContributor for Cassandra if no existing bean with the names "cassandraHealthIndicator" and "cassandraHealthContributor" is found.
         * 
         * @param sessions a map of CqlSession instances
         * @return the created ReactiveHealthContributor for Cassandra
         */
        @Bean
		@ConditionalOnMissingBean(name = { "cassandraHealthIndicator", "cassandraHealthContributor" })
		ReactiveHealthContributor cassandraHealthContributor(Map<String, CqlSession> sessions) {
			return createContributor(sessions);
		}

	}

}
