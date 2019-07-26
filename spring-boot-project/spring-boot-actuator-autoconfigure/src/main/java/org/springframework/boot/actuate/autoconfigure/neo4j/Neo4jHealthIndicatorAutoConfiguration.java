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

package org.springframework.boot.actuate.autoconfigure.neo4j;

import java.util.Map;

import org.neo4j.driver.v1.Driver;
import org.neo4j.ogm.session.SessionFactory;

import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthIndicatorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.neo4j.Neo4jHealthIndicator;
import org.springframework.boot.actuate.neo4j.Neo4jNativeHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link Neo4jHealthIndicator}. It
 * prefers a native Neo4j driver bean over OGM's session factory.
 *
 * @author Eric Spiegelberg
 * @author Stephane Nicoll
 * @author Michael J. Simons
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnEnabledHealthIndicator("neo4j")
@AutoConfigureBefore(HealthIndicatorAutoConfiguration.class)
@AutoConfigureAfter(Neo4jDataAutoConfiguration.class)
public class Neo4jHealthIndicatorAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Driver.class)
	@ConditionalOnBean(Driver.class)
	@Order(-20)
	static class Neo4jNeo4jHealthIndicatorConfiguration
			extends CompositeHealthIndicatorConfiguration<Neo4jNativeHealthIndicator, Driver> {

		@Bean
		@ConditionalOnMissingBean(name = "neo4jHealthIndicator")
		HealthIndicator neo4jHealthIndicator(Map<String, Driver> drivers) {
			return createHealthIndicator(drivers);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(SessionFactory.class)
	@ConditionalOnBean(SessionFactory.class)
	@Order(-10)
	static class Neo4jOgmHealthIndicatorConfiguration
			extends CompositeHealthIndicatorConfiguration<Neo4jHealthIndicator, SessionFactory> {

		@Bean
		@ConditionalOnMissingBean(name = "neo4jHealthIndicator")
		HealthIndicator neo4jHealthIndicator(Map<String, SessionFactory> sessionFactories) {
			return createHealthIndicator(sessionFactories);
		}

	}

}
