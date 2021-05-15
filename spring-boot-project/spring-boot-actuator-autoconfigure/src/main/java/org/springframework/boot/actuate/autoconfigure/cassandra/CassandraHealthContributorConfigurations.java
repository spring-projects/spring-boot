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
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;

/**
 * Health contributor options for Cassandra.
 *
 * @author Stephane Nicoll
 */
class CassandraHealthContributorConfigurations {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(CqlSession.class)
	static class CassandraDriverConfiguration
			extends CompositeHealthContributorConfiguration<CassandraDriverHealthIndicator, CqlSession> {

		@Bean
		@ConditionalOnMissingBean(name = { "cassandraHealthIndicator", "cassandraHealthContributor" })
		HealthContributor cassandraHealthContributor(Map<String, CqlSession> sessions) {
			return createContributor(sessions);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(CassandraOperations.class)
	@ConditionalOnBean(CassandraOperations.class)
	@Deprecated
	static class CassandraOperationsConfiguration extends
			CompositeHealthContributorConfiguration<org.springframework.boot.actuate.cassandra.CassandraHealthIndicator, CassandraOperations> {

		@Bean
		@ConditionalOnMissingBean(name = { "cassandraHealthIndicator", "cassandraHealthContributor" })
		HealthContributor cassandraHealthContributor(Map<String, CassandraOperations> cassandraOperations) {
			return createContributor(cassandraOperations);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(CqlSession.class)
	static class CassandraReactiveDriverConfiguration extends
			CompositeReactiveHealthContributorConfiguration<CassandraDriverReactiveHealthIndicator, CqlSession> {

		@Bean
		@ConditionalOnMissingBean(name = { "cassandraHealthIndicator", "cassandraHealthContributor" })
		ReactiveHealthContributor cassandraHealthContributor(Map<String, CqlSession> sessions) {
			return createContributor(sessions);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ReactiveCassandraOperations.class)
	@ConditionalOnBean(ReactiveCassandraOperations.class)
	@Deprecated
	static class CassandraReactiveOperationsConfiguration extends
			CompositeReactiveHealthContributorConfiguration<org.springframework.boot.actuate.cassandra.CassandraReactiveHealthIndicator, ReactiveCassandraOperations> {

		@Bean
		@ConditionalOnMissingBean(name = { "cassandraHealthIndicator", "cassandraHealthContributor" })
		ReactiveHealthContributor cassandraHealthContributor(
				Map<String, ReactiveCassandraOperations> reactiveCassandraOperations) {
			return createContributor(reactiveCassandraOperations);
		}

	}

}
