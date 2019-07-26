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

import org.junit.jupiter.api.Test;
import org.neo4j.driver.v1.Driver;
import org.neo4j.ogm.session.SessionFactory;

import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.ApplicationHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.neo4j.Neo4jHealthIndicator;
import org.springframework.boot.actuate.neo4j.Neo4jNativeHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Neo4jHealthIndicatorAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Michael J. Simons
 */
class Neo4jHealthIndicatorAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(Neo4jHealthIndicatorAutoConfiguration.class, HealthIndicatorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.withUserConfiguration(SessionFactoryBasedNeo4jConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(Neo4jHealthIndicator.class)
						.doesNotHaveBean(ApplicationHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withUserConfiguration(SessionFactoryBasedNeo4jConfiguration.class)
				.withPropertyValues("management.health.neo4j.enabled:false").run((context) -> assertThat(context)
						.doesNotHaveBean(Neo4jHealthIndicator.class).hasSingleBean(ApplicationHealthIndicator.class));
	}

	@Test
	void defaultIndicatorCanBeReplaced() {
		this.contextRunner
				.withUserConfiguration(SessionFactoryBasedNeo4jConfiguration.class, CustomIndicatorConfiguration.class)
				.run((context) -> {
					assertThat(context).hasBean("neo4jHealthIndicator");
					assertThat(context).doesNotHaveBean(ApplicationHealthIndicator.class);
					Health health = context.getBean("neo4jHealthIndicator", HealthIndicator.class).health();
					assertThat(health.getDetails()).containsOnly(entry("test", true));
				});
	}

	@Test
	void defaultNativeIndicatorCanBeReplaced() {
		this.contextRunner
				.withUserConfiguration(DriverBasedNeo4jConfiguration.class, CustomIndicatorConfiguration.class)
				.run((context) -> {
					assertThat(context).hasBean("neo4jHealthIndicator");
					assertThat(context).doesNotHaveBean(ApplicationHealthIndicator.class);
					Health health = context.getBean("neo4jHealthIndicator", HealthIndicator.class).health();
					assertThat(health.getDetails()).containsOnly(entry("test", true));
				});
	}

	@Test
	void shouldPreferDriver() {
		this.contextRunner
				.withUserConfiguration(SessionFactoryBasedNeo4jConfiguration.class, DriverBasedNeo4jConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(Neo4jNativeHealthIndicator.class)
						.doesNotHaveBean(Neo4jHealthIndicator.class).doesNotHaveBean(ApplicationHealthIndicator.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class SessionFactoryBasedNeo4jConfiguration {

		@Bean
		SessionFactory sessionFactory() {
			return mock(SessionFactory.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DriverBasedNeo4jConfiguration {

		@Bean
		Driver driver() {
			return mock(Driver.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomIndicatorConfiguration {

		@Bean
		HealthIndicator neo4jHealthIndicator() {
			return new AbstractHealthIndicator() {

				protected void doHealthCheck(Health.Builder builder) throws Exception {
					builder.up().withDetail("test", true);
				}
			};
		}

	}

}
