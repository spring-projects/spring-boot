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

package org.springframework.boot.cassandra.autoconfigure.health;

import java.util.Map;

import com.datastax.oss.driver.api.core.CqlSession;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.cassandra.health.CassandraDriverHealthIndicator;
import org.springframework.boot.cassandra.health.CassandraDriverReactiveHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.CompositeHealthContributorConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.CompositeReactiveHealthContributorConfiguration;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.ReactiveHealthContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

		CassandraDriverConfiguration() {
			super(CassandraDriverHealthIndicator::new);
		}

		@Bean
		@ConditionalOnMissingBean(name = { "cassandraHealthIndicator", "cassandraHealthContributor" })
		HealthContributor cassandraHealthContributor(ConfigurableListableBeanFactory beanFactory) {
			return createContributor(beanFactory, CqlSession.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(CqlSession.class)
	static class CassandraReactiveDriverConfiguration extends
			CompositeReactiveHealthContributorConfiguration<CassandraDriverReactiveHealthIndicator, CqlSession> {

		CassandraReactiveDriverConfiguration() {
			super(CassandraDriverReactiveHealthIndicator::new);
		}

		@Bean
		@ConditionalOnMissingBean(name = { "cassandraHealthIndicator", "cassandraHealthContributor" })
		ReactiveHealthContributor cassandraHealthContributor(Map<String, CqlSession> sessions) {
			return createContributor(sessions);
		}

	}

}
